package com.example.queueapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

/**
 * NotificationHelper — Plays alarm sound and/or vibrates
 * when the customer's ticket is being served.
 * Respects the user's toggle preferences from SharedPreferences.
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";

    public static final String PREFS_NAME            = "queue_app_prefs";
    public static final String KEY_ALARM_ENABLED     = "alarm_sound_enabled";
    public static final String KEY_VIBRATION_ENABLED = "vibration_enabled";

    private static MediaPlayer mediaPlayer;

    // ── Check user preferences ───────────────────────────────

    public static boolean isAlarmEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ALARM_ENABLED, true);  // default ON
    }

    public static boolean isVibrationEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_VIBRATION_ENABLED, true);  // default ON
    }

    public static void setAlarmEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
               .edit().putBoolean(KEY_ALARM_ENABLED, enabled).apply();
    }

    public static void setVibrationEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
               .edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply();
    }

    // ── Trigger alarm + vibration based on settings ──────────

    public static void notifyNowServing(Context context) {
        if (isAlarmEnabled(context)) {
            playAlarmSound(context);
        }
        if (isVibrationEnabled(context)) {
            vibrate(context);
        }
    }

    // ── Play alarm sound ─────────────────────────────────────

    private static void playAlarmSound(Context context) {
        try {
            stopAlarmSound();  // stop any previous alarm

            // Try alarm ringtone first, fall back to notification ringtone
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(context, alarmUri);
            mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            );
            mediaPlayer.setLooping(false);
            mediaPlayer.prepare();
            mediaPlayer.start();

            // Auto-stop after 5 seconds
            mediaPlayer.setOnCompletionListener(mp -> stopAlarmSound());

        } catch (Exception e) {
            Log.e(TAG, "Failed to play alarm sound", e);
        }
    }

    public static void stopAlarmSound() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }
        } catch (Exception ignored) { }
    }

    // ── Vibrate ──────────────────────────────────────────────

    private static void vibrate(Context context) {
        try {
            Vibrator vibrator;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager)
                        context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                vibrator = vm.getDefaultVibrator();
            } else {
                vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            }

            if (vibrator == null || !vibrator.hasVibrator()) return;

            // Vibration pattern: wait 0ms, vibrate 400ms, pause 200ms, vibrate 400ms, pause 200ms, vibrate 400ms
            long[] pattern = {0, 400, 200, 400, 200, 400};

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(pattern, -1)  // -1 = don't repeat
                );
            } else {
                vibrator.vibrate(pattern, -1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to vibrate", e);
        }
    }
}

