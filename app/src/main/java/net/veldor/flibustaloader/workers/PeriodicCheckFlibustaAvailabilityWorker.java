package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.ecxeptions.TorNotLoadedException;
import net.veldor.flibustaloader.http.TorWebClient;

public class PeriodicCheckFlibustaAvailabilityWorker extends Worker {


    public static final String ACTION = "periodic check availability";

    public PeriodicCheckFlibustaAvailabilityWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("surprise", "PeriodicCheckFlibustaAvailabilityWorker doWork 27: CHECK AVAILABILITY STARTED");
        if (!isStopped()) {
            // check availability
            try {
                TorWebClient webClient = new TorWebClient();
                String answer = webClient.request(App.BASE_URL);
                if (answer.length() > 0) {
                    answer = webClient.request(App.BASE_URL + "/opds/");
                    if (answer.length() > 0) {
                        // флибуста снова с нами!
                        App.getInstance().getNotificator().notifyFlibustaIsBack();
                        WorkManager.getInstance(App.getInstance()).cancelAllWorkByTag(ACTION);
                        WorkManager.getInstance(App.getInstance()).cancelUniqueWork(ACTION);
                    }
                }
            } catch (TorNotLoadedException e) {
                Log.d("surprise", "PeriodicCheckFlibustaAvailabilityWorker doWork 42: can't check availability");
                e.printStackTrace();
            }
        }
        else{
            Log.d("surprise", "PeriodicCheckFlibustaAvailabilityWorker doWork 47: i m stopped");
        }
        Log.d("surprise", "PeriodicCheckFlibustaAvailabilityWorker doWork 47: availability checked");
        return Result.success();
    }
}
