package com.folioreader.android.sample;

import android.content.Context;
import android.content.SharedPreferences;

import com.folioreader.stats.ReaderStats;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Favourites and last-opened times of library books, keyed by {@link ReaderStats#key(String)}. */
final class LibraryMeta {

    private static final String PREFS = "library_meta";
    private static final Gson GSON = new Gson();

    private LibraryMeta() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static Set<String> favorites(Context c) {
        try {
            Set<String> s = GSON.fromJson(prefs(c).getString("fav", null), new TypeToken<HashSet<String>>() {
            }.getType());
            return s != null ? s : new HashSet<>();
        } catch (RuntimeException e) {
            return new HashSet<>();
        }
    }

    /** @return the new favourite state */
    static boolean toggleFavorite(Context c, String fileName) {
        Set<String> s = favorites(c);
        String k = ReaderStats.key(fileName);
        boolean now = s.add(k);
        if (!now) {
            s.remove(k);
        }
        prefs(c).edit().putString("fav", GSON.toJson(s)).apply();
        return now;
    }

    static Map<String, Long> opened(Context c) {
        try {
            Map<String, Long> m = GSON.fromJson(prefs(c).getString("opened", null),
                    new TypeToken<HashMap<String, Long>>() {
                    }.getType());
            return m != null ? m : new HashMap<>();
        } catch (RuntimeException e) {
            return new HashMap<>();
        }
    }

    static void touch(Context c, String fileName) {
        Map<String, Long> m = opened(c);
        m.put(ReaderStats.key(fileName), System.currentTimeMillis());
        prefs(c).edit().putString("opened", GSON.toJson(m)).apply();
    }

    static void forget(Context c, String fileName) {
        String k = ReaderStats.key(fileName);
        Set<String> f = favorites(c);
        f.remove(k);
        Map<String, Long> m = opened(c);
        m.remove(k);
        prefs(c).edit().putString("fav", GSON.toJson(f)).putString("opened", GSON.toJson(m)).apply();
    }

    /** book key -> display title, from the stored library paths. */
    static Map<String, String> titles(Context c) {
        Map<String, String> out = new HashMap<>();
        try {
            java.util.List<String> paths = GSON.fromJson(
                    c.getSharedPreferences(c.getPackageName() + "_preferences", Context.MODE_PRIVATE)
                            .getString("key", null),
                    new TypeToken<java.util.ArrayList<String>>() {
                    }.getType());
            if (paths != null) {
                for (String p : paths) {
                    String name = BookLibrary.fileName(p);
                    out.put(ReaderStats.key(name),
                            name.toLowerCase(java.util.Locale.ROOT).endsWith(".epub")
                                    ? name.substring(0, name.length() - 5) : name);
                }
            }
        } catch (RuntimeException ignored) {
        }
        return out;
    }
}
