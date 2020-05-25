package net.veldor.flibustaloader.workers;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.BuildConfig;
import net.veldor.flibustaloader.updater.Updater;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import androidx.work.Worker;
import androidx.work.WorkerParameters;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.util.EntityUtils;

public class CheckUpdateWorker extends Worker {

    public CheckUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        if(App.isTestVersion){
            return Result.success();
        }
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet(Updater.GITHUB_RELEASES_URL);
        try {
        // кастомный обработчик ответов
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

            @Override
            public String handleResponse(HttpResponse response){
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    try {
                        String body = EntityUtils.toString(entity);
                        JSONObject releaseInfo = new JSONObject(body);
                        String lastVersion = releaseInfo.getString(Updater.GITHUB_APP_VERSION);
                        String currentVersion = BuildConfig.VERSION_NAME;
                        if(!lastVersion.equals(currentVersion)){
                            // версии отличаются
                            Updater.newVersion.postValue(true);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    // неверный ответ с сервера
                    Log.d("surprise", "CheckUpdateWorker handleResponse: wrong update server answer");
                }
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
                // отправлю оповещение об отсутствии новой версии
                Updater.newVersion.postValue(false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return Result.success();
    }


}
