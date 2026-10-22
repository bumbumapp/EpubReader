package com.folioreader.android.sample;

import android.app.Application;

public class EpubApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        EdgeToEdgeCallbacks.register(this);
    }
}
