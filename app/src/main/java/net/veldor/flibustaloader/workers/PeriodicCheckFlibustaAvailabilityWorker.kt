package net.veldor.flibustaloader.workers

import android.content.Context
import android.util.Log
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.ecxeptions.ConnectionLostException
import net.veldor.flibustaloader.http.TorStarter
import net.veldor.flibustaloader.http.TorWebClient
import net.veldor.flibustaloader.notificatons.NotificationHandler
import net.veldor.flibustaloader.utils.PreferencesHandler

class PeriodicCheckFlibustaAvailabilityWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        if (PreferencesHandler.instance.isCheckAvailability() && !PreferencesHandler.instance.isExternalVpn) {
            Log.d(
                "surprise",
                "PeriodicCheckFlibustaAvailabilityWorker doWork 32: START CHECK AVAIL"
            )
            // помечу рабочего важным
            // Mark the Worker as important
            setForegroundAsync(createForegroundInfo())
            while (App.instance.torInitInProgress) {
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            App.instance.torInitInProgress = true
            // попробую стартовать TOR
            val starter = TorStarter()
            App.sTorStartTry = 0
            while (App.sTorStartTry < 4) {
                // есть три попытки, если все три неудачны- верну ошибку
                if (starter.startTor()) {
                    App.sTorStartTry = 0
                    break
                } else {
                    App.sTorStartTry++
                }
            }
            App.instance.torInitInProgress = false

            // готово, проверю доступность
            var webClient: TorWebClient? = null
            try {
                webClient = TorWebClient()
            } catch (e: ConnectionLostException) {
                e.printStackTrace()
            }
            if (webClient == null) {
                return Result.success()
            }
            val url = "http://flibustahezeous3.onion"
            val answer = webClient.requestNoMirror(url)
            if (answer != null && answer.isNotEmpty()) {
                NotificationHandler.instance.notifyFlibustaIsBack()
                WorkManager.getInstance(App.instance).cancelAllWorkByTag(ACTION)
                WorkManager.getInstance(App.instance).cancelUniqueWork(ACTION)
            }
        } else {
            WorkManager.getInstance(App.instance).cancelAllWorkByTag(ACTION)
            WorkManager.getInstance(App.instance).cancelUniqueWork(ACTION)
        }
        return Result.success()
    }

    private fun createForegroundInfo(): ForegroundInfo {
        // Build a notification using bytesRead and contentLength
        val notification = NotificationHandler.instance.checkAvailabilityNotification
        return ForegroundInfo(NotificationHandler.CHECK_AVAILABILITY_NOTIFICATION, notification)
    }

    companion object {
        const val ACTION = "periodic check availability"
    }
}