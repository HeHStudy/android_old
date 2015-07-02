package org.healtheheartstudy;

import android.app.Application;

import org.healtheheartstudy.network.RequestManager;

import timber.log.Timber;

public class MainApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        RequestManager.init(this);
        if (BuildConfig.DEBUG) Timber.plant(new Timber.DebugTree());
    }

}
