package net.veldor.flibustaloader.receivers

import android.annotation.SuppressLint
import net.veldor.flibustaloader.workers.DownloadBooksWorker.Companion.dropDownloadsQueue
import net.veldor.flibustaloader.workers.DownloadBooksWorker.Companion.skipFirstBook
import net.veldor.flibustaloader.utils.LogHandler.Companion.getInstance
import android.content.Intent
import net.veldor.flibustaloader.notificatons.NotificationHandler
import android.widget.Toast
import net.veldor.flibustaloader.App
import androidx.work.WorkManager
import net.veldor.flibustaloader.view_models.OPDSViewModel
import net.veldor.flibustaloader.ui.MainActivity
import android.app.PendingIntent
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.util.Log
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.workers.DownloadBooksWorker
import kotlin.system.exitProcess

class MiscActionsReceiver : BroadcastReceiver() {
    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onReceive(context: Context, intent: Intent) {
        intent.action
        val action = intent.getStringExtra(EXTRA_ACTION_TYPE)
        if (action != null) {
            when (action) {
                ACTION_CANCEL_MASS_DOWNLOAD -> {
                    NotificationHandler.instance.hideMassDownloadInQueueMessage()
                    dropDownloadsQueue()
                    WorkManager.getInstance(App.instance)
                        .cancelAllWorkByTag(OPDSViewModel.MULTIPLY_DOWNLOAD)
                    // отменяю работу и очищу очередь скачивания
                    NotificationHandler.instance.cancelBookLoadNotification()
                    App.instance.liveDownloadState.postValue(DownloadBooksWorker.DOWNLOAD_FINISHED)
                    Toast.makeText(
                        App.instance,
                        "Скачивание книг отменено и очередь скачивания очищена!",
                        Toast.LENGTH_LONG
                    ).show()
                }
                ACTION_PAUSE_MASS_DOWNLOAD -> {
                    NotificationHandler.instance.hideMassDownloadInQueueMessage()
                    WorkManager.getInstance(App.instance)
                        .cancelAllWorkByTag(OPDSViewModel.MULTIPLY_DOWNLOAD)
                    Toast.makeText(
                        App.instance,
                        "Скачивание книг приостановлено!",
                        Toast.LENGTH_LONG
                    ).show()
                    // покажу уведомление о приостановленной загрузке
                    NotificationHandler.instance.createMassDownloadPausedNotification()
                    App.instance.liveDownloadState.postValue(DownloadBooksWorker.DOWNLOAD_FINISHED)
                }
                ACTION_SKIP_BOOK -> {
                    // пропущу первую книгу в очереди и продолжу скачивание
                    skipFirstBook()
                    NotificationHandler.instance.mNotificationManager.cancel(NotificationHandler.DOWNLOAD_PAUSED_NOTIFICATION)
                }
                ACTION_RESUME_MASS_DOWNLOAD -> {
                    Log.d("surprise", "MiscActionsReceiver onReceive: resume mass download")
                    NotificationHandler.instance.mNotificationManager.cancel(NotificationHandler.DOWNLOAD_PAUSED_NOTIFICATION)
                    App.instance.requestDownloadBooksStart()
                }
                ACTION_REPEAT_DOWNLOAD -> Log.d("surprise", "onReceive: start")
                ACTION_RESTART_TOR -> {
                    NotificationHandler.instance.hideMassDownloadInQueueMessage()
                    App.instance.startTor()
                }
                ACTION_SEND_LOGS -> {
                    NotificationHandler.instance.mNotificationManager.cancel(NotificationHandler.IS_TEST_VERSION_NOTIFICATION)
                    val it = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
                    context.sendBroadcast(it)
                    getInstance()!!.sendLogs()
                }
                ACTION_ENABLE_VPN_MODE -> {
                    PreferencesHandler.instance.isExternalVpn = !PreferencesHandler.instance.isExternalVpn
                    val mStartActivity = Intent(context, MainActivity::class.java)
                    val mPendingIntentId = 123456
                    val mPendingIntent = PendingIntent.getActivity(
                        context,
                        mPendingIntentId,
                        mStartActivity,
                        PendingIntent.FLAG_CANCEL_CURRENT
                    )
                    val mgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    mgr[AlarmManager.RTC, System.currentTimeMillis() + 100] = mPendingIntent
                    exitProcess(0)
                }
            }
        }
    }

    companion object {
        const val EXTRA_ACTION_TYPE = "action type"
        const val ACTION_CANCEL_MASS_DOWNLOAD = "cancel download"
        const val ACTION_PAUSE_MASS_DOWNLOAD = "pause download"
        const val ACTION_RESUME_MASS_DOWNLOAD = "resume download"
        const val ACTION_REPEAT_DOWNLOAD = "repeat download"
        const val ACTION_SKIP_BOOK = "skip book"
        const val ACTION_RESTART_TOR = "restart tor"
        const val ACTION_SEND_LOGS = "send logs"
        const val ACTION_ENABLE_VPN_MODE = "enable vpn"
    }
}