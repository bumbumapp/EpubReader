package com.folioreader.stats;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class ReadingLogTest {

    private static final long MIN = 60_000L;

    @Test
    public void shortSessionsAreIgnoredAndLongOnesCapped() {
        assertEquals(0, ReadingLog.clampSession(2_000));
        assertEquals(10 * MIN, ReadingLog.clampSession(10 * MIN));
        assertEquals(ReadingLog.MAX_SESSION_MS, ReadingLog.clampSession(20 * 60 * MIN));
    }

    @Test
    public void dayIndexUsesLocalOffset() {
        long t = 23 * 60 * MIN; // 23:00 UTC on day 0
        assertEquals(0, ReadingLog.dayIndex(t, 0));
        assertEquals(1, ReadingLog.dayIndex(t, 2 * 60 * MIN)); // already tomorrow at UTC+2
        assertEquals(-1, ReadingLog.dayIndex(-1, 0));
    }

    @Test
    public void streakCountsBackFromTodayOrYesterday() {
        Map<Long, Long> d = new HashMap<>();
        ReadingLog.add(d, 10, 5 * MIN);
        ReadingLog.add(d, 9, 2 * MIN);
        ReadingLog.add(d, 8, 30_000L); // under a minute: breaks the streak
        assertEquals(2, ReadingLog.streak(d, 10));
        // nothing read yet today: yesterday's streak still counts
        assertEquals(2, ReadingLog.streak(d, 11));
        assertEquals(0, ReadingLog.streak(d, 12));
    }

    @Test
    public void lastDaysIsOldestFirst() {
        Map<Long, Long> d = new HashMap<>();
        ReadingLog.add(d, 10, 3);
        ReadingLog.add(d, 10, 4);
        ReadingLog.add(d, 8, 1);
        assertArrayEquals(new long[]{1, 0, 7}, ReadingLog.lastDays(d, 10, 3));
        assertEquals(8, ReadingLog.total(d));
    }

    @Test
    public void chapterProgressReachesOneOnLastChapter() {
        assertEquals(0.25f, ReadingLog.chapterProgress(0, 4), 0.001f);
        assertEquals(1f, ReadingLog.chapterProgress(3, 4), 0.001f);
        assertEquals(0f, ReadingLog.chapterProgress(2, 0), 0.001f);
    }

    @Test
    public void formatShowsHoursAndMinutes() {
        assertEquals("0 min", ReadingLog.format(0));
        assertEquals("<1 min", ReadingLog.format(30_000));
        assertEquals("12 min", ReadingLog.format(12 * MIN));
        assertEquals("1h 05m", ReadingLog.format(65 * MIN));
    }

    private static Bookmark bm(String book, String loc, long created, String chapter) {
        Bookmark b = new Bookmark();
        b.bookKey = book;
        b.locator = loc;
        b.created = created;
        b.chapter = chapter;
        b.bookTitle = book;
        return b;
    }

    @Test
    public void bookmarkDuplicatesAreRejectedAndIdsAreUnique() {
        List<Bookmark> list = new ArrayList<>();
        assertTrue(BookmarkList.add(list, bm("a", "L1", 100, "One")));
        assertFalse(BookmarkList.add(list, bm("a", "L1", 200, "One")));
        assertTrue(BookmarkList.add(list, bm("a", "L2", 100, "Two"))); // same timestamp, new id
        assertEquals(2, list.size());
        assertTrue(list.get(0).id != list.get(1).id);
    }

    @Test
    public void bookmarkFilterSortsNewestFirstAndSearchesChapter() {
        List<Bookmark> list = new ArrayList<>();
        BookmarkList.add(list, bm("dune", "L1", 100, "Arrival"));
        BookmarkList.add(list, bm("dune", "L2", 300, "Storm"));
        BookmarkList.add(list, bm("emma", "L3", 200, "Ball"));
        assertEquals("Storm", BookmarkList.filter(list, "").get(0).chapter);
        assertEquals(1, BookmarkList.filter(list, "BALL").size());
        assertEquals(2, BookmarkList.filter(list, "dune").size());
    }

    @Test
    public void removeAndRemoveBook() {
        List<Bookmark> list = new ArrayList<>();
        BookmarkList.add(list, bm("a", "L1", 1, "x"));
        BookmarkList.add(list, bm("b", "L2", 2, "y"));
        long id = list.get(0).id;
        assertTrue(BookmarkList.remove(list, id));
        assertFalse(BookmarkList.remove(list, id));
        BookmarkList.removeBook(list, "b");
        assertTrue(list.isEmpty());
    }

    @Test
    public void bookKeyStripsPathAndExtension() {
        assertEquals("my book", ReaderStats.key("/data/files/My Book.EPUB"));
        assertEquals("plain", ReaderStats.key("plain"));
    }
}
