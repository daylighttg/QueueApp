package com.example.queueapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import org.json.JSONObject;

/**
 * SettingsActivity — Settings Screen
 * Lets the user toggle alarm sound / vibration,
 * auto-discover the Flask server, or enter a custom URL.
 */
public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchAlarmSound;
    private SwitchCompat switchVibration;
    private EditText etServerUrl;
    private TextView tvConnectionStatus;
    private Button btnAutoDiscover;
    private Button btnSaveServer;
    private Button btnTestConnection;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // ── Bind views ───────────────────────────────────────
        switchAlarmSound  = findViewById(R.id.switchAlarmSound);
        switchVibration   = findViewById(R.id.switchVibration);
        etServerUrl       = findViewById(R.id.etServerUrl);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        btnAutoDiscover   = findViewById(R.id.btnAutoDiscover);
        btnSaveServer     = findViewById(R.id.btnSaveServer);
        btnTestConnection = findViewById(R.id.btnTestConnection);
        btnBack           = findViewById(R.id.btnSettingsBack);

        // ── Load saved preferences ───────────────────────────
        switchAlarmSound.setChecked(NotificationHelper.isAlarmEnabled(this));
        switchVibration.setChecked(NotificationHelper.isVibrationEnabled(this));
        etServerUrl.setText(ApiService.getServerUrl(this));

        // ── Toggle listeners ─────────────────────────────────
        switchAlarmSound.setOnCheckedChangeListener((btn, checked) ->
            NotificationHelper.setAlarmEnabled(this, checked)
        );

        switchVibration.setOnCheckedChangeListener((btn, checked) ->
            NotificationHelper.setVibrationEnabled(this, checked)
        );

        // ── Auto-discover button ─────────────────────────────
        btnAutoDiscover.setOnClickListener(v -> startAutoDiscover());

        // ── Save button ──────────────────────────────────────
        btnSaveServer.setOnClickListener(v -> saveServerUrl());

        // ── Test connection button ───────────────────────────
        btnTestConnection.setOnClickListener(v -> testConnection());

        // ── Back button ──────────────────────────────────────
        btnBack.setOnClickListener(v -> finish());

        // Auto-check connection status on open
        testConnectionSilent();
    }

    // ── Auto-discover the Flask server on the LAN ────────────
    private void startAutoDiscover() {
        btnAutoDiscover.setEnabled(false);
        btnAutoDiscover.setText("Scanning…");
        tvConnectionStatus.setText("Status: Scanning network…");
        tvConnectionStatus.setTextColor(0xFF888888);

        ServerDiscovery.discover(this, new ServerDiscovery.DiscoveryCallback() {
            @Override
            public void onFound(String serverUrl) {
                runOnUiThread(() -> {
                    etServerUrl.setText(serverUrl);
                    ApiService.setServerUrl(SettingsActivity.this, serverUrl);
                    tvConnectionStatus.setText("Status: ✅ Found server at " + serverUrl);
                    tvConnectionStatus.setTextColor(0xFF2E7D32);
                    btnAutoDiscover.setEnabled(true);
                    btnAutoDiscover.setText("🔍 Auto-Detect");
                    Toast.makeText(SettingsActivity.this,
                        "Server found and saved!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onNotFound() {
                runOnUiThread(() -> {
                    tvConnectionStatus.setText("Status: ❌ No server found on network");
                    tvConnectionStatus.setTextColor(0xFFC62828);
                    btnAutoDiscover.setEnabled(true);
                    btnAutoDiscover.setText("🔍 Auto-Detect");
                    Toast.makeText(SettingsActivity.this,
                        "Server not found. Please enter the URL manually.",
                        Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // ── Save the manually entered URL ────────────────────────
    private void saveServerUrl() {
        String url = etServerUrl.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a server URL.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Remove trailing slash
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        // Prepend http:// if missing
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        ApiService.setServerUrl(this, url);
        etServerUrl.setText(url);
        Toast.makeText(this, "Server URL saved!", Toast.LENGTH_SHORT).show();

        // Test the new URL
        testConnection();
    }

    // ── Test connection to the server ────────────────────────
    private void testConnection() {
        tvConnectionStatus.setText("Status: Testing connection…");
        tvConnectionStatus.setTextColor(0xFF888888);

        ApiService.get(this, "/status", new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    tvConnectionStatus.setText("Status: ✅ Connected");
                    tvConnectionStatus.setTextColor(0xFF2E7D32);
                    Toast.makeText(SettingsActivity.this,
                        "Connection successful!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    tvConnectionStatus.setText("Status: ❌ Cannot connect — " + error);
                    tvConnectionStatus.setTextColor(0xFFC62828);
                });
            }
        });
    }

    // ── Silent connection check (no toast) ───────────────────
    private void testConnectionSilent() {
        tvConnectionStatus.setText("Status: Checking…");
        tvConnectionStatus.setTextColor(0xFF888888);

        ApiService.get(this, "/status", new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    tvConnectionStatus.setText("Status: ✅ Connected");
                    tvConnectionStatus.setTextColor(0xFF2E7D32);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    tvConnectionStatus.setText("Status: ❌ Not connected");
                    tvConnectionStatus.setTextColor(0xFFC62828);
                });
            }
        });
    }
}

