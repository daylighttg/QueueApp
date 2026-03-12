package com.example.queueapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * TicketActivity — Ticket Screen
 * Shows the customer their ticket number and current position.
 * Refresh button re-checks position from the server.
 */
public class TicketActivity extends AppCompatActivity {

    private TextView tvTicket;
    private TextView tvPosition;
    private TextView tvNowServing;
    private Button btnRefresh;
    private Button btnBack;

    private String myTicket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket);

        tvTicket     = findViewById(R.id.tvTicket);
        tvPosition   = findViewById(R.id.tvPosition);
        tvNowServing = findViewById(R.id.tvTicketNowServing);
        btnRefresh   = findViewById(R.id.btnTicketRefresh);
        btnBack      = findViewById(R.id.btnBack);

        // Get data passed from JoinQueueActivity
        myTicket      = getIntent().getStringExtra("ticket");
        String name   = getIntent().getStringExtra("name");
        int position  = getIntent().getIntExtra("position", 0);

        tvTicket.setText("🎫 " + myTicket);
        tvPosition.setText("You are #" + position + " in line");
        tvNowServing.setText("Now Serving: —");

        btnRefresh.setOnClickListener(v -> refreshStatus());
        btnBack.setOnClickListener(v -> finish());

        refreshStatus();
    }

    // ── Ask Flask for current queue + status ─────────────────
    private void refreshStatus() {
        // Fetch waiting list to find our position
        ApiService.get("/queue", new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> updatePosition(response));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                    Toast.makeText(TicketActivity.this,
                        "Could not reach server.", Toast.LENGTH_SHORT).show()
                );
            }
        });

        // Fetch who is being served
        ApiService.get("/status", new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> updateServing(response));
            }

            @Override
            public void onError(String error) { /* already toasted above */ }
        });
    }

    // ── Update position label ────────────────────────────────
    private void updatePosition(JSONObject response) {
        try {
            JSONArray waiting = response.getJSONArray("waiting");
            boolean found = false;

            for (int i = 0; i < waiting.length(); i++) {
                JSONObject person = waiting.getJSONObject(i);
                if (person.getString("ticket").equals(myTicket)) {
                    int pos = person.getInt("position");
                    tvPosition.setText("You are #" + pos + " in line");
                    found = true;
                    break;
                }
            }

            if (!found) {
                // Ticket not in waiting list — may be serving or done
                tvPosition.setText("You are no longer in the waiting list.");
            }
        } catch (Exception e) {
            tvPosition.setText("Could not update position.");
        }
    }

    // ── Update "Now Serving" label ───────────────────────────
    private void updateServing(JSONObject response) {
        try {
            if (response.isNull("serving")) {
                tvNowServing.setText("Now Serving: —");
            } else {
                JSONObject serving = response.getJSONObject("serving");
                String ticket = serving.getString("ticket");
                String name   = serving.getString("name");
                tvNowServing.setText("Now Serving: " + ticket + " — " + name);
            }
        } catch (Exception e) {
            tvNowServing.setText("Now Serving: —");
        }
    }
}

