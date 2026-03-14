package com.example.queueapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ServerDiscovery — Scans the local Wi-Fi subnet for the Flask server
 * running on port 5000.  Tries every IP from .1 to .254 in parallel
 * with a very short timeout.  The first IP that responds to GET /status
 * with HTTP 200 is returned.
 */
public class ServerDiscovery {

    private static final String TAG = "ServerDiscovery";
    private static final int PORT = 5000;
    private static final int TIMEOUT_MS = 800;          // per-host timeout
    private static final int THREAD_POOL_SIZE = 30;     // parallel probes
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface DiscoveryCallback {
        void onFound(String serverUrl);
        void onNotFound();
    }

    /**
     * Starts the scan on a background thread pool.
     * Always posts callbacks on the main thread.
     */
    public static void discover(Context context, DiscoveryCallback callback) {
        new Thread(() -> {
            String subnet = getSubnetPrefix(context);
            if (subnet == null) {
                Log.w(TAG, "Could not determine Wi-Fi subnet");
                postNotFound(callback);
                return;
            }

            Log.d(TAG, "Scanning subnet: " + subnet + "x on port " + PORT);

            AtomicReference<String> found = new AtomicReference<>(null);
            CountDownLatch latch = new CountDownLatch(254);
            ExecutorService pool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

            for (int i = 1; i <= 254; i++) {
                final String ip = subnet + i;
                pool.submit(() -> {
                    try {
                        if (found.get() != null) return;   // already found
                        String testUrl = "http://" + ip + ":" + PORT + "/status";
                        HttpURLConnection conn = (HttpURLConnection) new URL(testUrl).openConnection();
                        conn.setConnectTimeout(TIMEOUT_MS);
                        conn.setReadTimeout(TIMEOUT_MS);
                        conn.setRequestMethod("GET");

                        int code = conn.getResponseCode();
                        conn.disconnect();

                        if (code == 200) {
                            String serverUrl = "http://" + ip + ":" + PORT;
                            Log.d(TAG, "Found server at " + serverUrl);
                            found.compareAndSet(null, serverUrl);
                        }
                    } catch (Exception ignored) {
                        // Host unreachable or timed out — expected for most IPs
                    } finally {
                        latch.countDown();
                    }
                });
            }

            try {
                latch.await();  // wait for all probes to finish
                if (found.get() != null) {
                    postFound(callback, found.get());
                } else {
                    postNotFound(callback);
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                postNotFound(callback);
            } finally {
                pool.shutdown();
            }
        }).start();
    }

    /**
     * Returns the first 3 octets of the device's Wi-Fi IP, e.g. "192.168.1."
     * Returns null if Wi-Fi is not connected.
     */
    private static String getSubnetPrefix(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
                if (cm == null) return null;
                Network activeNetwork = cm.getActiveNetwork();
                if (activeNetwork == null) return null;
                LinkProperties linkProperties = cm.getLinkProperties(activeNetwork);
                if (linkProperties == null) return null;

                for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
                    InetAddress address = linkAddress.getAddress();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        String hostAddress = address.getHostAddress();
                        if (hostAddress == null) continue;
                        int lastDot = hostAddress.lastIndexOf('.');
                        if (lastDot > 0) {
                            return hostAddress.substring(0, lastDot + 1);
                        }
                    }
                }
                return null;
            }

            WifiManager wm = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            if (wm == null) return null;

            WifiInfo info = wm.getConnectionInfo();
            int ipInt = info.getIpAddress();
            if (ipInt == 0) return null;

            return (ipInt & 0xFF) + "."
                + ((ipInt >> 8) & 0xFF) + "."
                + ((ipInt >> 16) & 0xFF) + ".";
        } catch (Exception e) {
            Log.e(TAG, "getSubnetPrefix failed", e);
            return null;
        }
    }

    private static void postFound(DiscoveryCallback callback, String serverUrl) {
        MAIN_HANDLER.post(() -> callback.onFound(serverUrl));
    }

    private static void postNotFound(DiscoveryCallback callback) {
        MAIN_HANDLER.post(callback::onNotFound);
    }
}

