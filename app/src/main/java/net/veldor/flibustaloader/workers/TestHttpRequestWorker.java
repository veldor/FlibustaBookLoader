package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;

import java.io.IOException;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.util.EntityUtils;

public class TestHttpRequestWorker extends Worker {
    public TestHttpRequestWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        CloseableHttpClient httpclient = null;
        try {
            httpclient = HttpClients.createDefault();
            HttpGet httpget = new HttpGet("http://flibusta.is/opds");
            // кастомный обработчик ответов
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
                @Override
                public String handleResponse(HttpResponse response) {
                    int status = response.getStatusLine().getStatusCode();
                    Log.d("surprise", "TestHttpRequestWorker handleResponse status is " + status);
                    HttpEntity entity = response.getEntity();
                    if(entity != null){
                        try {
                            String answer = EntityUtils.toString(entity);
                            Log.d("surprise", "TestHttpRequestWorker handleResponse answer is " + answer);
                        } catch (IOException e) {
                            Log.d("surprise", "TestHttpRequestWorker handleResponse can't get content");
                        }
                    }
                    return null;
                }
            };
            // выполню запрос
            httpclient.execute(httpget, responseHandler);
        } catch (IOException e) {
            Log.d("surprise", "TestHttpRequestWorker doWork have error in request: " + e.getMessage());
        } finally {
            try {
                // по-любому закрою клиент
                if (httpclient != null) {
                    httpclient.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Result.success();
    }
}
