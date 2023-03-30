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


import static com.folioreader.Constants.TIMER_FINISHED;
import static com.folioreader.Constants.TIMES;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IInterface;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.folioreader.Config;
import com.folioreader.FolioReader;
import com.folioreader.model.HighLight;
import com.folioreader.model.Timers;
import com.folioreader.model.locators.ReadLocator;
import com.folioreader.ui.base.OnSaveHighlight;
import com.folioreader.util.AppUtil;
import com.folioreader.util.OnHighlightListener;
import com.folioreader.util.ReadLocatorListener;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity
        implements OnHighlightListener, ReadLocatorListener, FolioReader.OnClosedListener {

    private static final String LOG_TAG = HomeActivity.class.getSimpleName();
    private FolioReader folioReader;
    LinearLayout opentoEmpty;
    RecyclerView books;
    ArrayList<String>arrayList=new ArrayList<>();
    ArrayList<String>arrayListmodified=new ArrayList<>();
    ArrayList<String>arrayListtitle=new ArrayList<>();
    private InterstitialAd mInterstitialAd;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        folioReader = FolioReader.get()
                .setOnHighlightListener(this)
                .setReadLocatorListener(this)
                .setOnClosedListener(this);

        getHighlightsAndSave();
        books=findViewById(R.id.book_epubs);
        books.setHasFixedSize(true);
        books.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false));
       if (getArrayList()!=null && getArrayListmodified()!=null){
           arrayList=getArrayList();
           arrayListmodified=getArrayListmodified();


           for (int i=0;i<arrayList.size();i++){
               File file=new File(arrayList.get(i));
               if (!file.exists()){
                   arrayList.remove(arrayList.get(i));
                   arrayListmodified.remove(arrayListmodified.get(i));
               }
           }

           saveArrayList(arrayList);
           saveArrayListLink(arrayListmodified);
           setadapter(arrayList,arrayListmodified);
       }
        if (getIntent().getData()!=null) {
            Intent intent = getIntent();
            if (intent == null)
                return;
            String a = intent.getAction();
            if (a == null)
                return;
            Uri u = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (u == null){
                u = intent.getData();
            }

            if (u == null) {
                String t = intent.getStringExtra(Intent.EXTRA_TEXT); // handling SEND intents
                if (t != null)
                    u = Uri.parse(t);
            }
            if (u == null)
                return;
            Log.d("tag","URi"+u);
            getToFile2(u);

        }
        opentoEmpty=findViewById(R.id.toDoEmptyView);
        if (getArrayList()!=null){
            opentoEmpty.setVisibility(View.GONE);
        }
        if (getArrayList()==null){
            opentoEmpty.setVisibility(View.VISIBLE);
        }
        findViewById(R.id.btn_raw).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openfiles();
            }
        });

        loadBanners();
        loadInterstitialAds();
        Timers.timer().start();
    }

    private void openfiles() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/epub+zip");
        String[] mimetypes = {"application/epub+zip"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        startActivityForResult(intent,101);
    }
    private static String getFilePathForN(Uri uri, Context context) {
        Uri returnUri = uri;
        Cursor returnCursor = context.getContentResolver().query(returnUri, null, null, null, null);

        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        String name = (returnCursor.getString(nameIndex));
        String size = (Long.toString(returnCursor.getLong(sizeIndex)));
        File file = new File(context.getFilesDir(), name);
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(file);

            int read = 0;
            int maxBufferSize = 1 * 1024 * 1024;
            int bytesAvailable = inputStream.available();

            //int bufferSize = 1024;
            int bufferSize = Math.min(bytesAvailable, maxBufferSize);

            final byte[] buffers = new byte[bufferSize];
            while ((read = inputStream.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }
            Log.e("File Size", "Size " + file.length());
            inputStream.close();
            outputStream.close();
            Log.e("File Path", "Path " + file.getPath());
            Log.e("File Size", "Size " + file.length());
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        }
        return file.getPath();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 101:
                if (resultCode == RESULT_OK) {

                    Uri uri=data.getData();
                    String path=getDriveFilePath(uri,this);
                    String currentTime = new SimpleDateFormat("HH:mm,dd.MM.yyyy", Locale.getDefault()).format(new Date());

                    Log.d("Tag","Tags"+currentTime);
                    if (getArrayList()!=null){
                        arrayList=getArrayList();
                        arrayListmodified=getArrayListmodified();
                        for (int i=0;i<arrayList.size();i++){
                            arrayListtitle.add(arrayList.get(i).substring(arrayList.get(i).lastIndexOf("/")+1));
                        }
                        if (!arrayListtitle.contains(path.substring(path.lastIndexOf("/")+1))){
                            arrayList.add(path);
                            arrayListmodified.add(currentTime);
                        }
                        else {
                             currentTime= new SimpleDateFormat("HH:mm,dd.MM.yyyy", Locale.getDefault()).format(new Date());;
                             int i=arrayList.indexOf(path);
                             arrayListmodified.set(i, currentTime);
                        }
                        saveArrayList(arrayList);
                        saveArrayListLink(arrayListmodified);

                    }
                    if (getArrayList()==null){
                        arrayList.add(path);
                        arrayListmodified.add(currentTime);
                        saveArrayList(arrayList);
                        saveArrayListLink(arrayListmodified);
                    }

                    Log.d("Tag","Errs"+uri);
                    Config config = AppUtil.getSavedConfig(getApplicationContext());
                    if (config == null)
                        config = new Config();
                    config.setAllowedDirection(Config.AllowedDirection.VERTICAL_AND_HORIZONTAL);
                    Log.d("Tag", "Errrs" + path);
                    folioReader.setConfig(config, true)
                            .openBook(path);

                            setadapter(getArrayList(),getArrayListmodified());



                    if (getArrayList()!=null){
                        opentoEmpty.setVisibility(View.GONE);
                    }
                    if (getArrayList()==null){
                        opentoEmpty.setVisibility(View.VISIBLE);
                    }


                }
        }
    }
    public ArrayList<String> getArrayList(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Gson gson = new Gson();
        String json = prefs.getString("key", null);
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public void saveArrayList(ArrayList<String> list){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString("key", json);
        editor.apply();

    }
    public void saveArrayListLink(ArrayList<String> list){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString("keylink", json);
        editor.apply();

    }
    public ArrayList<String> getArrayListmodified(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Gson gson = new Gson();
        String json = prefs.getString("keylink", null);
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        return gson.fromJson(json, type);
    }
    private void loadBanners() {

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    private void getToFile2(Uri uri) {

        String path=getFilePathForN(uri,this);
        String currentTime = new SimpleDateFormat("HH:mm,dd.MM.yyyy", Locale.getDefault()).format(new Date());

        Log.d("Tag","Tags"+currentTime);
        if (getArrayList()!=null){
            arrayList=getArrayList();
            arrayListmodified=getArrayListmodified();
            for (int i=0;i<arrayList.size();i++){
                arrayListtitle.add(arrayList.get(i).substring(arrayList.get(i).lastIndexOf("/")+1));
            }
            if (!arrayListtitle.contains(path.substring(path.lastIndexOf("/")+1))){
                arrayList.add(path);
                arrayListmodified.add(currentTime);
                saveArrayList(arrayList);
                saveArrayListLink(arrayListmodified);
            }
            else {
                currentTime= new SimpleDateFormat("HH:mm,dd.MM.yyyy", Locale.getDefault()).format(new Date());;
                int i=arrayListtitle.indexOf(path.substring(path.lastIndexOf("/")+1));
                arrayListmodified.set(i, currentTime);
                saveArrayList(arrayList);
                saveArrayListLink(arrayListmodified);
            }


        }
        if (getArrayList()==null){
            arrayList.add(path);
            arrayListmodified.add(currentTime);
            saveArrayList(arrayList);
            saveArrayListLink(arrayListmodified);
        }

        Config config = AppUtil.getSavedConfig(getApplicationContext());
        if (config == null)
            config = new Config();
        config.setAllowedDirection(Config.AllowedDirection.VERTICAL_AND_HORIZONTAL);
        Log.d("Tag", "Errrs" + path);
        folioReader.setConfig(config, true)
                .openBook(path);
       // Toast.makeText(this,getArrayList().size()+"",Toast.LENGTH_LONG).show();
        if (getArrayListmodified()!=null){
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    setadapter(getArrayList(),getArrayListmodified());
                }
            },4000);
        }



    }

    private void setadapter(ArrayList<String> arrayList, ArrayList<String> arrayListmodified) {
        EpubAdabter epubAdabter=new EpubAdabter(this,arrayList,arrayListmodified, new ClickListener() {
            @Override
            public void onClick(final int positon) {
               if (TIMER_FINISHED){
                    if (mInterstitialAd.isLoaded()){
                        mInterstitialAd.show();
                        mInterstitialAd.setAdListener(new AdListener(){
                            @Override
                            public void onAdClosed() {
                                Timers.timer().start();
                                TIMER_FINISHED=false;
                                loadInterstitialAds();
                                onClickNewItem(positon);
                                super.onAdClosed();


                            }
                        });
                    }
                    else {
                       onClickNewItem(positon);
                    }
               }else {
                   onClickNewItem(positon);
               }


            }
        });
        books.setAdapter(epubAdabter);
    }
    private void loadInterstitialAds() {
        AdRequest adRequest = new AdRequest.Builder().build();
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_ads_id));
        mInterstitialAd.loadAd(adRequest);

    }

    private void onClickNewItem(int positon){
       ArrayList<String> arrayList1=new ArrayList();
       ArrayList<String> arrayListmodiffed1=new ArrayList();
       arrayList1=getArrayList();
       arrayListmodiffed1=getArrayListmodified();
       String currentTime = new SimpleDateFormat("HH:mm,dd.MM.yyyy", Locale.getDefault()).format(new Date());
       arrayListmodiffed1.set(positon,currentTime);
       saveArrayListLink(arrayListmodiffed1);
       setadapter(arrayList1,arrayListmodiffed1);
       Config config = AppUtil.getSavedConfig(getApplicationContext());
       if (config == null)
           config = new Config();
       config.setAllowedDirection(Config.AllowedDirection.VERTICAL_AND_HORIZONTAL);
       folioReader.setConfig(config, true)
               .openBook(arrayList1.get(positon));
   }
    private static String getDriveFilePath(Uri uri, Context context) {
        Uri returnUri = uri;
        Cursor returnCursor = context.getContentResolver().query(returnUri, null, null, null, null);
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();

        String name = (returnCursor.getString(nameIndex));
        String size = (Long.toString(returnCursor.getLong(sizeIndex)));
        File file = new File(context.getCacheDir(), name);
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(file);
            int read = 0;
            int maxBufferSize = 1 * 1024 * 1024;
            int bytesAvailable = inputStream.available();

            //int bufferSize = 1024;
            int bufferSize = Math.min(bytesAvailable, maxBufferSize);

            final byte[] buffers = new byte[bufferSize];
            while ((read = inputStream.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }
            Log.e("File Size", "Size " + file.length());
            inputStream.close();
            outputStream.close();
            Log.e("File Path", "Path " + file.getPath());
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        }
        return file.getPath();
    }


    private ReadLocator getLastReadLocator() {

        String jsonString = loadAssetTextAsString("Locators/LastReadLocators/last_read_locator_1.json");
        return ReadLocator.fromJson(jsonString);
    }
    public static String[] getTrailFromUri(Uri uri) {
        if ("org.courville.nova.provider".equals(uri.getHost()) && ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            String path = uri.getPath();
            if (path.startsWith("/external_files/")) {
                return path.substring("/external_files/".length()).split("/");
            }
        }
        return getTrailPathFromUri(uri).split("/");
    }

    public static String getTrailPathFromUri(Uri uri) {

        String path = uri.getPath();
        String[] array = path.split(":");
        if (array.length > 1) {
            return array[array.length - 1];
        } else {
            return path;
        }
    }
    @Override
    public void saveReadLocator(ReadLocator readLocator) {
        Log.i(LOG_TAG, "-> saveReadLocator -> " + readLocator.toJson());
    }

    /*
     * For testing purpose, we are getting dummy highlights from asset. But you can get highlights from your server
     * On success, you can save highlights to FolioReader DB.
     */
    private void getHighlightsAndSave() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<HighLight> highlightList = null;
                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    highlightList = objectMapper.readValue(
                            loadAssetTextAsString("highlights/highlights_data.json"),
                            new TypeReference<List<HighlightData>>() {
                            });
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (highlightList == null) {
                    folioReader.saveReceivedHighLights(highlightList, new OnSaveHighlight() {
                        @Override
                        public void onFinished() {
                            //You can do anything on successful saving highlight list
                        }
                    });
                }
            }
        }).start();
    }

    private String loadAssetTextAsString(String name) {
        BufferedReader in = null;
        try {
            StringBuilder buf = new StringBuilder();
            InputStream is = getAssets().open(name);
            in = new BufferedReader(new InputStreamReader(is));

            String str;
            boolean isFirst = true;
            while ((str = in.readLine()) != null) {
                if (isFirst)
                    isFirst = false;
                else
                    buf.append('\n');
                buf.append(str);
            }
            return buf.toString();
        } catch (IOException e) {
            Log.e("HomeActivity", "Error opening asset " + name);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e("HomeActivity", "Error closing asset " + name);
                }
            }
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FolioReader.clear();
    }

    @Override
    public void onHighlight(HighLight highlight, HighLight.HighLightAction type) {
//        Toast.makeText(this,
//                "highlight id = " + highlight.getUUID() + " type = " + type,
//                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFolioReaderClosed() {
        Log.v(LOG_TAG, "-> onFolioReaderClosed");
    }



}