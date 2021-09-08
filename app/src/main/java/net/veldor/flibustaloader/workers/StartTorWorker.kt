package net.veldor.flibustaloader.workers

import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.http.GlobalWebClient
import net.veldor.flibustaloader.http.TorStarter
import android.content.Context
import android.util.Log
import androidx.work.*
import net.veldor.flibustaloader.http.TorWebClient

class StartTorWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        if (App.instance.torInitInProgress) {
            return Result.success()
        }
        App.instance.torInitInProgress = true
        // попробую стартовать TOR 3 раза
        while (App.sTorStartTry < 4 && !isStopped) {
            // есть три попытки, если все три неудачны- верну ошибку
            val starter = TorStarter()
            if (starter.startTor()) {
                GlobalWebClient.mConnectionState.postValue(GlobalWebClient.CONNECTED)
                // success try
                App.instance.torInitInProgress = false
                // обнулю счётчик попыток
                App.sTorStartTry = 0
                return Result.success()
            }
            // попытка неудачна, плюсую счётчик попыток
            App.sTorStartTry++
        }
        // сюда попаду, если что-то помешало запуску
        if (isStopped) {
            Log.d("surprise", "StartTorWorker doWork: somebody stop this worker")
        }
        App.instance.torInitInProgress = false
        return Result.success()
    }
}