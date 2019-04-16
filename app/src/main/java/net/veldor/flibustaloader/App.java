package net.veldor.flibustaloader;

import android.app.Application;
import android.arch.lifecycle.MutableLiveData;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.workers.StartTorWorker;

import java.io.File;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class App extends Application {

    public static final int MAX_BOOK_NUMBER = 548398;

    public static final String NEW_BOOKS = "http://flibustahezeous3.onion/new";
    private static final String PREFERENCE_VIEW_MODE = "view mode";
    public static final int VIEW_MODE_NORMAL = 1;
    public static final int VIEW_MODE_LIGHT = 2;
    public static final int VIEW_MODE_FAT = 3;
    public static final int VIEW_MODE_FAST = 4;
    public static final int VIEW_MODE_FAST_FAT = 5;
    public static final String BASE_URL = "http://flibustahezeous3.onion";
    public static final String BASE_BOOK_URL = "http://flibustahezeous3.onion/b/";

    public static final String TOR_FILES_LOCATION = "torfiles";
    private static final String PREFERENCE_NIGHT_MODE_ENABLED = "night mode";
    private static final String PREFERENCE_LAST_LOADED_URL = "last_loaded_url";
    private static final String PREFERENCE_LAST_SCROLL = "last_scroll";

    // место для хранения TOR клиента
    public final MutableLiveData<AndroidOnionProxyManager> mTorManager = new MutableLiveData<>();

    private static App instance;
    public File downloadedApkFile;
    public Uri updateDownloadUri;
    private SharedPreferences mSharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // запускаю tor
        OneTimeWorkRequest startTorWork = new OneTimeWorkRequest.Builder(StartTorWorker.class).build();
        WorkManager.getInstance().enqueue(startTorWork);

        // читаю настройки sharedPreferences

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    public static App getInstance() {
        return instance;
    }

    public int getViewMode() {
        return (mSharedPreferences.getInt(PREFERENCE_VIEW_MODE, VIEW_MODE_LIGHT));
    }

    public void switchViewMode(int type) {
        int mode = 1;
        switch (type) {
            case R.id.menuUseNormalStyle:
                mode = VIEW_MODE_NORMAL;
                break;
            case R.id.menuUseLightStyle:
                mode = VIEW_MODE_LIGHT;
                break;
            case R.id.menuUseLightFastStyle:
                mode = VIEW_MODE_FAST;
                break;
            case R.id.menuUseLightFatStyle:
                mode = VIEW_MODE_FAT;
                break;
            case R.id.menuUseFatFastStyle:
                mode = VIEW_MODE_FAST_FAT;
                break;
        }
        mSharedPreferences.edit().putInt(PREFERENCE_VIEW_MODE, mode).apply();
    }

    public void switchNightMode() {
        boolean currentValue = mSharedPreferences.getBoolean(PREFERENCE_NIGHT_MODE_ENABLED, false);
        if (currentValue) {
            mSharedPreferences.edit().putBoolean(PREFERENCE_NIGHT_MODE_ENABLED, false).apply();
        } else {
            mSharedPreferences.edit().putBoolean(PREFERENCE_NIGHT_MODE_ENABLED, true).apply();
        }
    }

    public boolean getNightMode() {
        return (mSharedPreferences.getBoolean(PREFERENCE_NIGHT_MODE_ENABLED, false));
    }

    public void restartTor() {
        // запускаю tor
        OneTimeWorkRequest startTorWork = new OneTimeWorkRequest.Builder(StartTorWorker.class).build();
        WorkManager.getInstance().enqueue(startTorWork);
    }

    public void setLastLoadedUrl(String url) {
        if (!url.equals("http://flibustahezeous3.onion/favicon.ico") && !url.equals("http://flibustahezeous3.onion/sites/default/files/bluebreeze_favicon.ico"))
            mSharedPreferences.edit().putString(PREFERENCE_LAST_LOADED_URL, url).apply();
    }

    public String getLastLoadedUrl() {
        Log.d("surprise", "App getLastLoadedUrl: get last url is " + mSharedPreferences.getString(PREFERENCE_LAST_LOADED_URL, BASE_BOOK_URL));
        return mSharedPreferences.getString(PREFERENCE_LAST_LOADED_URL, BASE_BOOK_URL);
    }
    public void setLastScroll(int mScroll) {
        mSharedPreferences.edit().putInt(PREFERENCE_LAST_SCROLL, mScroll).apply();
    }
}
