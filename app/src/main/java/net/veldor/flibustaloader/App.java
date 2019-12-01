package net.veldor.flibustaloader;

import android.app.Application;
import android.arch.lifecycle.MutableLiveData;
import android.arch.persistence.room.Room;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.database.AppDatabase;
import net.veldor.flibustaloader.selections.Author;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.selections.FoundedSequence;
import net.veldor.flibustaloader.selections.Genre;
import net.veldor.flibustaloader.workers.StartTorWorker;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import static android.support.v7.app.AppCompatDelegate.MODE_NIGHT_NO;
import static android.support.v7.app.AppCompatDelegate.MODE_NIGHT_YES;

public class App extends Application {

    public static final int MAX_BOOK_NUMBER = 548398;

    public static final File DOWNLOAD_FOLDER_LOCATION = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

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
    private static final String PREFERENCE_DOWNLOAD_LOCATION = "download_location";


    private static final String PREFERENCE_CONTENT_TYPE_MODE = "content mode";
    public static final int CONTENT_MODE_WEB_VIEW = 1;
    private static final int CONTENT_MODE_ODPS = 0;
    public static int sSearchType = ODPSActivity.SEARCH_BOOKS;

    // место для хранения TOR клиента
    public final MutableLiveData<AndroidOnionProxyManager> mTorManager = new MutableLiveData<>();

    // место для хранения текста ответа поиска
    public final MutableLiveData<String> mSearchResult = new MutableLiveData<>();
    // место для хранения результатов парсинга ответа
    public final MutableLiveData<ArrayList> mParsedResult = new MutableLiveData<>();
    // место для хранения текста ответа поиска
    public final MutableLiveData<String> mSearchTitle = new MutableLiveData<>();
    // место для хранения выбранного писателя
    public final MutableLiveData<Author> mSelectedAuthor = new MutableLiveData<>();
    // место для хранения выбранной серии
    public final MutableLiveData<FoundedSequence> mSelectedSequence = new MutableLiveData<>();
    // место для хранения выбранного жанра
    public final MutableLiveData<Genre> mSelectedGenre = new MutableLiveData<>();
    // место для хранения выбора писателей
    public final MutableLiveData<ArrayList<Author>> mSelectedAuthors = new MutableLiveData<>();

    private static App instance;
    public File downloadedApkFile;
    public Uri updateDownloadUri;
    public final MutableLiveData<ArrayList<DownloadLink>> mDownloadLinksList = new MutableLiveData<>();
    public ArrayList<String> mSearchHistory = new ArrayList<>();
    public MutableLiveData<ArrayList<FoundedSequence>> mSelectedSequences = new MutableLiveData<>();
    public String mNextPageUrl;
    public String mResponce;
    private SharedPreferences mSharedPreferences;
    private AppDatabase mDb;

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        // запускаю tor
        OneTimeWorkRequest startTorWork = new OneTimeWorkRequest.Builder(StartTorWorker.class).build();
        WorkManager.getInstance().enqueue(startTorWork);

        // читаю настройки sharedPreferences

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (getNightMode()) {
            AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES);
        }
        else{
            AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO);
        }

        mDb =  Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "database").build();
    }

    public static App getInstance() {
        return instance;
    }

    public int getViewMode() {
        return (mSharedPreferences.getInt(PREFERENCE_VIEW_MODE, VIEW_MODE_LIGHT));
    }

    public AppDatabase getmDb(){
        return mDb;
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

    public File getDownloadFolder(){
        // возвращу папку для закачек
        String download_location =  mSharedPreferences.getString(PREFERENCE_DOWNLOAD_LOCATION, DOWNLOAD_FOLDER_LOCATION.toString());
        File dl = new File(download_location);
        if(dl.isDirectory()){
            Log.d("surprise", "App getDownloadFolder: found changed dir " + dl.toString());
            return dl;
        }
        else{
            Log.d("surprise", "App getDownloadFolder: not found changed dir " + dl);
            return DOWNLOAD_FOLDER_LOCATION;
        }
    }

    public void setDownloadFolder(Uri uri) {
        File dl = new File(uri.getPath());
        Log.d("surprise", "App setDownloadFolder: uri is " + uri.getPath());
        if(dl.exists()){
            Log.d("surprise", "App setDownloadFolder: destination exists");
        }
        else{
            Log.d("surprise", "App setDownloadFolder: destination not found");
        }
        mSharedPreferences.edit().putString(PREFERENCE_DOWNLOAD_LOCATION, uri.getPath()).apply();
    }

    public boolean isODPS() {
        return mSharedPreferences.getInt(PREFERENCE_CONTENT_TYPE_MODE, CONTENT_MODE_WEB_VIEW) != CONTENT_MODE_WEB_VIEW;
    }


    public int getContentTypeMode(){
        return mSharedPreferences.getInt(PREFERENCE_CONTENT_TYPE_MODE, CONTENT_MODE_WEB_VIEW);
    }


    public void switchODPSMode() {
        int contentTypeMode = getContentTypeMode();
        if(contentTypeMode == CONTENT_MODE_WEB_VIEW){
            Log.d("surprise", "App switchODPSMode: switch to ODPS");
            mSharedPreferences.edit().putInt(PREFERENCE_CONTENT_TYPE_MODE, CONTENT_MODE_ODPS).apply();
        }
        else{
            Log.d("surprise", "App switchODPSMode: switch to webView");
            mSharedPreferences.edit().putInt(PREFERENCE_CONTENT_TYPE_MODE, CONTENT_MODE_WEB_VIEW).apply();
        }
    }
}
