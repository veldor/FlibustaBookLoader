package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import net.veldor.flibustaloader.updater.Updater;

import java.io.IOException;

import androidx.work.Worker;
import androidx.work.WorkerParameters;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;

public class CheckUpdateWorker extends Worker {

    public CheckUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet(Updater.GITHUB_RELEASES_URL);
        try {
        // кастомный обработчик ответов
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

            @Override
            public String handleResponse(HttpResponse response){
                int status = response.getStatusLine().getStatusCode();
                Log.d("surprise", "CheckUpdateWorker handleResponse: checkUpdate status " + status);
                return null;
            }
        };
        // выполню запрос
            httpclient.execute(httpget, responseHandler);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                // по-любому закрою клиент
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return Result.success();
    }


}
