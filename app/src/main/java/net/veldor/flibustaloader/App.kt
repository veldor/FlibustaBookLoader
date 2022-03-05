package net.veldor.flibustaloader

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.MutableLiveData
import androidx.multidex.MultiDexApplication
import androidx.room.Room
import androidx.work.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.veldor.flibustaloader.database.AppDatabase
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule
import net.veldor.flibustaloader.delegates.DownloadWorkSwitchStateDelegate
import net.veldor.flibustaloader.http.TorStarter
import net.veldor.flibustaloader.notificatons.NotificationHandler
import net.veldor.flibustaloader.utils.FilesHandler
import net.veldor.flibustaloader.utils.LogHandler
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.view_models.DownloadScheduleViewModel
import net.veldor.flibustaloader.view_models.OPDSViewModel
import java.io.File
import java.util.concurrent.TimeUnit
import com.google.gson.Gson
import net.veldor.flibustaloader.workers.*


class App : MultiDexApplication() {
    var isCustomBridgesSet: Boolean = false
    var migrationError: Boolean = false
    val liveBookDownloadInProgress: MutableLiveData<BooksDownloadSchedule> = MutableLiveData()
    val liveBookJustLoaded: MutableLiveData<BooksDownloadSchedule> = MutableLiveData()
    val liveBookJustRemovedFromQueue: MutableLiveData<BooksDownloadSchedule> = MutableLiveData()
    val liveBookJustError: MutableLiveData<BooksDownloadSchedule> = MutableLiveData()
    val liveLowMemory: MutableLiveData<Boolean> = MutableLiveData()

    // хранилище статуса HTTP запроса
    val requestStatus = MutableLiveData<String>()
    var useMirror = false
    var downloadedApkFile: File? = null
    var updateDownloadUri: Uri? = null
    val mLoadedTor = MutableLiveData<AndroidOnionProxyManager>()
    val mLiveDownloadedBookId = MutableLiveData<String>()
    val liveDownloadState = MutableLiveData(DownloadBooksWorker.DOWNLOAD_FINISHED)
    lateinit var mDatabase: AppDatabase
    var torException: java.lang.Exception? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("surprise", "App.kt 51 onCreate test is $isTestVersion")
        // got instance
        instance = this
        // clear previously loaded images
        FilesHandler.clearCache(cacheDir)
        // получаю базу данных
        try {
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
                    AppDatabase.MIGRATION_6_7,
                    AppDatabase.MIGRATION_7_8
                )
                .allowMainThreadQueries()
                .build()
            // попробую найти ошибку при миграции. Если она есть- не стану грузить приложение и попрошу сбросить настройки
            mDatabase.booksDownloadScheduleDao().queueSize
        } catch (e: Exception) {
            // ошибка при миграции, судя по всему
            migrationError = true
        }
        setNightMode()
        // тут буду отслеживать состояние массовой загрузки и выводить уведомление ожидания подключения
        handleMassDownload()
        startTorInit()
    }

    public fun setNightMode() {

        // определю ночной режим
        if (PreferencesHandler.instance.isEInk) {
            AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_NO
            )
        } else {
            if (PreferencesHandler.instance.nightMode) {
                Log.d("surprise", "App.kt 88 onCreate set night mode on")
                AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES
                )
            } else {
                Log.d("surprise", "App.kt 93 onCreate set night mode system")
                AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                )
            }
        }
    }


    fun startTorInit() {
        runBlocking {
            launch {
                startTor()
                if (isTestVersion) {
                    LogHandler.getInstance()!!.initLog()
                    NotificationHandler.instance.showTestVersionNotification()
                    Log.d("surprise", "App.kt 108 startTorInit test notification showed")
                }
            }
        }
    }

    private fun handleMassDownload() {
        val workStatus = WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData(
            OPDSViewModel.MULTIPLY_DOWNLOAD
        )
        workStatus.observeForever { workInfos: List<WorkInfo?>? ->
            if (workInfos != null) {
                if (workInfos.isNotEmpty()) {
                    // получу сведения о состоянии загрузки
                    val info = workInfos[0]
                    if (info != null) {
                        when (info.state) {
                            WorkInfo.State.ENQUEUED ->                                 // ожидаем запуска скачивания, покажу уведомление
                                NotificationHandler.instance.showMassDownloadInQueueMessage()
                            WorkInfo.State.RUNNING -> NotificationHandler.instance.hideMassDownloadInQueueMessage()
                            WorkInfo.State.SUCCEEDED -> NotificationHandler.instance.cancelBookLoadNotification()
                            else -> {
                            }
                        }
                    }
                }
            }
        }
    }

    fun startTor() {
        // если используется внешний VPN- TOR не нужен
        if (!PreferencesHandler.instance.isExternalVpn) {
            // если рабочий ещё не запущен- запущу. Если уже работает- проигнорирую
            if (TorStarter.liveTorLaunchState.value != TorStarter.TOR_LAUNCH_IN_PROGRESS) {
                TorStarter.liveTorLaunchState.postValue(TorStarter.TOR_LAUNCH_IN_PROGRESS)
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                // запускаю tor
                // getting bridges
                val startTorWork = OneTimeWorkRequest.Builder(StartTorWorker::class.java).addTag(
                    START_TOR
                ).setConstraints(constraints).build()
                // if use custom bridges- save it
                if (PreferencesHandler.instance.isUseCustomBridges()) {
                    FilesHandler.saveBridges(PreferencesHandler.instance.getCustomBridges())
                    WorkManager.getInstance(this)
                        .enqueueUniqueWork(
                            START_TOR,
                            ExistingWorkPolicy.REPLACE,
                            startTorWork
                        )
                } else {
                    val db = Firebase.firestore
                    db.collection("bridges")
                        .get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                Log.d("surprise", "${document.id} => ${document.data}")
                                // save document data
                                FilesHandler.saveBridges(document.data)
                            }
                            WorkManager.getInstance(this)
                                .enqueueUniqueWork(
                                    START_TOR,
                                    ExistingWorkPolicy.REPLACE,
                                    startTorWork
                                )
                        }
                        .addOnFailureListener { exception ->
                            Log.w("surprise", "Error getting documents.", exception)
                            WorkManager.getInstance(this)
                                .enqueueUniqueWork(
                                    START_TOR,
                                    ExistingWorkPolicy.REPLACE,
                                    startTorWork
                                )
                        }
                }
            }
        }
    }

    private fun startDownloadBooksWorker() {
        // запущу рабочего, который загрузит все книги
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val downloadAllWorker = OneTimeWorkRequest.Builder(
            DownloadBooksWorker::class.java
        ).addTag(OPDSViewModel.MULTIPLY_DOWNLOAD).setConstraints(constraints).build()
        WorkManager.getInstance(instance).enqueueUniqueWork(
            OPDSViewModel.MULTIPLY_DOWNLOAD,
            ExistingWorkPolicy.KEEP,
            downloadAllWorker
        )
    }


    fun startCheckWorker() {
        // запущу периодическую проверку доступности флибы
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        // запущу рабочего, который периодически будет обновлять данные
        val periodicTask = PeriodicWorkRequest.Builder(
            PeriodicCheckFlibustaAvailabilityWorker::class.java, 30, TimeUnit.MINUTES
        ).setConstraints(constraints).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PeriodicCheckFlibustaAvailabilityWorker.ACTION,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicTask
        )
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

    fun requestDownloadBooksStart() {
        if (PreferencesHandler.instance.isDownloadAutostart) {
            var workInProgress = false
            val workManager = WorkManager.getInstance(this)
            val statuses =
                workManager.getWorkInfosForUniqueWork(OPDSViewModel.MULTIPLY_DOWNLOAD)
            val workInfoList: List<WorkInfo> = statuses.get()
            workInfoList.forEach {
                if (it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED) {
                    workInProgress = true
                }
            }
            if (!workInProgress) {
                startDownloadBooksWorker()
                DownloadScheduleViewModel.downloadState.postValue(true)
            }
        }
    }

    fun switchDownloadState(delegate: DownloadWorkSwitchStateDelegate) {
        var workStopped = false
        val workManager = WorkManager.getInstance(this)
        val statuses =
            workManager.getWorkInfosForUniqueWork(OPDSViewModel.MULTIPLY_DOWNLOAD)
        val workInfoList: List<WorkInfo> = statuses.get()
        workInfoList.forEach {
            // если найдены рабочие экземпляры- остановлю их
            if (it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED) {
                val work = workManager.getWorkInfoById(it.id)
                work.cancel(true)
                if (!workStopped) {
                    workStopped = true
                }
            }
        }
        if (workStopped) {
            WorkManager.getInstance(instance)
                .cancelAllWorkByTag(OPDSViewModel.MULTIPLY_DOWNLOAD)
            // если работы приостановлены- удалю сообщение о загрузке
            // оповещу о смене статуса
            DownloadScheduleViewModel.downloadState.postValue(false)
            delegate.stateSwitched(1)
        } else {
            // запущу загрузку
            startDownloadBooksWorker()
            DownloadScheduleViewModel.downloadState.postValue(true)
            delegate.stateSwitched(2)

        }
    }

    override fun onTerminate() {
        super.onTerminate()
        // clear previously loaded images
        FilesHandler.clearCache(cacheDir)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Toast.makeText(this, getString(R.string.no_more_ram_message), Toast.LENGTH_LONG).show()
        liveLowMemory.postValue(true)
    }

    fun downloadBook(scheduleItem: BooksDownloadSchedule) {
        val gson = Gson()
        val j = gson.toJson(scheduleItem)

        // запущу рабочего, который загрузит все книги
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val data = Data.Builder()
        data.putString("item", j)
        val downloadWorker = OneTimeWorkRequest.Builder(
            DownloadBookWorker::class.java
        ).setInputData(data.build())
            .setConstraints(constraints).build()
        WorkManager.getInstance(instance).enqueue(
            downloadWorker
        )
    }

    fun stopTorInit() {
        WorkManager.getInstance(instance).cancelAllWorkByTag(START_TOR)
    }

    companion object {
        //todo switch to false on release
        const val isTestVersion = true
        val sResetLoginCookie = MutableLiveData<Boolean>()
        const val BACKUP_DIR_NAME = "FlibustaDownloaderBackup"
        const val BACKUP_FILE_NAME = "settings_backup.zip"
        const val START_TOR = "start_tor"
        lateinit var instance: App
            private set
    }
}