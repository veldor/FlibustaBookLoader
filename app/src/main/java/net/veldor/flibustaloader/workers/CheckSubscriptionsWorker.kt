package net.veldor.flibustaloader.workers

import android.content.Context
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import net.veldor.flibustaloader.notificatons.NotificationHandler
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.utils.SubscribesHandler
import net.veldor.flibustaloader.view_models.SubscriptionsViewModel

class CheckSubscriptionsWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        val info = createForegroundInfo()
        setForegroundAsync(info)
        if(!SubscriptionsViewModel.liveCheckInProgress.value!!) {
            SubscriptionsViewModel.liveCheckInProgress.postValue(true)
            val lastCheckedBookId = SubscribesHandler.checkSubscribes(null, true)
            if (lastCheckedBookId != null) {
                PreferencesHandler.instance.lastCheckedBookId = lastCheckedBookId
            }
            SubscriptionsViewModel.liveCheckInProgress.postValue(false)
            val results = SubscriptionsViewModel.foundedSubscribes.value
            if(results != null && results.size > 0){
                NotificationHandler.instance.sendFoundSubscribesNotification()
            }
        }
        return Result.success()
    }


    private fun createForegroundInfo(): ForegroundInfo {
        val notification = NotificationHandler.instance.checkSubscribesNotification
        return ForegroundInfo(
            NotificationHandler.CHECK_SUBSCRIBES_WORKER_NOTIFICATION,
            notification
        )
    }

    companion object {
        const val PERIODIC_CHECK_TAG = "periodic check subscriptions"
    }
}