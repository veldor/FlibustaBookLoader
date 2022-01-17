package net.veldor.flibustaloader.workers

import android.content.Context
import android.util.Log
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.http.TorStarter
import net.veldor.flibustaloader.notificatons.NotificationHandler
import net.veldor.flibustaloader.utils.FireStorageHandler
import net.veldor.flibustaloader.utils.PreferencesHandler
import java.lang.Exception

class StartTorWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        // отмечу рабочего важным
        val info = createForegroundInfo()
        setForegroundAsync(info)
        try {
            Log.d("surprise", "StartTorWorker.kt 12 doWork running start tor worker")
            // получу список мостов для TOR из Firestore
            val starter = TorStarter()
            // попробую стартовать TOR 3 раза
            while (TorStarter.torStartTry < 4 && !isStopped) {
                // есть три попытки, если все три неудачны- верну ошибку
                if (starter.startTor()) {
                    TorStarter.liveTorLaunchState.postValue(TorStarter.TOR_LAUNCH_SUCCESS)
                    // обнулю счётчик попыток
                    TorStarter.torStartTry = 0
                    if (App.instance.isCustomBridgesSet) run {
                        if (!PreferencesHandler.instance.isUseCustomBridges()) {
                            FireStorageHandler().saveCustomBridges()
                        }
                    }
                    App.instance.isCustomBridgesSet = false
                    return Result.success()
                }
                Log.d("surprise", "doWork: failed tor start try")
                // попытка неудачна, плюсую счётчик попыток
                TorStarter.torStartTry++
            }
            // сюда попаду, если что-то помешало запуску
            if (isStopped) {
                Log.d("surprise", "StartTorWorker doWork: somebody stop this worker")
            }
            //
            TorStarter.torStartTry = 0
            TorStarter.liveTorLaunchState.postValue(TorStarter.TOR_LAUNCH_FAILED)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("surprise", "StartTorWorker.kt 43 doWork we have problem with TOR start")
            TorStarter.liveTorLaunchState.postValue(TorStarter.TOR_LAUNCH_FAILED)
        }
        return Result.success()
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = NotificationHandler.instance.startTorNotification
        return ForegroundInfo(
            NotificationHandler.START_TOR_WORKER_NOTIFICATION,
            notification
        )
    }
}