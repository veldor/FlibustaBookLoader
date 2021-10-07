package net.veldor.flibustaloader.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import net.veldor.flibustaloader.http.GlobalWebClient
import net.veldor.flibustaloader.http.TorStarter

class StartTorWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        val starter = TorStarter()
        // попробую стартовать TOR 3 раза
        while (TorStarter.torStartTry < 4 && !isStopped) {
            // есть три попытки, если все три неудачны- верну ошибку
            if (starter.startTor()) {
                GlobalWebClient.mConnectionState.postValue(GlobalWebClient.CONNECTED)
                TorStarter.liveTorLaunchState.postValue(TorStarter.TOR_LAUNCH_SUCCESS)
                // обнулю счётчик попыток
                TorStarter.torStartTry = 0
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
        return Result.success()
    }
}