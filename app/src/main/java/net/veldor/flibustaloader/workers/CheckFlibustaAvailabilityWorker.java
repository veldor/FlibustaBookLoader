package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.ecxeptions.TorNotLoadedException;
import net.veldor.flibustaloader.http.GlobalWebClient;

import java.io.IOException;

public class CheckFlibustaAvailabilityWorker extends Worker {


    public static final String ACTION = "check availability";

    public CheckFlibustaAvailabilityWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("surprise", "CheckFlibustaAvailabilityWorker doWork 27: CHECK AVAILABILITY");
        // check availability
        try {
            String answer = GlobalWebClient.request(App.BASE_URL);
            if (answer != null) {
                if (answer.length() == 0) {
                    return Result.failure();
                }
                answer = GlobalWebClient.request(App.BASE_URL + "/opds/");
                if(answer != null){
                    if (answer.length() == 0) {
                        return Result.failure();
                    }
                    return Result.success();
                }
            }
        } catch (TorNotLoadedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Result.failure();
    }
}
