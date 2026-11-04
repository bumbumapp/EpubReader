package com.folioreader.android.sample;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import com.folioreader.stats.ReadingLog;

public class EpubAdabter extends RecyclerView.Adapter<EpubAdabter.ViewHolder> {

    interface Actions {
        void onOpen(LibraryLogic.Item item);

        void onFavorite(LibraryLogic.Item item);

        void onMore(LibraryLogic.Item item, View anchor);
    }

    private final Context context;
    private final List<LibraryLogic.Item> items;
    private final Actions actions;

    EpubAdabter(Context context, List<LibraryLogic.Item> items, Actions actions) {
        this.context = context;
        this.items = items;
        this.actions = actions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_books, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final LibraryLogic.Item item = items.get(position);
        holder.title.setText(item.name);
        int percent = Math.round(item.progress * 100f);
        String opened = item.lastOpened > 0
                ? DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(item.lastOpened))
                : context.getString(R.string.never_opened);
        holder.modified.setText(context.getString(R.string.book_meta, percent,
                ReadingLog.format(item.readMs), opened));
        holder.progress.setProgress(percent);
        holder.fav.setImageResource(item.favorite ? R.drawable.ic_star : R.drawable.ic_star_border);
        holder.openFile.setOnClickListener(v -> actions.onOpen(item));
        holder.fav.setOnClickListener(v -> actions.onFavorite(item));
        holder.more.setOnClickListener(v -> actions.onMore(item, v));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title, modified;
        final View openFile;
        final ImageView imageView;
        final ProgressBar progress;
        final ImageButton fav, more;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.book_title);
            imageView = itemView.findViewById(R.id.book_cover);
            openFile = itemView.findViewById(R.id.top);
            modified = itemView.findViewById(R.id.book_modififed);
            progress = itemView.findViewById(R.id.book_progress);
            fav = itemView.findViewById(R.id.book_fav);
            more = itemView.findViewById(R.id.book_more);
        }
    }
}
