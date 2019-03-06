package net.veldor.flibustaloader;

import android.app.Application;
import android.arch.lifecycle.MutableLiveData;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.workers.StartTorWorker;

import java.io.File;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class App extends Application {


    private static final String PREFERENCE_LIGHT_MODE_ENABLED = "light mode";
    public static final String BASE_URL = "http://flibustahezeous3.onion";

    public static final String TOR_FILES_LOCATION = "torfiles";
    public String currentLoadedUrl = "http://flibustahezeous3.onion/";

    public boolean updateDownloadInProgress = false;

    // место для хранения TOR клиента
    public MutableLiveData<AndroidOnionProxyManager> mTorManager = new MutableLiveData<>();

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

    public boolean getViewMode(){
        return (mSharedPreferences.getBoolean(PREFERENCE_LIGHT_MODE_ENABLED, false));
    }

    public void swtichViewMode(){
        boolean currentValue = mSharedPreferences.getBoolean(PREFERENCE_LIGHT_MODE_ENABLED, false);
        if(currentValue){
            mSharedPreferences.edit().putBoolean(PREFERENCE_LIGHT_MODE_ENABLED, false).apply();
        }
        else {
            mSharedPreferences.edit().putBoolean(PREFERENCE_LIGHT_MODE_ENABLED, true).apply();
        }
    }
}
