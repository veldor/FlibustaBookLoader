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
import net.veldor.flibustaloader.utils.URLHelper

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
            // готово, проверю доступность

            val webClient = TorWebClient()
            val url = URLHelper.getFlibustaUrl()
            val answer = webClient.directRequest(url)
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