package net.veldor.flibustaloader.workers

import android.content.Context
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.http.UniversalWebClient
import net.veldor.flibustaloader.notificatons.NotificationHandler

class PeriodicCheckFlibustaAvailabilityWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        setForegroundAsync(createForegroundInfo())
        // готово, проверю доступность

        val response = UniversalWebClient().rawRequest("/opds")
        if (response.statusCode == 200) {
            val answer = UniversalWebClient().responseToString(response.inputStream)
            if (answer.isNullOrEmpty() || !answer.startsWith("<?xml version=\"1.0\" encoding=\"utf-8\"?>")) {
                NotificationHandler.instance.notifyFlibustaIsBack()
                WorkManager.getInstance(App.instance).cancelAllWorkByTag(ACTION)
                WorkManager.getInstance(App.instance).cancelUniqueWork(ACTION)
            }
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