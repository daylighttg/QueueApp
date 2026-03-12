package com.example.queueapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

/**
 * JoinQueueActivity — Join Screen
 * Customer types their name and taps "Confirm" to join the queue.
 * On success, opens TicketActivity with the ticket info.
 */
public class JoinQueueActivity extends AppCompatActivity {

    private EditText etName;
    private Button btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_queue);

        etName     = findViewById(R.id.etName);
        btnConfirm = findViewById(R.id.btnConfirm);

        btnConfirm.setOnClickListener(v -> joinQueue());
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

            ApiService.post("/join", body, new ApiService.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(() -> onJoinSuccess(response));
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        btnConfirm.setEnabled(true);
                        btnConfirm.setText("Confirm");
                        Toast.makeText(JoinQueueActivity.this,
                            "Failed to join: " + error, Toast.LENGTH_LONG).show();
                    });
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
}

