/*
 * Copyright (C) 2016 Pedro Paulo de Amorim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.folioreader.android.sample;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.IntentCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.folioreader.Config;
import com.folioreader.FolioReader;
import com.folioreader.model.HighLight;
import com.folioreader.model.locators.ReadLocator;
import com.folioreader.util.AppUtil;
import com.folioreader.util.OnHighlightListener;
import com.folioreader.util.ReadLocatorListener;
import com.folioreader.stats.ReaderStats;
import com.folioreader.stats.ReadingLog;
import com.folioreader.ads.AdsConsent;
import com.folioreader.ads.BannerHelper;
import com.folioreader.ads.InterstitialHelper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity
        implements OnHighlightListener, ReadLocatorListener, FolioReader.OnClosedListener {

    private static final String LOG_TAG = HomeActivity.class.getSimpleName();
    private static final String KEY_PATHS = "key";
    private static final String KEY_MODIFIED = "keylink";

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private FolioReader folioReader;
    private LinearLayout emptyView;
    private RecyclerView books;
    private Toolbar toolbar;
    private boolean privacyItemAdded;

    static final String EXTRA_OPEN_KEY = "open_book_key";
    static final String EXTRA_OPEN_LOCATOR = "open_locator";

    private String query = "";
    private LibraryLogic.Filter filter = LibraryLogic.Filter.ALL;
    private LibraryLogic.Sort sort = LibraryLogic.Sort.RECENT;
    private final List<TextView> chipViews = new ArrayList<>();
    private TextView sortButton;

    private final ActivityResultLauncher<String[]> openDocument =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    importAndOpen(uri);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        // Android 15+ edge-to-edge: the status bar is drawn over the light window background.
        androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);

        folioReader = FolioReader.get()
                .setOnHighlightListener(this)
                .setReadLocatorListener(this)
                .setOnClosedListener(this);

        toolbar = findViewById(R.id.toolbar);
        emptyView = findViewById(R.id.toDoEmptyView);
        books = findViewById(R.id.book_epubs);
        books.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        findViewById(R.id.btn_raw).setOnClickListener(v -> openFiles());
        findViewById(R.id.tile_bookmarks).setOnClickListener(
                v -> startActivity(new Intent(this, BookmarksActivity.class)));
        findViewById(R.id.tile_highlights).setOnClickListener(
                v -> startActivity(new Intent(this, HighlightsActivity.class)));
        findViewById(R.id.tile_stats).setOnClickListener(
                v -> startActivity(new Intent(this, StatsActivity.class)));
        setupLibraryTools();
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        ((android.widget.TextView) findViewById(R.id.home_greeting)).setText(hour >= 5 && hour < 12
                ? R.string.bento_greeting_morning : hour >= 12 && hour < 18
                ? R.string.bento_greeting_afternoon : R.string.bento_greeting_evening);

        refreshList();

        if (savedInstanceState == null) {
            handleViewIntent(getIntent());
        }

        AdsConsent.gather(this, () -> {
            BannerHelper.load(this, (ViewGroup) findViewById(R.id.adView));
            InterstitialHelper.preload(this);
            addPrivacyOptionsIfRequired();
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleViewIntent(intent);
    }

    private void addPrivacyOptionsIfRequired() {
        if (privacyItemAdded || !AdsConsent.isPrivacyOptionsRequired(this)) {
            return;
        }
        privacyItemAdded = true;
        toolbar.getMenu().add(R.string.privacy_options).setOnMenuItemClickListener(item -> {
            AdsConsent.showPrivacyOptions(this);
            return true;
        });
    }

    private void handleViewIntent(@Nullable Intent intent) {
        if (intent != null && intent.hasExtra(EXTRA_OPEN_KEY)) {
            String key = intent.getStringExtra(EXTRA_OPEN_KEY);
            String locator = intent.getStringExtra(EXTRA_OPEN_LOCATOR);
            intent.removeExtra(EXTRA_OPEN_KEY);
            intent.removeExtra(EXTRA_OPEN_LOCATOR);
            for (String path : getPaths()) {
                if (ReaderStats.key(BookLibrary.fileName(path)).equals(key) && new File(path).exists()) {
                    openBook(path, locator);
                    return;
                }
            }
            Toast.makeText(this, R.string.bookmark_book_missing, Toast.LENGTH_LONG).show();
            return;
        }
        if (intent == null || intent.getAction() == null) {
            return;
        }
        Uri uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri.class);
        if (uri == null) {
            uri = intent.getData();
        }
        if (uri == null) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT); // SEND intents
            if (text != null) {
                uri = Uri.parse(text);
            }
        }
        if (uri != null && uri.getScheme() != null) {
            importAndOpen(uri);
        }
    }

    private void openFiles() {
        try {
            openDocument.launch(new String[]{"application/epub+zip"});
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_file_picker, Toast.LENGTH_LONG).show();
        }
    }

    /** Copies the picked document into app storage off the main thread, then opens it. */
    private void importAndOpen(Uri uri) {
        final Context app = getApplicationContext();
        io.execute(() -> {
            final String path = copyToAppStorage(app, uri);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (path == null) {
                    Toast.makeText(this, R.string.cannot_open_book, Toast.LENGTH_LONG).show();
                    return;
                }
                ArrayList<String> paths = getPaths();
                ArrayList<String> modified = getModified();
                BookLibrary.addOrTouch(paths, modified, path, now());
                save(paths, modified);
                refreshList();
                openBook(path);
            });
        });
    }

    @Nullable
    static String displayName(Context context, Uri uri) {
        String name = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor c = context.getContentResolver().query(uri, null, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) {
                        name = c.getString(idx);
                    }
                }
            } catch (Exception e) {
                Log.w(LOG_TAG, "displayName", e);
            }
        }
        if (name == null) {
            name = uri.getLastPathSegment();
        }
        return BookFiles.safeEpubName(name);
    }

    @Nullable
    private static String copyToAppStorage(Context context, Uri uri) {
        String name = displayName(context, uri);
        File dir = new File(context.getFilesDir(), "books");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            return null;
        }
        File file = new File(dir, name);
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(file)) {
            if (in == null) {
                return null;
            }
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "copyToAppStorage", e);
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            return null;
        }
        return file.length() > 0 ? file.getPath() : null;
    }

    private void setupLibraryTools() {
        LinearLayout chips = findViewById(R.id.filter_chips);
        final LibraryLogic.Filter[] filters = LibraryLogic.Filter.values();
        final int[] labels = {R.string.filter_all, R.string.filter_favorites, R.string.filter_reading,
                R.string.filter_finished};
        for (int i = 0; i < filters.length; i++) {
            final LibraryLogic.Filter f = filters[i];
            TextView chip = new TextView(this);
            chip.setText(labels[i]);
            chip.setTextSize(13);
            chip.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this, R.font.jakarta_semibold));
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setMinWidth(dp(48));
            chip.setPadding(dp(14), 0, dp(14), 0);
            chip.setClickable(true);
            chip.setFocusable(true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(40));
            lp.setMarginEnd(dp(8));
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> {
                filter = f;
                refreshList();
            });
            chips.addView(chip);
            chipViews.add(chip);
        }
        sortButton = findViewById(R.id.sort_button);
        sortButton.setOnClickListener(v -> {
            LibraryLogic.Sort[] all = LibraryLogic.Sort.values();
            sort = all[(sort.ordinal() + 1) % all.length];
            refreshList();
        });
        ((EditText) findViewById(R.id.search_books)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable e) {
                query = e.toString();
                refreshList();
            }
        });
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void refreshList() {
        ArrayList<String> paths = getPaths();
        ArrayList<String> modified = getModified();
        BookLibrary.prune(paths, modified, BookLibrary.FILE_EXISTS);
        save(paths, modified);

        Set<String> favs = LibraryMeta.favorites(this);
        Map<String, Long> opened = LibraryMeta.opened(this);
        Map<String, Long> readMs = ReaderStats.bookMs(this);
        List<LibraryLogic.Item> all = new ArrayList<>();
        for (int i = 0; i < paths.size(); i++) {
            String name = BookLibrary.fileName(paths.get(i));
            String key = ReaderStats.key(name);
            Long o = opened.get(key);
            Long ms = readMs.get(key);
            all.add(new LibraryLogic.Item(i, name.endsWith(".epub") ? name.substring(0, name.length() - 5) : name,
                    favs.contains(key), ReaderStats.progress(this, name), o == null ? 0 : o,
                    ms == null ? 0 : ms));
        }
        List<LibraryLogic.Item> shown = LibraryLogic.apply(all, query, filter, sort);
        books.setAdapter(new EpubAdabter(this, shown, new EpubAdabter.Actions() {
            @Override
            public void onOpen(LibraryLogic.Item item) {
                InterstitialHelper.showIfReady(HomeActivity.this, () -> onBookClicked(item.index));
            }

            @Override
            public void onFavorite(LibraryLogic.Item item) {
                LibraryMeta.toggleFavorite(HomeActivity.this, item.name + ".epub");
                refreshList();
            }

            @Override
            public void onMore(LibraryLogic.Item item, View anchor) {
                showBookMenu(item, anchor);
            }
        }));
        books.setVisibility(shown.isEmpty() ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
        boolean noBooks = all.isEmpty();
        ((TextView) findViewById(R.id.empty_title)).setText(noBooks ? R.string.home_empty_title : R.string.empty_filtered_title);
        ((TextView) findViewById(R.id.empty_body)).setText(noBooks ? R.string.home_empty_body : R.string.empty_filtered_body);
        findViewById(R.id.library_tools).setVisibility(noBooks ? View.GONE : View.VISIBLE);

        sortButton.setVisibility(noBooks ? View.GONE : View.VISIBLE);
        int[] on = {R.string.sort_recent, R.string.sort_name, R.string.sort_progress};
        sortButton.setText(on[sort.ordinal()]);
        for (int i = 0; i < chipViews.size(); i++) {
            boolean sel = LibraryLogic.Filter.values()[i] == filter;
            TextView c = chipViews.get(i);
            c.setBackgroundResource(sel ? R.drawable.bg_chip_on : R.drawable.bg_chip_off);
            c.setTextColor(getResources().getColor(sel ? R.color.brand_onInk : R.color.brand_text, getTheme()));
        }
        updateTiles();
    }

    private void updateTiles() {
        int n = ReaderStats.bookmarks(this).size();
        ((TextView) findViewById(R.id.tile_bookmarks_sub)).setText(getString(R.string.tile_count_saved, n));
        int hl = HighlightsActivity.loadAll(this).size();
        ((TextView) findViewById(R.id.tile_highlights_sub)).setText(getString(R.string.tile_count_saved, hl));
        long today = ReadingLog.get(ReaderStats.days(this), ReaderStats.today());
        ((TextView) findViewById(R.id.tile_stats_sub)).setText(
                getString(R.string.tile_today_min, ReadingLog.format(today)));
    }

    private void showBookMenu(LibraryLogic.Item item, View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 1, 0, R.string.menu_share_book);
        menu.getMenu().add(0, 2, 1, R.string.menu_remove_book);
        menu.setOnMenuItemClickListener(m -> {
            ArrayList<String> paths = getPaths();
            if (item.index < 0 || item.index >= paths.size()) {
                return true;
            }
            String path = paths.get(item.index);
            if (m.getItemId() == 1) {
                shareBook(path);
            } else {
                confirmRemove(path);
            }
            return true;
        });
        menu.show();
    }

    private void shareBook(String path) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", new File(path));
            Intent send = new Intent(Intent.ACTION_SEND)
                    .setType("application/epub+zip")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, getString(R.string.menu_share_book)));
        } catch (RuntimeException e) {
            Log.w(LOG_TAG, "shareBook", e);
            Toast.makeText(this, R.string.cannot_open_book, Toast.LENGTH_LONG).show();
        }
    }

    private void confirmRemove(String path) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.remove_book_title)
                .setMessage(R.string.remove_book_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.remove, (d, w) -> {
                    String name = BookLibrary.fileName(path);
                    ArrayList<String> paths = getPaths();
                    ArrayList<String> modified = getModified();
                    BookLibrary.prune(paths, modified, BookLibrary.FILE_EXISTS);
                    int i = paths.indexOf(path);
                    if (i >= 0) {
                        paths.remove(i);
                        modified.remove(i);
                    }
                    save(paths, modified);
                    //noinspection ResultOfMethodCallIgnored
                    new File(path).delete();
                    ReaderStats.forgetBook(this, name);
                    LibraryMeta.forget(this, name);
                    refreshList();
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    private void onBookClicked(int position) {
        ArrayList<String> paths = getPaths();
        ArrayList<String> modified = getModified();
        BookLibrary.prune(paths, modified, BookLibrary.FILE_EXISTS);
        if (position < 0 || position >= paths.size()) {
            refreshList();
            return;
        }
        modified.set(position, now());
        save(paths, modified);
        String path = paths.get(position);
        refreshList();
        openBook(path, null);
    }

    private void openBook(String path) {
        openBook(path, null);
    }

    /** Opens the book at the given position, or where the reader left off when none is given. */
    private void openBook(String path, @Nullable String locatorJson) {
        String name = BookLibrary.fileName(path);
        LibraryMeta.touch(this, name);
        if (locatorJson == null) {
            locatorJson = ReaderStats.locator(this, name);
        }
        ReadLocator locator = locatorJson != null ? ReadLocator.fromJson(locatorJson) : null;
        Config config = AppUtil.getSavedConfig(getApplicationContext());
        if (config == null) {
            config = new Config();
        }
        config.setAllowedDirection(Config.AllowedDirection.VERTICAL_AND_HORIZONTAL);
        folioReader.setReadLocator(locator).setConfig(config, true).openBook(path);
    }

    private static String now() {
        return new SimpleDateFormat("HH:mm,dd.MM.yyyy", Locale.getDefault()).format(new Date());
    }

    private SharedPreferences prefs() {
        // Same file as the deprecated PreferenceManager.getDefaultSharedPreferences(): keeps saved lists.
        return getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
    }

    private ArrayList<String> readList(String key) {
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        try {
            ArrayList<String> list = new Gson().fromJson(prefs().getString(key, null), type);
            return BookLibrary.orEmpty(list);
        } catch (RuntimeException e) {
            return new ArrayList<>();
        }
    }

    private ArrayList<String> getPaths() {
        return readList(KEY_PATHS);
    }

    private ArrayList<String> getModified() {
        return readList(KEY_MODIFIED);
    }

    private void save(ArrayList<String> paths, ArrayList<String> modified) {
        Gson gson = new Gson();
        prefs().edit()
                .putString(KEY_PATHS, gson.toJson(paths))
                .putString(KEY_MODIFIED, gson.toJson(modified))
                .apply();
    }

    @Override
    public void saveReadLocator(ReadLocator readLocator) {
        Log.i(LOG_TAG, "-> saveReadLocator -> " + readLocator.toJson());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdown();
        FolioReader.clear();
    }

    @Override
    public void onHighlight(HighLight highlight, HighLight.HighLightAction type) {
    }

    @Override
    public void onFolioReaderClosed() {
        Log.v(LOG_TAG, "-> onFolioReaderClosed");
        refreshList();
    }
}
