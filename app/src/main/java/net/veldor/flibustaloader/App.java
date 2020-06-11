package net.veldor.flibustaloader;

import android.app.Application;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseBooleanArray;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.room.Room;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.database.AppDatabase;
import net.veldor.flibustaloader.database.dao.BooksDownloadScheduleDao;
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule;
import net.veldor.flibustaloader.notificatons.Notificator;
import net.veldor.flibustaloader.selections.Author;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.selections.FoundedSequence;
import net.veldor.flibustaloader.ui.OPDSActivity;
import net.veldor.flibustaloader.utils.SubscribeAuthors;
import net.veldor.flibustaloader.utils.SubscribeBooks;
import net.veldor.flibustaloader.utils.SubscribeSequences;
import net.veldor.flibustaloader.utils.URLHelper;
import net.veldor.flibustaloader.workers.DownloadBooksWorker;
import net.veldor.flibustaloader.workers.ParseWebRequestWorker;
import net.veldor.flibustaloader.workers.StartTorWorker;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static net.veldor.flibustaloader.view_models.MainViewModel.MULTIPLY_DOWNLOAD;

public class App extends Application {
    //todo switch to false on release
    public static final boolean isTestVersion = true;

    public static final String SEARCH_URL = "http://flibustahezeous3.onion/booksearch?ask=";
    private static final String PARSE_WEB_REQUEST_TAG = "parse web request";
    private static final String EXTERNAL_VPN = "external vpn";
    private static final String PREFERENCE_LINEAR_LAYOUT = "linear layout";
    public static int sTorStartTry = 0;
    public static final String BACKUP_DIR_NAME = "FlibustaDownloaderBackup";
    public static final String BACKUP_FILE_NAME = "settings_backup.zip";
    public static final int MAX_BOOK_NUMBER = 548398;
    public static final int VIEW_WEB = 1;
    public static final int VIEW_ODPS = 2;
    private static final String PREFERENCE_CHECK_UPDATES = "check_updates";
    private static final String PREFERENCE_HIDE_READ = "hide read";
    public static final String START_TOR = "start_tor";
    private static final String PREFERENCE_LOAD_ALL = "load all";
    private static final String PREFERENCE_VIEW = "view";
    private static final String PREFERENCE_LAST_CHECKED_BOOK = "last_checked_book";
    private static final String PREFERENCE_FAVORITE_MIME = "favorite format";
    private static final String PREFERENCE_SAVE_ONLY_SELECTED = "save only selected";
    private static final String PREFERENCE_RE_DOWNLOAD = "re download";
    private static final String PREFERENCE_PREVIEWS = "cover_previews_show";

    public static int sSearchType = OPDSActivity.SEARCH_BOOKS;
    // место для хранения текста ответа поиска
    public final MutableLiveData<String> mSearchTitle = new MutableLiveData<>();
    public final MutableLiveData<ArrayList<FoundedSequence>> mSelectedSequences = new MutableLiveData<>();
    // место для хранения выбранной серии
    public final MutableLiveData<FoundedSequence> mSelectedSequence = new MutableLiveData<>();
    // добавление результатов к уже имеющимся
    public boolean mResultsEscalate = false;

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
    public static final String PREFERENCE_DOWNLOAD_LOCATION = "download_location";

    // место для хранения TOR клиента
    public final MutableLiveData<AndroidOnionProxyManager> mTorManager = new MutableLiveData<>();

    // место для хранения выбранного писателя
    public final MutableLiveData<Author> mSelectedAuthor = new MutableLiveData<>();

    // место для хранения выбора писателей
    public final MutableLiveData<ArrayList<Author>> mSelectedAuthors = new MutableLiveData<>();

    private static App instance;
    public boolean mDownloadUnloaded;
    public String mSelectedFormat;
    private Notificator sNotificator;
    public File downloadedApkFile;
    public Uri updateDownloadUri;
    public final MutableLiveData<ArrayList<DownloadLink>> mDownloadLinksList = new MutableLiveData<>();
    public final MutableLiveData<FoundedBook> mSelectedBook = new MutableLiveData<>();
    public final MutableLiveData<FoundedBook> mContextBook = new MutableLiveData<>();
    public final MutableLiveData<AndroidOnionProxyManager> mLoadedTor = new MutableLiveData<>();
    public final MutableLiveData<Author> mAuthorNewBooks = new MutableLiveData<>();
    public final MutableLiveData<String> mMultiplyDownloadStatus = new MutableLiveData<>();
    public final MutableLiveData<String> mLiveDownloadedBookId = new MutableLiveData<>();
    public LiveData<WorkInfo> mDownloadAllWork;
    public boolean mDownloadsInProgress;
    public OneTimeWorkRequest mProcess;
    public final ArrayList<FoundedBook> mBooksDownloadFailed = new ArrayList<>();
    public int mBookSortOption = -1;
    public int mAuthorSortOptions = -1;
    public int mOtherSortOptions = -1;
    public final MutableLiveData<String> mLoadAllStatus = new MutableLiveData<>();
    public final MutableLiveData<ArrayList<FoundedBook>> mSubscribeResults = new MutableLiveData<>();
    public final MutableLiveData<FoundedBook> mShowCover = new MutableLiveData<>();
    public SparseBooleanArray mDownloadSelectedBooks;
    public final MutableLiveData<Boolean> mTypeSelected = new MutableLiveData<>();
    private SharedPreferences mSharedPreferences;
    public AppDatabase mDatabase;
    public final LiveData<WorkInfo> mSearchWork = new LiveData<WorkInfo>() {
    };
    private SubscribeBooks mBooksSubscribe;
    private SubscribeAuthors mAuthorsSubscribe;
    private SubscribeSequences mSequencesSubscribe;
    public InputStream mRequestData;


    @Override
    public void onCreate() {
        super.onCreate();
        // читаю настройки sharedPreferences
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        instance = this;

        sNotificator = Notificator.getInstance();

        startTor();

        // определю ночной режим
        if (getNightMode()) {
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }

        // получаю базу данных
        mDatabase = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "database")
                .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
                .allowMainThreadQueries()
                .build();

        // тут буду отслеживать состояние массовой загрузки и выводить уведомление ожидания подключения
        handleMassDownload();
    }

    private void handleMassDownload() {
        LiveData<List<WorkInfo>> workStatus = WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData(MULTIPLY_DOWNLOAD);
        workStatus.observeForever(workInfos -> {
            if (workInfos != null) {
                if (workInfos.size() > 0) {
                    // получу сведения о состоянии загрузки
                    WorkInfo info = workInfos.get(0);
                    if (info != null) {
                        Log.d("surprise", "App onChanged: status is " + info.getState());
                        switch (info.getState()) {
                            case ENQUEUED:
                                // ожидаем запуска скачивания, покажу уведомление
                                sNotificator.showMassDownloadInQueueMessage();
                                break;
                            case RUNNING:
                                sNotificator.hideMassDownloadInQueueMessage();
                                break;
                            case SUCCEEDED:
                                sNotificator.cancelBookLoadNotification();
                                break;
                            default:
                        }
                    }
                }
            }
        });
    }

    public boolean checkDownloadQueue() {
        // получу все книги в очереди скачивания
        BooksDownloadScheduleDao dao = mDatabase.booksDownloadScheduleDao();
        BooksDownloadSchedule queuedBook = dao.getFirstQueuedBook();
        return queuedBook != null;
    }

    public Notificator getNotificator() {
        if (sNotificator == null) {
            sNotificator = Notificator.getInstance();
        }
        return sNotificator;
    }

    public void startTor() {
        // если используется внешний VPN- TOR не нужен
        if(!isExternalVpn()){
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();
            // запускаю tor
            OneTimeWorkRequest startTorWork = new OneTimeWorkRequest.Builder(StartTorWorker.class).addTag(START_TOR).setConstraints(constraints).build();
            WorkManager.getInstance(this).enqueueUniqueWork(START_TOR, ExistingWorkPolicy.REPLACE, startTorWork);
        }
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

    public void setLastLoadedUrl(String url) {
        if (!url.equals("http://flibustahezeous3.onion/favicon.ico") && !url.equals("http://flibustahezeous3.onion/sites/default/files/bluebreeze_favicon.ico"))
            mSharedPreferences.edit().putString(PREFERENCE_LAST_LOADED_URL, url).apply();
    }

    public String getLastLoadedUrl() {
        return mSharedPreferences.getString(PREFERENCE_LAST_LOADED_URL, URLHelper.getBaseUrl());
    }

    public boolean isCheckUpdate() {
        return mSharedPreferences.getBoolean(PREFERENCE_CHECK_UPDATES, true);
    }

    public boolean isHideRead() {
        return mSharedPreferences.getBoolean(PREFERENCE_HIDE_READ, false);
    }

    public void switchHideRead() {
        mSharedPreferences.edit().putBoolean(PREFERENCE_HIDE_READ, !isHideRead()).apply();
    }

    public boolean isDownloadAll() {
        return mSharedPreferences.getBoolean(PREFERENCE_LOAD_ALL, false);
    }

    public void switchDownloadAll() {
        mSharedPreferences.edit().putBoolean(PREFERENCE_LOAD_ALL, !isDownloadAll()).apply();
    }

    public int getView() {
        return mSharedPreferences.getInt(PREFERENCE_VIEW, 0);
    }

    public void setView(int viewType) {
        mSharedPreferences.edit().putInt(PREFERENCE_VIEW, viewType).apply();
    }

    // добавлю хранилище подписок
    public SubscribeBooks getBooksSubscribe() {
        if (mBooksSubscribe == null) {
            mBooksSubscribe = new SubscribeBooks();
        }
        return mBooksSubscribe;
    }

    public SubscribeAuthors getAuthorsSubscribe() {
        if (mAuthorsSubscribe == null) {
            mAuthorsSubscribe = new SubscribeAuthors();
        }
        return mAuthorsSubscribe;
    }

    public SubscribeSequences getSequencesSubscribe() {
        if (mSequencesSubscribe == null) {
            mSequencesSubscribe = new SubscribeSequences();
        }
        return mSequencesSubscribe;
    }

    public String getLastCheckedBookId() {
        return (mSharedPreferences.getString(PREFERENCE_LAST_CHECKED_BOOK, "tag:book:0"));
    }

    public void setLastCheckedBook(String firstCheckedId) {
        mSharedPreferences.edit().putString(PREFERENCE_LAST_CHECKED_BOOK, firstCheckedId).apply();
    }

    public void discardFavoriteType() {
        mSharedPreferences.edit().remove(PREFERENCE_FAVORITE_MIME).apply();
    }


    public void saveFavoriteMime(String longMime) {
        mSharedPreferences.edit().putString(PREFERENCE_FAVORITE_MIME, longMime).apply();
    }

    public String getFavoriteMime() {
        String favoriteFormat = mSharedPreferences.getString(PREFERENCE_FAVORITE_MIME, null);
        if(favoriteFormat == null || favoriteFormat.isEmpty()){
            return null;
        }
        return favoriteFormat;
    }

    public void setSaveOnlySelected(boolean checked) {
        mSharedPreferences.edit().putBoolean(PREFERENCE_SAVE_ONLY_SELECTED, checked).apply();
    }

    public Boolean isSaveOnlySelected() {
        return (mSharedPreferences.getBoolean(PREFERENCE_SAVE_ONLY_SELECTED, false));
    }

    public void setReDownload(boolean checked) {
        mSharedPreferences.edit().putBoolean(PREFERENCE_RE_DOWNLOAD, checked).apply();
    }

    public Boolean isReDownload() {
        return (mSharedPreferences.getBoolean(PREFERENCE_RE_DOWNLOAD, true));
    }

    public boolean isPreviews() {
        return (mSharedPreferences.getBoolean(PREFERENCE_PREVIEWS, true));
    }

    public void switchShowPreviews() {
        mSharedPreferences.edit().putBoolean(PREFERENCE_PREVIEWS, !isPreviews()).apply();
    }

    public void initializeDownload() {
        // отменю предыдущую работу
        // запущу рабочего, который загрузит все книги
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest downloadAllWorker = new OneTimeWorkRequest.Builder(DownloadBooksWorker.class).addTag(MULTIPLY_DOWNLOAD).setConstraints(constraints).build();
        WorkManager.getInstance(App.getInstance()).enqueueUniqueWork(MULTIPLY_DOWNLOAD, ExistingWorkPolicy.REPLACE, downloadAllWorker);
        App.getInstance().mDownloadAllWork = WorkManager.getInstance(App.getInstance()).getWorkInfoByIdLiveData(downloadAllWorker.getId());
    }

    public void handleWebPage(InputStream my) {
        mRequestData = my;
        OneTimeWorkRequest parseDataWorker = new OneTimeWorkRequest.Builder(ParseWebRequestWorker.class).addTag(PARSE_WEB_REQUEST_TAG).build();
        WorkManager.getInstance(App.getInstance()).enqueueUniqueWork(PARSE_WEB_REQUEST_TAG, ExistingWorkPolicy.REPLACE, parseDataWorker);
    }

    public boolean isExternalVpn() {
        return (mSharedPreferences.getBoolean(EXTERNAL_VPN, false));
    }

    public void switchExternalVpnUse() {
        mSharedPreferences.edit().putBoolean(EXTERNAL_VPN, !isExternalVpn()).apply();
    }

    public void switchLayout() {
        mSharedPreferences.edit().putBoolean(PREFERENCE_LINEAR_LAYOUT, !isLinearLayout()).apply();
    }

    public boolean isLinearLayout() {
        return (mSharedPreferences.getBoolean(PREFERENCE_LINEAR_LAYOUT, true));
    }



    public void setDownloadDir(Uri uri) {
        mSharedPreferences.edit().putString(PREFERENCE_DOWNLOAD_LOCATION, uri.toString()).apply();
    }

    public DocumentFile getDownloadDir() {
        // возвращу папку для закачек
        String download_location = mSharedPreferences.getString(PREFERENCE_DOWNLOAD_LOCATION, null);
        if(download_location != null){
            try{
                DocumentFile dl = DocumentFile.fromTreeUri(App.getInstance(), Uri.parse(download_location));
                if(dl != null && dl.isDirectory()){
                    return dl;
                }
            }
            catch (Exception e){
                return null;
            }
        }
        // верну путь к папке загрузок
        return null;
    }
}