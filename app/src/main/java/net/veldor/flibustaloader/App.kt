package net.veldor.flibustaloader

import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.MutableLiveData
import androidx.multidex.MultiDexApplication
import androidx.room.Room
import androidx.work.*
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
import net.veldor.flibustaloader.workers.DownloadBooksWorker
import net.veldor.flibustaloader.workers.PeriodicCheckFlibustaAvailabilityWorker
import net.veldor.flibustaloader.workers.ShareLastReleaseWorker
import net.veldor.flibustaloader.workers.StartTorWorker
import java.io.File
import java.util.concurrent.TimeUnit


class App : MultiDexApplication() {
    val liveBookDownloadInProgress: MutableLiveData<BooksDownloadSchedule> = MutableLiveData()
    val liveBookJustLoaded: MutableLiveData<BooksDownloadSchedule> = MutableLiveData()
    val liveBookJustRemovedFromQueue: MutableLiveData<BooksDownloadSchedule> = MutableLiveData()
    val liveBookJustError: MutableLiveData<BooksDownloadSchedule> = MutableLiveData()

    // хранилище статуса HTTP запроса
    val requestStatus = MutableLiveData<String>()

    var useMirror = false
    var downloadedApkFile: File? = null
    var updateDownloadUri: Uri? = null
    val mLoadedTor = MutableLiveData<AndroidOnionProxyManager>()
    val mLiveDownloadedBookId = MutableLiveData<String>()
    val liveDownloadState = MutableLiveData(DownloadBooksWorker.DOWNLOAD_FINISHED)
    lateinit var mDatabase: AppDatabase

    override fun onCreate() {
        super.onCreate()
        // got instance
        instance = this
        // clear previously loaded images
        FilesHandler.clearCache()
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
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8
            )
            .allowMainThreadQueries()
            .build()

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

    fun startTorInit() {
        runBlocking {
            launch {
                startTor()
                if (isTestVersion) {
                    LogHandler.getInstance()!!.initLog()
                    NotificationHandler.instance.showTestVersionNotification()
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
                val startTorWork = OneTimeWorkRequest.Builder(StartTorWorker::class.java).addTag(
                    START_TOR
                ).setConstraints(constraints).build()
                WorkManager.getInstance(this)
                    .enqueueUniqueWork(START_TOR, ExistingWorkPolicy.REPLACE, startTorWork)
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
        Log.d("surprise", "switchDownloadState: switching download state")
        var workStopped = false
        val workManager = WorkManager.getInstance(this)
        val statuses =
            workManager.getWorkInfosForUniqueWork(OPDSViewModel.MULTIPLY_DOWNLOAD)
        val workInfoList: List<WorkInfo> = statuses.get()
        workInfoList.forEach {
            // если найдены рабочие экземпляры- остановлю их
            if (it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED) {
                Log.d("surprise", "switchDownloadState: found download work, cancelling it")
                val work = workManager.getWorkInfoById(it.id)
                work.cancel(true)
                if (!workStopped) {
                    workStopped = true
                }
            }
        }
        Log.d("surprise", "switchDownloadState: work stop state is $workStopped")
        if (workStopped) {
            WorkManager.getInstance(App.instance)
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
        FilesHandler.clearCache()
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