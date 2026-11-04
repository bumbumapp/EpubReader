package com.folioreader.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Pure list operations for bookmarks. Unit tested. */
public final class BookmarkList {

    private BookmarkList() {
    }

    /**
     * Adds the bookmark unless the same spot of the same book is already saved.
     *
     * @return true when added, false when it was a duplicate
     */
    public static boolean add(List<Bookmark> list, Bookmark b) {
        for (Bookmark o : list) {
            if (safe(o.bookKey).equals(safe(b.bookKey)) && safe(o.locator).equals(safe(b.locator))) {
                return false;
            }
        }
        long id = b.created;
        boolean clash = true;
        while (clash) {
            clash = false;
            for (Bookmark o : list) {
                if (o.id == id) {
                    id++;
                    clash = true;
                    break;
                }
            }
        }
        b.id = id;
        list.add(b);
        return true;
    }

    public static boolean remove(List<Bookmark> list, long id) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == id) {
                list.remove(i);
                return true;
            }
        }
        return false;
    }

    /** Newest first, keeping only bookmarks whose book or chapter or note contains the query. */
    public static List<Bookmark> filter(List<Bookmark> list, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Bookmark> out = new ArrayList<>();
        for (Bookmark b : list) {
            if (q.isEmpty() || safe(b.bookTitle).toLowerCase(Locale.ROOT).contains(q)
                    || safe(b.chapter).toLowerCase(Locale.ROOT).contains(q)
                    || safe(b.note).toLowerCase(Locale.ROOT).contains(q)) {
                out.add(b);
            }
        }
        Collections.sort(out, new Comparator<Bookmark>() {
            @Override
            public int compare(Bookmark a, Bookmark b) {
                return Long.compare(b.created, a.created);
            }
        });
        return out;
    }

    public static void removeBook(List<Bookmark> list, String bookKey) {
        for (int i = list.size() - 1; i >= 0; i--) {
            if (safe(list.get(i).bookKey).equals(safe(bookKey))) {
                list.remove(i);
            }
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
