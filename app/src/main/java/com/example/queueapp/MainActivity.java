package com.example.queueapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

/**
 * MainActivity — Home Screen
 * Shows "Now Serving" info and "People Waiting" count.
 * Has a button to open the Join Queue screen.
 * Auto-refreshes every 5 seconds.
 * On first launch, auto-discovers the Flask server on the LAN.
 */
public class MainActivity extends AppCompatActivity {

    private TextView tvNowServing;
    private TextView tvWaitingCount;
    private Button btnJoinQueue;
    private Button btnRefresh;
    private Button btnSettings;

    private final Handler autoRefreshHandler = new Handler(Looper.getMainLooper());
    private final int REFRESH_INTERVAL = 5000; // 5 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvNowServing   = findViewById(R.id.tvNowServing);
        tvWaitingCount = findViewById(R.id.tvWaitingCount);
        btnJoinQueue   = findViewById(R.id.btnJoinQueue);
        btnRefresh     = findViewById(R.id.btnRefresh);
        btnSettings    = findViewById(R.id.btnSettings);

        btnJoinQueue.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, JoinQueueActivity.class);
            startActivity(intent);
        });

        btnRefresh.setOnClickListener(v -> fetchStatus());

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // Auto-discover server on first launch
        if (!ApiService.isServerConfigured(this)) {
            Toast.makeText(this, "Searching for server…", Toast.LENGTH_SHORT).show();
            ServerDiscovery.discover(this, new ServerDiscovery.DiscoveryCallback() {
                @Override
                public void onFound(String serverUrl) {
                    ApiService.setServerUrl(MainActivity.this, serverUrl);
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this,
                            "Server found: " + serverUrl, Toast.LENGTH_SHORT).show();
                        fetchStatus();
                    });
                }

                @Override
                public void onNotFound() {
                    runOnUiThread(() ->
                        Toast.makeText(MainActivity.this,
                            "Server not found. Go to Settings to enter the IP manually.",
                            Toast.LENGTH_LONG).show()
                    );
                }
            });
        }

        fetchStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchStatus();
        startAutoRefresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAutoRefresh();
    }

    // ── Fetch current status from Flask GET /status ──────────
    private void fetchStatus() {
        ApiService.get(this, "/status", new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> updateDisplay(response));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                    Toast.makeText(MainActivity.this,
                        "Could not reach server: " + error, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    // ── Update the screen labels ─────────────────────────────
    private void updateDisplay(JSONObject response) {
        try {
            int waiting = response.getInt("waiting");
            tvWaitingCount.setText("People Waiting: " + waiting);

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
            tvWaitingCount.setText("People Waiting: —");
        }
    }

    // ── Auto-refresh helpers ─────────────────────────────────
    private final Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            fetchStatus();
            autoRefreshHandler.postDelayed(this, REFRESH_INTERVAL);
        }
    };

    private void startAutoRefresh() {
        autoRefreshHandler.postDelayed(autoRefreshRunnable, REFRESH_INTERVAL);
    }

    private void stopAutoRefresh() {
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
    }
}
