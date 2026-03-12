package com.example.queueapp;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * ApiService — Network Helper
 * Handles all HTTP requests to the Flask server.
 * Other activities call ApiService.get() or ApiService.post() —
 * they never talk to Flask directly.
 *
 * The server URL is read from SharedPreferences.  On first launch the
 * app auto-discovers the server via ServerDiscovery.  If that fails
 * the user can enter the IP manually in SettingsActivity.
 */
public class ApiService {

    public static final String PREFS_NAME     = "queue_app_prefs";
    public static final String KEY_SERVER_URL  = "server_url";
    private static final String DEFAULT_URL    = "http://192.168.1.5:5000";

    // ── Read / write server URL ──────────────────────────────

    public static String getServerUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_SERVER_URL, DEFAULT_URL);
    }

    public static void setServerUrl(Context context, String url) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_SERVER_URL, url).apply();
    }

    public static boolean isServerConfigured(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.contains(KEY_SERVER_URL);
    }

    // ── Callback interface ───────────────────────────────────
    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }

    // ── GET request ──────────────────────────────────────────
    public static void get(Context context, String endpoint, ApiCallback callback) {
        String serverUrl = getServerUrl(context);
        new Thread(() -> {
            try {
                URL url = new URL(serverUrl + endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int code = conn.getResponseCode();
                String body = readStream(conn);
                conn.disconnect();

                JSONObject json = parseResponse(body);

                if (code >= 200 && code < 300) {
                    callback.onSuccess(json);
                } else {
                    String error = json.optString("error", "Server error " + code);
                    callback.onError(error);
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    // ── POST request ─────────────────────────────────────────
    public static void post(Context context, String endpoint, JSONObject data, ApiCallback callback) {
        String serverUrl = getServerUrl(context);
        new Thread(() -> {
            try {
                URL url = new URL(serverUrl + endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                // Write JSON body
                byte[] bytes = data.toString().getBytes(StandardCharsets.UTF_8);
                OutputStream os = conn.getOutputStream();
                os.write(bytes);
                os.flush();
                os.close();

                int code = conn.getResponseCode();
                String body = readStream(conn);
                conn.disconnect();

                JSONObject json = parseResponse(body);

                if (code >= 200 && code < 300) {
                    callback.onSuccess(json);
                } else {
                    String error = json.optString("error", "Server error " + code);
                    callback.onError(error);
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    // ── Read the response stream into a String ───────────────
    private static String readStream(HttpURLConnection conn) {
        try {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    conn.getResponseCode() >= 400
                        ? conn.getErrorStream()
                        : conn.getInputStream(),
                    StandardCharsets.UTF_8
                )
            );
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            return "{}";
        }
    }

    // ── Parse JSON from the Flask response ───────────────────
    private static JSONObject parseResponse(String body) {
        try {
            return new JSONObject(body);
        } catch (Exception e) {
            return new JSONObject();
        }
    }
}
