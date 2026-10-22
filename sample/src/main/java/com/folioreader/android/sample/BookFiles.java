package com.folioreader.android.sample;

import androidx.annotation.Nullable;

/** File-name helpers for imported books. Unit tested. */
final class BookFiles {

    private BookFiles() {
    }

    /**
     * Returns a safe file name ending in ".epub": the reader derives the book type from the
     * extension and strips exactly 5 characters, so names without it failed to open.
     */
    static String safeEpubName(@Nullable String name) {
        String n = name == null ? "" : name.trim();
        int slash = Math.max(n.lastIndexOf('/'), n.lastIndexOf(':'));
        if (slash >= 0) {
            n = n.substring(slash + 1);
        }
        n = n.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1f]", "_");
        if (n.isEmpty() || n.equalsIgnoreCase(".epub")) {
            n = "book";
        }
        if (!n.toLowerCase(java.util.Locale.ROOT).endsWith(".epub")) {
            n = n + ".epub";
        } else {
            n = n.substring(0, n.length() - 5) + ".epub";
        }
        return n;
    }
}
