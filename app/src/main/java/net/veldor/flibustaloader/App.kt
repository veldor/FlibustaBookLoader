package net.veldor.flibustaloader

import android.net.Uri
import android.util.Log
import android.util.SparseBooleanArray
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.multidex.MultiDexApplication
import androidx.room.Room
import androidx.work.*
import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.veldor.flibustaloader.database.AppDatabase
import net.veldor.flibustaloader.http.GlobalWebClient
import net.veldor.flibustaloader.notificatons.NotificationHandler
import net.veldor.flibustaloader.selections.Author
import net.veldor.flibustaloader.selections.DownloadLink
import net.veldor.flibustaloader.selections.FoundedBook
import net.veldor.flibustaloader.selections.FoundedSequence
import net.veldor.flibustaloader.ui.OPDSActivity
import net.veldor.flibustaloader.utils.*
import net.veldor.flibustaloader.view_models.MainViewModel
import net.veldor.flibustaloader.workers.*
import java.io.File
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit

class App : MultiDexApplication() {
    // хранилище статуса HTTP запроса
    val requestStatus = MutableLiveData<String>()

    // место для хранения текста ответа поиска
    val mSearchTitle = MutableLiveData<String>()
    val mSelectedSequences = MutableLiveData<ArrayList<FoundedSequence>>()

    // место для хранения выбранной серии
    val mSelectedSequence = MutableLiveData<FoundedSequence>()

    // добавление результатов к уже имеющимся
    var mResultsEscalate = false

    // место для хранения выбранного писателя
    val mSelectedAuthor = MutableLiveData<Author>()

    // место для хранения выбора писателей
    val mSelectedAuthors = MutableLiveData<ArrayList<Author>>()
    var mDownloadUnloaded = false
    var mSelectedFormat: String? = null
    var useMirror = false
    private var sNotificator: NotificationHandler? = null
    var downloadedApkFile: File? = null
    var updateDownloadUri: Uri? = null
    val mDownloadLinksList = MutableLiveData<ArrayList<DownloadLink>>()
    val mSelectedBook = MutableLiveData<FoundedBook>()
    val mContextBook = MutableLiveData<FoundedBook>()
    val mLoadedTor = MutableLiveData<AndroidOnionProxyManager>()
    val mAuthorNewBooks = MutableLiveData<Author>()
    val mMultiplyDownloadStatus = MutableLiveData<String>()
    val mLiveDownloadedBookId = MutableLiveData<String>()
    var mDownloadAllWork: LiveData<WorkInfo>? = null
    var mDownloadsInProgress = false
    var mProcess: OneTimeWorkRequest? = null
    val mBooksDownloadFailed = ArrayList<FoundedBook>()
    var mBookSortOption = -1
    var mAuthorSortOptions = -1
    var mOtherSortOptions = -1
    val mLoadAllStatus = MutableLiveData<String>()
    val mSubscribeResults = MutableLiveData<ArrayList<FoundedBook>>()
    val mShowCover = MutableLiveData<FoundedBook>()
    var mDownloadSelectedBooks: SparseBooleanArray? = null
    val mTypeSelected = MutableLiveData<Boolean>()
    lateinit var mDatabase: AppDatabase
    lateinit var mSearchWork: LiveData<WorkInfo>
    private var mBooksSubscribe: SubscribeBooks? = null
    private var mBooksBlacklist: BlacklistBooks? = null
    private var mAuthorsSubscribe: SubscribeAuthors? = null
    private var mSequencesSubscribe: SubscribeSequences? = null
    var mRequestData: InputStream? = null
    private var mAuthorsBlacklist: BlacklistAuthors? = null
    private var mSequencesBlacklist: BlacklistSequences? = null
    private var mGenresBlacklist: BlacklistGenres? = null
    var torInitInProgress = false


    override fun onCreate() {
        super.onCreate()
        // got instance
        instance = this

        setupApp()

        // определю ночной режим
        if (PreferencesHandler.instance.nightMode) {
            AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_YES
            )
        } else {
            AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            )
        }
        // тут буду отслеживать состояние массовой загрузки и выводить уведомление ожидания подключения
        handleMassDownload()
    }

    private fun setupApp() {
            runBlocking {
                launch {
                    startTor()
                    if (isTestVersion) {
                        LogHandler.getInstance()!!.initLog()
                        NotificationHandler.instance.showTestVersionNotification()
                    }
                }
            }

        // получаю базу данных
        mDatabase = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database"
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7
            )
            .allowMainThreadQueries()
            .build()
    }

    private fun handleMassDownload() {
        val workStatus = WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData(
            MainViewModel.MULTIPLY_DOWNLOAD
        )
        workStatus.observeForever { workInfos: List<WorkInfo?>? ->
            if (workInfos != null) {
                if (workInfos.isNotEmpty()) {
                    // получу сведения о состоянии загрузки
                    val info = workInfos[0]
                    if (info != null) {
                        when (info.state) {
                            WorkInfo.State.ENQUEUED ->                                 // ожидаем запуска скачивания, покажу уведомление
                                sNotificator!!.showMassDownloadInQueueMessage()
                            WorkInfo.State.RUNNING -> sNotificator!!.hideMassDownloadInQueueMessage()
                            WorkInfo.State.SUCCEEDED -> sNotificator!!.cancelBookLoadNotification()
                            else -> {
                            }
                        }
                    }
                }
            }
        }
    }

    fun checkDownloadQueue(): Boolean {
        // получу все книги в очереди скачивания
        val dao = mDatabase.booksDownloadScheduleDao()
        val queuedBook = dao.firstQueuedBook
        return queuedBook != null
    }

    fun startTor() {
        // если используется внешний VPN- TOR не нужен
        if (!PreferencesHandler.instance.isExternalVpn) {
            // если рабочий ещё не запущен- запущу. Если уже работает- проигнорирую
            if (!torInitInProgress) {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                // запускаю tor
                val startTorWork = OneTimeWorkRequest.Builder(StartTorWorker::class.java).addTag(
                    START_TOR
                ).setConstraints(constraints).build()
                WorkManager.getInstance(this)
                    .enqueueUniqueWork(START_TOR, ExistingWorkPolicy.REPLACE, startTorWork)
            }
        }
        else {
            // по умолчанию считаю, что соединение успешно
            GlobalWebClient.mConnectionState.postValue(GlobalWebClient.CONNECTED)
        }
    }

    // добавлю хранилище подписок
    val booksSubscribe: SubscribeBooks
        get() {
            if (mBooksSubscribe == null) {
                mBooksSubscribe = SubscribeBooks()
            }
            return mBooksSubscribe!!
        }

    // добавлю хранилище чёрного списка
    val booksBlacklist: BlacklistBooks
        get() {
            if (mBooksBlacklist == null) {
                mBooksBlacklist = BlacklistBooks()
            }
            return mBooksBlacklist!!
        }
    val authorsSubscribe: SubscribeAuthors
        get() {
            if (mAuthorsSubscribe == null) {
                mAuthorsSubscribe = SubscribeAuthors()
            }
            return mAuthorsSubscribe!!
        }
    val authorsBlacklist: BlacklistAuthors
        get() {
            if (mAuthorsBlacklist == null) {
                mAuthorsBlacklist = BlacklistAuthors()
            }
            return mAuthorsBlacklist!!
        }
    val sequencesBlacklist: BlacklistSequences
        get() {
            if (mSequencesBlacklist == null) {
                mSequencesBlacklist = BlacklistSequences()
            }
            return mSequencesBlacklist!!
        }
    val genresBlacklist: BlacklistGenres
        get() {
            if (mGenresBlacklist == null) {
                mGenresBlacklist = BlacklistGenres()
            }
            return mGenresBlacklist!!
        }
    val sequencesSubscribe: SubscribeSequences
        get() {
            if (mSequencesSubscribe == null) {
                mSequencesSubscribe = SubscribeSequences()
            }
            return mSequencesSubscribe!!
        }

    fun initializeDownload() {
        if (!DownloadBooksWorker.downloadInProgress) {
            // отменю предыдущую работу
            // запущу рабочего, который загрузит все книги
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val downloadAllWorker = OneTimeWorkRequest.Builder(
                DownloadBooksWorker::class.java
            ).addTag(MainViewModel.MULTIPLY_DOWNLOAD).setConstraints(constraints).build()
            WorkManager.getInstance(instance).enqueueUniqueWork(
                MainViewModel.MULTIPLY_DOWNLOAD,
                ExistingWorkPolicy.KEEP,
                downloadAllWorker
            )
            instance.mDownloadAllWork =
                WorkManager.getInstance(instance).getWorkInfoByIdLiveData(downloadAllWorker.id)
        }
    }

    fun handleWebPage(my: InputStream?) {
        mRequestData = my
        val parseDataWorker = OneTimeWorkRequest.Builder(
            ParseWebRequestWorker::class.java
        ).addTag(PARSE_WEB_REQUEST_TAG).build()
        WorkManager.getInstance(instance)
            .enqueueUniqueWork(PARSE_WEB_REQUEST_TAG, ExistingWorkPolicy.REPLACE, parseDataWorker)
    }


    fun startCheckWorker() {
        // запущу периодическую проверку доступности флибы
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        // запущу рабочего, который периодически будет обновлять данные
        val periodicTask = PeriodicWorkRequest.Builder(
            PeriodicCheckFlibustaAvailabilityWorker::class.java, 15, TimeUnit.MINUTES
        ).setConstraints(constraints).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PeriodicCheckFlibustaAvailabilityWorker.ACTION,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicTask
        )
        Log.d("surprise", "App startCheckWorker 497: CHECKER PLANNED")
    }

    fun shareLatestRelease() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        // запущу рабочего, который периодически будет обновлять данные
        val task = OneTimeWorkRequest.Builder(ShareLastReleaseWorker::class.java)
            .setConstraints(constraints).build()
        WorkManager.getInstance(this).enqueue(task)
    }

    companion object {
        //todo switch to false on release
        const val isTestVersion = true
        val sResetLoginCookie = MutableLiveData<Boolean>()
        const val SEARCH_URL = "http://flibustahezeous3.onion/booksearch?ask="
        const val PIC_MIRROR_URL = "http://flibusta.is"
        private const val PARSE_WEB_REQUEST_TAG = "parse web request"
        var sTorStartTry = 0
        const val BACKUP_DIR_NAME = "FlibustaDownloaderBackup"
        const val BACKUP_FILE_NAME = "settings_backup.zip"
        const val MAX_BOOK_NUMBER = 548398
        const val VIEW_WEB = 1
        const val VIEW_ODPS = 2
        const val START_TOR = "start_tor"
        var sSearchType = OPDSActivity.SEARCH_BOOKS
        const val NEW_BOOKS = "http://flibustahezeous3.onion/new"
        const val VIEW_MODE_NORMAL = 1
        const val VIEW_MODE_LIGHT = 2
        const val VIEW_MODE_FAT = 3
        const val VIEW_MODE_FAST = 4
        const val VIEW_MODE_FAST_FAT = 5
        const val BASE_URL = "http://flibustahezeous3.onion"
        const val BASE_BOOK_URL = "http://flibustahezeous3.onion/b/"
        const val TOR_FILES_LOCATION = "torfiles"
        lateinit var instance: App
            private set
    }
}