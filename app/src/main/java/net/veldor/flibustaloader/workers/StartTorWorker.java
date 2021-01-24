package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.http.GlobalWebClient;
import net.veldor.flibustaloader.http.TorStarter;

public class StartTorWorker extends Worker {

    public StartTorWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (App.getInstance().torInitInProgress) {
            Log.d("surprise", "StartTorWorker doWork 33: started yet");
            return Result.success();
        }
        App.getInstance().torInitInProgress = true;
        // попробую стартовать TOR 3 раза
        while (App.sTorStartTry < 4 && !isStopped()) {
            // есть три попытки, если все три неудачны- верну ошибку
            TorStarter starter = new TorStarter();
            Log.d("surprise", "StartTorWorker doWork: start tor, try # " + App.sTorStartTry);
            if (starter.startTor()) {
                GlobalWebClient.mConnectionState.postValue(GlobalWebClient.CONNECTED);
                // success try
                Log.d("surprise", "StartTorWorker doWork 51:tor initiated");
                App.getInstance().torInitInProgress = false;
                // обнулю счётчик попыток
                App.sTorStartTry = 0;
                return Result.success();
            }
            // попытка неудачна, плюсую счётчик попыток
            App.sTorStartTry++;
        }
        // сюда попаду, если что-то помешало запуску
        if (isStopped()) {
            Log.d("surprise", "StartTorWorker doWork: somebody stop this worker");
        }
        App.getInstance().torInitInProgress = false;
        return Result.success();
    }

}
