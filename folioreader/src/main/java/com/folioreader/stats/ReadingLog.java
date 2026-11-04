package com.folioreader.stats;

import java.util.Map;

/** Pure maths for reading-time statistics (days are whole days since the epoch in local time). Unit tested. */
public final class ReadingLog {

    public static final long DAY_MS = 24L * 60 * 60 * 1000;
    /** Sessions shorter than this are taps, not reading. */
    public static final long MIN_SESSION_MS = 3_000L;
    /** A longer foreground stretch means the phone was left open on the book. */
    public static final long MAX_SESSION_MS = 3L * 60 * 60 * 1000;
    /** A day counts towards the streak after one minute of reading. */
    public static final long STREAK_DAY_MS = 60_000L;

    private ReadingLog() {
    }

    public static long dayIndex(long timeMs, long tzOffsetMs) {
        return Math.floorDiv(timeMs + tzOffsetMs, DAY_MS);
    }

    /** Returns the counted duration of a foreground stretch, or 0 when it is too short. */
    public static long clampSession(long durationMs) {
        if (durationMs < MIN_SESSION_MS) {
            return 0;
        }
        return Math.min(durationMs, MAX_SESSION_MS);
    }

    public static void add(Map<Long, Long> days, long day, long ms) {
        if (ms <= 0) {
            return;
        }
        Long old = days.get(day);
        days.put(day, (old == null ? 0 : old) + ms);
    }

    public static long total(Map<Long, Long> days) {
        long sum = 0;
        for (Long v : days.values()) {
            if (v != null) {
                sum += v;
            }
        }
        return sum;
    }

    public static long get(Map<Long, Long> days, long day) {
        Long v = days.get(day);
        return v == null ? 0 : v;
    }

    /** Consecutive days with at least a minute of reading, ending today (or yesterday if today is still empty). */
    public static int streak(Map<Long, Long> days, long today) {
        long day = get(days, today) >= STREAK_DAY_MS ? today : today - 1;
        int count = 0;
        while (get(days, day) >= STREAK_DAY_MS) {
            count++;
            day--;
        }
        return count;
    }

    /** Reading time of the last {@code n} days, oldest first, ending today. */
    public static long[] lastDays(Map<Long, Long> days, long today, int n) {
        long[] out = new long[n];
        for (int i = 0; i < n; i++) {
            out[i] = get(days, today - (n - 1 - i));
        }
        return out;
    }

    /** Whole-book progress from the chapter being read: 0..1, where the last chapter counts as 1. */
    public static float chapterProgress(int chapterIndex, int chapterCount) {
        if (chapterCount <= 0 || chapterIndex < 0) {
            return 0f;
        }
        return Math.min(1f, (chapterIndex + 1f) / chapterCount);
    }

    /** "1h 05m", "12 min", "<1 min". */
    public static String format(long ms) {
        long minutes = ms / 60_000L;
        if (minutes <= 0) {
            return ms > 0 ? "<1 min" : "0 min";
        }
        if (minutes < 60) {
            return minutes + " min";
        }
        return (minutes / 60) + "h " + String.format(java.util.Locale.US, "%02d", minutes % 60) + "m";
    }
}
