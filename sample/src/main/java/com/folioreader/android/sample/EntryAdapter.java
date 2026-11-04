package com.folioreader.android.sample;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/** Card list used by the bookmarks and highlights screens. */
final class EntryAdapter extends RecyclerView.Adapter<EntryAdapter.Holder> {

    static final class Row {
        long id;
        String bookKey;
        String bookTitle;
        String title;
        String note;
        String sub;
        int color;
        String locator;
        String shareText;
    }

    interface Actions {
        void onOpen(Row row);

        void onShare(Row row);

        void onDelete(Row row);
    }

    private final List<Row> rows;
    private final Actions actions;

    EntryAdapter(List<Row> rows, Actions actions) {
        this.rows = rows;
        this.actions = actions;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_entry, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        final Row r = rows.get(position);
        h.title.setText(r.title);
        h.sub.setText(r.sub);
        if (r.note == null || r.note.trim().isEmpty()) {
            h.note.setVisibility(View.GONE);
        } else {
            h.note.setVisibility(View.VISIBLE);
            h.note.setText(h.itemView.getContext().getString(R.string.highlight_note_prefix, r.note));
        }
        h.color.setBackgroundColor(r.color);
        h.root.setOnClickListener(v -> actions.onOpen(r));
        h.share.setOnClickListener(v -> actions.onShare(r));
        h.delete.setOnClickListener(v -> actions.onDelete(r));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final View root, color, share, delete;
        final TextView title, note, sub;

        Holder(@NonNull View v) {
            super(v);
            root = v.findViewById(R.id.entry_root);
            color = v.findViewById(R.id.entry_color);
            title = v.findViewById(R.id.entry_title);
            note = v.findViewById(R.id.entry_note);
            sub = v.findViewById(R.id.entry_sub);
            share = v.findViewById(R.id.entry_share);
            delete = v.findViewById(R.id.entry_delete);
        }
    }
}
