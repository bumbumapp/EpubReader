package com.folioreader.android.sample;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Search, filter and sort rules of the library list. Pure Java, unit tested. */
final class LibraryLogic {

    enum Filter {ALL, FAVORITES, READING, FINISHED}

    enum Sort {RECENT, NAME, PROGRESS}

    static final class Item {
        /** Index in the stored path list, so actions reach the right book after filtering. */
        final int index;
        final String name;
        final boolean favorite;
        final float progress;
        final long lastOpened;
        final long readMs;

        Item(int index, String name, boolean favorite, float progress, long lastOpened, long readMs) {
            this.index = index;
            this.name = name;
            this.favorite = favorite;
            this.progress = progress;
            this.lastOpened = lastOpened;
            this.readMs = readMs;
        }
    }

    private LibraryLogic() {
    }

    static boolean matches(Item item, Filter filter) {
        switch (filter) {
            case FAVORITES:
                return item.favorite;
            case READING:
                return item.progress > 0f && item.progress < 1f;
            case FINISHED:
                return item.progress >= 1f;
            default:
                return true;
        }
    }

    @NonNull
    static List<Item> apply(List<Item> items, String query, Filter filter, Sort sort) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Item> out = new ArrayList<>();
        for (Item i : items) {
            if (matches(i, filter) && (q.isEmpty() || i.name.toLowerCase(Locale.ROOT).contains(q))) {
                out.add(i);
            }
        }
        Comparator<Item> cmp;
        switch (sort) {
            case NAME:
                cmp = (a, b) -> a.name.compareToIgnoreCase(b.name);
                break;
            case PROGRESS:
                cmp = (a, b) -> Float.compare(b.progress, a.progress);
                break;
            default:
                cmp = (a, b) -> Long.compare(b.lastOpened, a.lastOpened);
        }
        Collections.sort(out, cmp);
        return out;
    }
}
