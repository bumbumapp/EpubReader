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
import android.widget.LinearLayout;
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
import java.util.Locale;
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
        books.setHasFixedSize(true);
        books.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        findViewById(R.id.btn_raw).setOnClickListener(v -> openFiles());

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

    private void refreshList() {
        ArrayList<String> paths = getPaths();
        ArrayList<String> modified = getModified();
        BookLibrary.prune(paths, modified, BookLibrary.FILE_EXISTS);
        save(paths, modified);
        books.setAdapter(new EpubAdabter(this, paths, modified,
                position -> InterstitialHelper.showIfReady(this, () -> onBookClicked(position))));
        emptyView.setVisibility(paths.isEmpty() ? View.VISIBLE : View.GONE);
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
        openBook(path);
    }

    private void openBook(String path) {
        Config config = AppUtil.getSavedConfig(getApplicationContext());
        if (config == null) {
            config = new Config();
        }
        config.setAllowedDirection(Config.AllowedDirection.VERTICAL_AND_HORIZONTAL);
        folioReader.setConfig(config, true).openBook(path);
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
