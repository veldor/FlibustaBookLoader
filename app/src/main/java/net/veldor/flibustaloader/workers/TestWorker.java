package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.http.ExternalVpnVewClient;

import java.io.IOException;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.util.EntityUtils;

public class TestWorker extends Worker {
    public TestWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // тест запроса
        HttpResponse response = ExternalVpnVewClient.rawRequest(App.BASE_URL);
        if(response != null){
            Log.d("surprise", "TestWorker doWork: " + response.getStatusLine().getStatusCode());
            if(response.getStatusLine().getStatusCode() == 200){
                try {
                    String result = EntityUtils.toString(response.getEntity());
                    Log.d("surprise", "TestWorker doWork: result is " + result);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        else{
            Log.d("surprise", "TestWorker doWork: error request");
        }
        return Result.success();
    }
}
