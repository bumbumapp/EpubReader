package com.folioreader.android.sample;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure list logic for the "recent books" screen (paths + last-opened labels kept as two parallel
 * lists in SharedPreferences for backward compatibility). Unit tested.
 */
final class BookLibrary {

    private BookLibrary() {
    }

    interface Exists {
        boolean exists(String path);
    }

    static final Exists FILE_EXISTS = path -> path != null && new File(path).exists();

    static String fileName(String path) {
        if (path == null) {
            return "";
        }
        return path.substring(path.lastIndexOf('/') + 1);
    }

    /**
     * Makes both lists the same length and drops entries whose file is gone. The old code removed
     * items while iterating forward (skipping entries) and crashed when the lists had different sizes.
     */
    static void prune(List<String> paths, List<String> modified, Exists exists) {
        while (modified.size() < paths.size()) {
            modified.add("");
        }
        while (modified.size() > paths.size()) {
            modified.remove(modified.size() - 1);
        }
        for (int i = paths.size() - 1; i >= 0; i--) {
            if (!exists.exists(paths.get(i))) {
                paths.remove(i);
                modified.remove(i);
            }
        }
    }

    /**
     * Adds the book, or refreshes its "last opened" time when a book with the same file name is
     * already listed (the old code looked the full path up by index and crashed with -1).
     *
     * @return the index of the book in the list
     */
    static int addOrTouch(List<String> paths, List<String> modified, String path, String time) {
        prune(paths, modified, p -> true);
        String name = fileName(path);
        for (int i = 0; i < paths.size(); i++) {
            if (fileName(paths.get(i)).equals(name)) {
                paths.set(i, path);
                modified.set(i, time);
                return i;
            }
        }
        paths.add(path);
        modified.add(time);
        return paths.size() - 1;
    }

    static ArrayList<String> orEmpty(ArrayList<String> list) {
        return list != null ? list : new ArrayList<>();
    }
}
