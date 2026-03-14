package com.example.queueapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

/**
 * JoinQueueActivity — Join Screen
 * Customer types their name and taps "Confirm" to join the queue.
 * On success, opens TicketActivity with the ticket info.
 */
public class JoinQueueActivity extends AppCompatActivity {

    private static final String STATE_CONFIRM_ENABLED = "state_confirm_enabled";
    private static final String STATE_CONFIRM_TEXT = "state_confirm_text";

    private EditText etName;
    private Button btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_queue);

        etName     = findViewById(R.id.etName);
        btnConfirm = findViewById(R.id.btnConfirm);

        if (savedInstanceState != null) {
            boolean wasEnabled = savedInstanceState.getBoolean(STATE_CONFIRM_ENABLED, true);
            String savedText = savedInstanceState.getString(STATE_CONFIRM_TEXT);
            if (savedText != null) {
                btnConfirm.setText(savedText);
            }

            if (!wasEnabled) {
                // The in-flight request cannot resume after process death, so recover the action.
                btnConfirm.setEnabled(true);
                btnConfirm.setText("Confirm");
            } else {
                btnConfirm.setEnabled(true);
            }
        }

        btnConfirm.setOnClickListener(v -> joinQueue());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showBackConfirmationDialog();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_CONFIRM_ENABLED, btnConfirm.isEnabled());
        outState.putString(STATE_CONFIRM_TEXT, btnConfirm.getText().toString());
    }

    // ── Send name to Flask POST /join ────────────────────────
    private void joinQueue() {
        String name = etName.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button while request is in progress
        btnConfirm.setEnabled(false);
        btnConfirm.setText("Joining…");

        try {
            JSONObject body = new JSONObject();
            body.put("name", name);

            ApiService.post(JoinQueueActivity.this, "/join", body, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    onJoinSuccess(response);
                }

                @Override
                public void onError(String error) {
                    btnConfirm.setEnabled(true);
                    btnConfirm.setText("Confirm");
                    Toast.makeText(JoinQueueActivity.this,
                        "Failed to join: " + error, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            btnConfirm.setEnabled(true);
            btnConfirm.setText("Confirm");
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ── On success → open TicketActivity ─────────────────────
    private void onJoinSuccess(JSONObject response) {
        try {
            String ticket   = response.getString("ticket");
            String name     = response.getString("name");
            int    position = response.getInt("position");

            Intent intent = new Intent(JoinQueueActivity.this, TicketActivity.class);
            intent.putExtra("ticket", ticket);
            intent.putExtra("name", name);
            intent.putExtra("position", position);
            startActivity(intent);
            finish(); // close this screen
        } catch (Exception e) {
            Toast.makeText(this, "Unexpected response: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
            btnConfirm.setEnabled(true);
            btnConfirm.setText("Confirm");
        }
    }

    private void showBackConfirmationDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Go back?")
            .setMessage("Are you sure you want to go back?")
            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
            .setPositiveButton("Go back", (dialog, which) -> finish())
            .show();
    }
}

