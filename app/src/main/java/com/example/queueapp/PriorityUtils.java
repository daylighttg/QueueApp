package com.example.queueapp;

public final class PriorityUtils {

    private PriorityUtils() {
    }

    public static int normalizePriority(int priority) {
        if (priority < 0) {
            return 0;
        }
        if (priority > 2) {
            return 2;
        }
        return priority;
    }

    public static String emojiFor(int priority) {
        switch (normalizePriority(priority)) {
            case 2:
                return "🔴";
            case 1:
                return "🟡";
            default:
                return "🟢";
        }
    }

    public static int colorResFor(int priority) {
        switch (normalizePriority(priority)) {
            case 2:
                return R.color.priority_vip;
            case 1:
                return R.color.priority_senior;
            default:
                return R.color.priority_regular;
        }
    }
}
