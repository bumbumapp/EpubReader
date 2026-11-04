package com.folioreader.stats;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * On-device reading data: time per day and per book, chapter progress, last position and bookmarks.
 * Written by the reader screen, read by the library screens. Nothing leaves the device.
 */
public final class ReaderStats {

    private static final String PREFS = "folio_reader_stats";
    private static final String KEY_DAYS = "days";
    private static final String KEY_BOOK_MS = "book_ms";
    private static final String KEY_PROGRESS = "progress";
    private static final String KEY_LOCATOR = "locator";
    private static final String KEY_BOOKMARKS = "bookmarks";
    private static final String KEY_IDS = "book_ids";
    private static final String KEY_GOAL = "goal_minutes";
    public static final int DEFAULT_GOAL_MINUTES = 20;

    private static final Gson GSON = new Gson();

    private ReaderStats() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Stable key for a book: its file name without extension, lower case. */
    public static String key(String fileName) {
        String n = fileName == null ? "" : fileName;
        int slash = n.lastIndexOf('/');
        if (slash >= 0) {
            n = n.substring(slash + 1);
        }
        if (n.toLowerCase(Locale.ROOT).endsWith(".epub")) {
            n = n.substring(0, n.length() - 5);
        }
        return n.toLowerCase(Locale.ROOT);
    }

    public static long today() {
        long now = System.currentTimeMillis();
        return ReadingLog.dayIndex(now, TimeZone.getDefault().getOffset(now));
    }

    private static <T> T read(Context c, String key, TypeToken<T> type, T fallback) {
        try {
            T v = GSON.fromJson(prefs(c).getString(key, null), type.getType());
            return v != null ? v : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    // ---- reading time ----

    public static synchronized void addReadingTime(Context c, String fileName, long durationMs) {
        long ms = ReadingLog.clampSession(durationMs);
        if (ms <= 0) {
            return;
        }
        Map<Long, Long> days = days(c);
        ReadingLog.add(days, today(), ms);
        Map<String, Long> books = bookMs(c);
        String k = key(fileName);
        Long old = books.get(k);
        books.put(k, (old == null ? 0 : old) + ms);
        prefs(c).edit().putString(KEY_DAYS, GSON.toJson(days)).putString(KEY_BOOK_MS, GSON.toJson(books)).apply();
    }

    public static Map<Long, Long> days(Context c) {
        return read(c, KEY_DAYS, new TypeToken<HashMap<Long, Long>>() {
        }, new HashMap<Long, Long>());
    }

    public static Map<String, Long> bookMs(Context c) {
        return read(c, KEY_BOOK_MS, new TypeToken<HashMap<String, Long>>() {
        }, new HashMap<String, Long>());
    }

    public static int goalMinutes(Context c) {
        return prefs(c).getInt(KEY_GOAL, DEFAULT_GOAL_MINUTES);
    }

    public static void setGoalMinutes(Context c, int minutes) {
        prefs(c).edit().putInt(KEY_GOAL, Math.max(1, Math.min(minutes, 600))).apply();
    }

    // ---- progress and last position ----

    public static synchronized void setProgress(Context c, String fileName, float progress) {
        Map<String, Float> map = read(c, KEY_PROGRESS, new TypeToken<HashMap<String, Float>>() {
        }, new HashMap<String, Float>());
        map.put(key(fileName), progress);
        prefs(c).edit().putString(KEY_PROGRESS, GSON.toJson(map)).apply();
    }

    public static float progress(Context c, String fileName) {
        Map<String, Float> map = read(c, KEY_PROGRESS, new TypeToken<HashMap<String, Float>>() {
        }, new HashMap<String, Float>());
        Float v = map.get(key(fileName));
        return v == null ? 0f : v;
    }

    public static synchronized void saveLocator(Context c, String fileName, String locatorJson) {
        if (locatorJson == null) {
            return;
        }
        Map<String, String> map = read(c, KEY_LOCATOR, new TypeToken<HashMap<String, String>>() {
        }, new HashMap<String, String>());
        map.put(key(fileName), locatorJson);
        prefs(c).edit().putString(KEY_LOCATOR, GSON.toJson(map)).apply();
    }

    /** Remembers which library book a reader book id belongs to, so highlights can be listed per book. */
    public static synchronized void linkBookId(Context c, String bookId, String fileName) {
        if (bookId == null || bookId.isEmpty()) {
            return;
        }
        Map<String, String> map = bookIds(c);
        String k = key(fileName);
        if (!k.equals(map.get(bookId))) {
            map.put(bookId, k);
            prefs(c).edit().putString(KEY_IDS, GSON.toJson(map)).apply();
        }
    }

    /** reader book id -> book key */
    public static Map<String, String> bookIds(Context c) {
        return read(c, KEY_IDS, new TypeToken<HashMap<String, String>>() {
        }, new HashMap<String, String>());
    }

    public static String locator(Context c, String fileName) {
        Map<String, String> map = read(c, KEY_LOCATOR, new TypeToken<HashMap<String, String>>() {
        }, new HashMap<String, String>());
        return map.get(key(fileName));
    }

    // ---- bookmarks ----

    public static List<Bookmark> bookmarks(Context c) {
        return read(c, KEY_BOOKMARKS, new TypeToken<ArrayList<Bookmark>>() {
        }, new ArrayList<Bookmark>());
    }

    /** @return true when saved, false when this spot was already bookmarked */
    public static synchronized boolean addBookmark(Context c, Bookmark b) {
        List<Bookmark> list = bookmarks(c);
        boolean added = BookmarkList.add(list, b);
        if (added) {
            prefs(c).edit().putString(KEY_BOOKMARKS, GSON.toJson(list)).apply();
        }
        return added;
    }

    public static synchronized void removeBookmark(Context c, long id) {
        List<Bookmark> list = bookmarks(c);
        if (BookmarkList.remove(list, id)) {
            prefs(c).edit().putString(KEY_BOOKMARKS, GSON.toJson(list)).apply();
        }
    }

    /** Forgets everything stored for a book that was deleted from the library. */
    public static synchronized void forgetBook(Context c, String fileName) {
        String k = key(fileName);
        Map<String, Long> ms = bookMs(c);
        ms.remove(k);
        Map<String, Float> pr = read(c, KEY_PROGRESS, new TypeToken<HashMap<String, Float>>() {
        }, new HashMap<String, Float>());
        pr.remove(k);
        Map<String, String> loc = read(c, KEY_LOCATOR, new TypeToken<HashMap<String, String>>() {
        }, new HashMap<String, String>());
        loc.remove(k);
        List<Bookmark> bm = bookmarks(c);
        BookmarkList.removeBook(bm, k);
        prefs(c).edit().putString(KEY_BOOK_MS, GSON.toJson(ms)).putString(KEY_PROGRESS, GSON.toJson(pr))
                .putString(KEY_LOCATOR, GSON.toJson(loc)).putString(KEY_BOOKMARKS, GSON.toJson(bm)).apply();
    }
}
