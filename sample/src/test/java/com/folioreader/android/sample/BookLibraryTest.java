package com.folioreader.android.sample;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

public class BookLibraryTest {

    private static ArrayList<String> list(String... s) {
        return new ArrayList<>(Arrays.asList(s));
    }

    @Test
    public void pruneRemovesConsecutiveMissingFilesAndAlignsLists() {
        ArrayList<String> paths = list("/a/1.epub", "/a/gone1.epub", "/a/gone2.epub", "/a/2.epub");
        ArrayList<String> mod = list("t1", "t2"); // shorter: used to crash the adapter
        BookLibrary.prune(paths, mod, p -> !p.contains("gone"));
        assertEquals(list("/a/1.epub", "/a/2.epub"), paths);
        assertEquals(2, mod.size());
        assertEquals("t1", mod.get(0));
    }

    @Test
    public void addOrTouchUpdatesExistingBookByFileName() {
        ArrayList<String> paths = list("/old/dir/book.epub");
        ArrayList<String> mod = list("t0");
        int i = BookLibrary.addOrTouch(paths, mod, "/new/dir/book.epub", "t1");
        assertEquals(0, i);
        assertEquals(1, paths.size());
        assertEquals("/new/dir/book.epub", paths.get(0));
        assertEquals("t1", mod.get(0));
    }

    @Test
    public void addOrTouchAppendsNewBook() {
        ArrayList<String> paths = list();
        ArrayList<String> mod = list();
        assertEquals(0, BookLibrary.addOrTouch(paths, mod, "/x/a.epub", "t"));
        assertEquals(1, BookLibrary.addOrTouch(paths, mod, "/x/b.epub", "t"));
    }

    @Test
    public void safeEpubNameAlwaysEndsWithEpub() {
        assertEquals("My Book.epub", BookFiles.safeEpubName("My Book.epub"));
        assertEquals("My Book.epub", BookFiles.safeEpubName("My Book.EPUB"));
        assertEquals("1234.epub", BookFiles.safeEpubName("primary:Download/1234"));
        assertEquals("book.epub", BookFiles.safeEpubName(null));
        assertEquals("a_b.epub", BookFiles.safeEpubName("a*b"));
    }
}
