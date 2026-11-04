package com.folioreader.android.sample;

import android.content.Context;
import android.graphics.Color;

import com.folioreader.FolioReader;
import com.folioreader.model.HighlightImpl;
import com.folioreader.model.sqlite.HighLightTable;
import com.folioreader.stats.ReaderStats;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Every highlight and note made in any book, searchable, shareable and exportable as text. */
public class HighlightsActivity extends ListScreen {

    /** A highlight with the title of the book it belongs to. */
    static final class Entry {
        HighlightImpl h;
        String bookKey;
        String bookTitle;
    }

    static List<Entry> loadAll(Context c) {
        List<Entry> out = new ArrayList<>();
        try {
            FolioReader.get(); // opens the highlight database
            Map<String, String> titles = LibraryMeta.titles(c);
            for (Map.Entry<String, String> id : ReaderStats.bookIds(c).entrySet()) {
                for (HighlightImpl h : HighLightTable.getAllHighlights(id.getKey())) {
                    Entry e = new Entry();
                    e.h = h;
                    e.bookKey = id.getValue();
                    e.bookTitle = titles.containsKey(e.bookKey) ? titles.get(e.bookKey) : e.bookKey;
                    out.add(e);
                }
            }
        } catch (RuntimeException ignored) {
            // The database is not ready yet: show an empty list instead of crashing.
        }
        Collections.sort(out, (a, b) -> {
            long x = a.h.getDate() == null ? 0 : a.h.getDate().getTime();
            long y = b.h.getDate() == null ? 0 : b.h.getDate().getTime();
            return Long.compare(y, x);
        });
        return out;
    }

    static boolean matches(String content, String note, String book, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return true;
        }
        return (content != null && content.toLowerCase(Locale.ROOT).contains(q))
                || (note != null && note.toLowerCase(Locale.ROOT).contains(q))
                || (book != null && book.toLowerCase(Locale.ROOT).contains(q));
    }

    static int colorFor(String type) {
        if (type == null) {
            return Color.parseColor("#F2C94C");
        }
        switch (type) {
            case "highlight_green":
                return Color.parseColor("#3DBE7F");
            case "highlight_blue":
                return Color.parseColor("#4C9BE8");
            case "highlight_pink":
                return Color.parseColor("#E8629B");
            case "highlight_underline":
                return Color.parseColor("#5B6275");
            default:
                return Color.parseColor("#F2C94C");
        }
    }

    static String shareLine(String book, String content, String note) {
        StringBuilder sb = new StringBuilder();
        sb.append('"').append(content == null ? "" : content.trim()).append('"');
        if (note != null && !note.trim().isEmpty()) {
            sb.append("\n  Note: ").append(note.trim());
        }
        sb.append("\n  - ").append(book);
        return sb.toString();
    }

    @Override
    int titleRes() {
        return R.string.highlights_title;
    }

    @Override
    int searchHintRes() {
        return R.string.highlights_search_hint;
    }

    @Override
    int emptyIconRes() {
        return R.drawable.ic_highlight;
    }

    @Override
    int emptyTitleRes() {
        return R.string.highlights_empty_title;
    }

    @Override
    int emptyBodyRes() {
        return R.string.highlights_empty_body;
    }

    @Override
    boolean hasExport() {
        return true;
    }

    @Override
    List<EntryAdapter.Row> rows(String query) {
        List<EntryAdapter.Row> out = new ArrayList<>();
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
        for (Entry e : loadAll(this)) {
            if (!matches(e.h.getContent(), e.h.getNote(), e.bookTitle, query)) {
                continue;
            }
            EntryAdapter.Row r = new EntryAdapter.Row();
            r.id = e.h.getId();
            r.bookKey = e.bookKey;
            r.bookTitle = e.bookTitle;
            r.title = e.h.getContent() == null ? "" : e.h.getContent().trim();
            r.note = e.h.getNote();
            Date d = e.h.getDate();
            r.sub = e.bookTitle + (d != null ? "  •  " + df.format(d) : "");
            r.color = colorFor(e.h.getType());
            r.shareText = shareLine(e.bookTitle, e.h.getContent(), e.h.getNote());
            out.add(r);
        }
        return out;
    }

    @Override
    void onExport(List<EntryAdapter.Row> shown) {
        StringBuilder sb = new StringBuilder();
        for (EntryAdapter.Row r : shown) {
            sb.append(r.shareText).append("\n\n");
        }
        shareText(getString(R.string.highlights_export_title), sb.toString().trim());
    }

    @Override
    public void onOpen(EntryAdapter.Row row) {
        openInLibrary(row.bookKey, null);
    }

    @Override
    public void onShare(EntryAdapter.Row row) {
        shareText(row.bookTitle, row.shareText);
    }

    @Override
    public void onDelete(EntryAdapter.Row row) {
        HighLightTable.deleteHighlight((int) row.id);
        reload();
    }
}
