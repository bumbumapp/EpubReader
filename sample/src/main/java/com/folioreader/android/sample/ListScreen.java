package com.folioreader.android.sample;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/** Shared shell of the bookmarks and highlights screens: header, search field, list, empty state. */
abstract class ListScreen extends AppCompatActivity implements EntryAdapter.Actions {

    private RecyclerView list;
    private View empty;
    private String query = "";

    abstract int titleRes();

    abstract int searchHintRes();

    abstract int emptyIconRes();

    abstract int emptyTitleRes();

    abstract int emptyBodyRes();

    /** Rows for the current query, already sorted. */
    abstract List<EntryAdapter.Row> rows(String query);

    boolean hasExport() {
        return false;
    }

    void onExport(List<EntryAdapter.Row> shown) {
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        ((TextView) findViewById(R.id.list_title)).setText(titleRes());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        empty = findViewById(R.id.list_empty);
        ((ImageView) findViewById(R.id.empty_icon)).setImageResource(emptyIconRes());
        ((TextView) findViewById(R.id.list_empty_title)).setText(emptyTitleRes());
        ((TextView) findViewById(R.id.list_empty_body)).setText(emptyBodyRes());
        EditText search = findViewById(R.id.list_search);
        search.setHint(searchHintRes());
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable e) {
                query = e.toString();
                reload();
            }
        });
        View export = findViewById(R.id.btn_export);
        if (hasExport()) {
            export.setVisibility(View.VISIBLE);
            export.setOnClickListener(v -> onExport(rows(query)));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    void reload() {
        List<EntryAdapter.Row> shown = rows(query);
        list.setAdapter(new EntryAdapter(shown, this));
        boolean none = shown.isEmpty();
        empty.setVisibility(none ? View.VISIBLE : View.GONE);
        list.setVisibility(none ? View.GONE : View.VISIBLE);
        findViewById(R.id.list_search).setVisibility(none && query.isEmpty() ? View.GONE : View.VISIBLE);
        findViewById(R.id.btn_export).setVisibility(hasExport() && !none ? View.VISIBLE : View.GONE);
    }

    void openInLibrary(String bookKey, @Nullable String locator) {
        Intent i = new Intent(this, HomeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(HomeActivity.EXTRA_OPEN_KEY, bookKey)
                .putExtra(HomeActivity.EXTRA_OPEN_LOCATOR, locator);
        startActivity(i);
    }

    void shareText(String subject, String text) {
        Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, subject).putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(send, getString(R.string.share)));
    }
}
