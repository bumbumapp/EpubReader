package com.folioreader.android.sample;

import android.text.format.DateUtils;

import com.folioreader.stats.Bookmark;
import com.folioreader.stats.BookmarkList;
import com.folioreader.stats.ReaderStats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** All bookmarks across books. Tapping one reopens the book at that spot. */
public class BookmarksActivity extends ListScreen {

    @Override
    int titleRes() {
        return R.string.bookmarks_title;
    }

    @Override
    int searchHintRes() {
        return R.string.bookmarks_search_hint;
    }

    @Override
    int emptyIconRes() {
        return R.drawable.ic_bookmark;
    }

    @Override
    int emptyTitleRes() {
        return R.string.bookmarks_empty_title;
    }

    @Override
    int emptyBodyRes() {
        return R.string.bookmarks_empty_body;
    }

    @Override
    List<EntryAdapter.Row> rows(String query) {
        Map<String, String> titles = LibraryMeta.titles(this);
        List<EntryAdapter.Row> out = new ArrayList<>();
        for (Bookmark b : BookmarkList.filter(ReaderStats.bookmarks(this), query)) {
            EntryAdapter.Row r = new EntryAdapter.Row();
            r.id = b.id;
            r.bookKey = b.bookKey;
            String book = titles.containsKey(b.bookKey) ? titles.get(b.bookKey) : b.bookTitle;
            r.bookTitle = book;
            r.title = book;
            r.note = null;
            r.sub = getString(R.string.bookmark_sub, b.chapter, Math.round(b.progress * 100f),
                    DateUtils.formatDateTime(this, b.created,
                            DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_MONTH));
            r.color = getResources().getColor(R.color.brand_accent, getTheme());
            r.locator = b.locator;
            r.shareText = book + " - " + b.chapter;
            out.add(r);
        }
        return out;
    }

    @Override
    public void onOpen(EntryAdapter.Row row) {
        openInLibrary(row.bookKey, row.locator);
    }

    @Override
    public void onShare(EntryAdapter.Row row) {
        shareText(row.bookTitle, row.shareText);
    }

    @Override
    public void onDelete(EntryAdapter.Row row) {
        ReaderStats.removeBookmark(this, row.id);
        reload();
    }
}
