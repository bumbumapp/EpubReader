package com.folioreader.android.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class LibraryLogicTest {

    private static List<LibraryLogic.Item> items() {
        List<LibraryLogic.Item> l = new ArrayList<>();
        l.add(new LibraryLogic.Item(0, "Dune", true, 0.5f, 100, 0));
        l.add(new LibraryLogic.Item(1, "emma", false, 1f, 300, 0));
        l.add(new LibraryLogic.Item(2, "Alice", false, 0f, 200, 0));
        return l;
    }

    @Test
    public void filtersByStatus() {
        assertEquals(1, LibraryLogic.apply(items(), "", LibraryLogic.Filter.FAVORITES, LibraryLogic.Sort.NAME).size());
        assertEquals("Dune", LibraryLogic.apply(items(), "", LibraryLogic.Filter.READING, LibraryLogic.Sort.NAME).get(0).name);
        assertEquals("emma", LibraryLogic.apply(items(), "", LibraryLogic.Filter.FINISHED, LibraryLogic.Sort.NAME).get(0).name);
    }

    @Test
    public void searchIsCaseInsensitiveAndKeepsOriginalIndex() {
        List<LibraryLogic.Item> r = LibraryLogic.apply(items(), " ALI ", LibraryLogic.Filter.ALL, LibraryLogic.Sort.RECENT);
        assertEquals(1, r.size());
        assertEquals(2, r.get(0).index);
    }

    @Test
    public void sortsRecentNameProgress() {
        assertEquals("emma", LibraryLogic.apply(items(), "", LibraryLogic.Filter.ALL, LibraryLogic.Sort.RECENT).get(0).name);
        assertEquals("Alice", LibraryLogic.apply(items(), "", LibraryLogic.Filter.ALL, LibraryLogic.Sort.NAME).get(0).name);
        assertEquals("emma", LibraryLogic.apply(items(), "", LibraryLogic.Filter.ALL, LibraryLogic.Sort.PROGRESS).get(0).name);
    }

    @Test
    public void highlightSearchMatchesTextNoteAndBook() {
        assertTrue(HighlightsActivity.matches("Fear is the mind-killer", null, "Dune", "mind"));
        assertTrue(HighlightsActivity.matches("x", "remember this", "Dune", "REMEMBER"));
        assertTrue(HighlightsActivity.matches("x", null, "Dune", "dun"));
        assertFalse(HighlightsActivity.matches("x", null, "Dune", "zzz"));
        assertTrue(HighlightsActivity.matches(null, null, null, ""));
    }

    @Test
    public void shareLineIncludesNoteAndBook() {
        String s = HighlightsActivity.shareLine("Dune", " Fear ", "key idea");
        assertTrue(s.contains("\"Fear\""));
        assertTrue(s.contains("Note: key idea"));
        assertTrue(s.endsWith("- Dune"));
    }
}
