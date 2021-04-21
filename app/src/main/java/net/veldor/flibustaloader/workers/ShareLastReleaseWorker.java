package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.updater.Updater;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.util.EntityUtils;

public class ShareLastReleaseWorker extends Worker {
    private final Context mContext;

    public ShareLastReleaseWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mContext = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet(Updater.GITHUB_RELEASES_URL);
        try {
            // кастомный обработчик ответов
            ResponseHandler<String> responseHandler = response -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    try {
                        String body = EntityUtils.toString(entity);
                        JSONObject releaseInfo = new JSONObject(body);
                        // версии отличаются
                        // получу ссылку на скачивание
                        JSONObject releaseAssets = releaseInfo.getJSONArray("assets").getJSONObject(0);
                        String downloadLink = releaseAssets.getString(Updater.GITHUB_DOWNLOAD_LINK);
                        String textToShare = String.format(Locale.ENGLISH, App.getInstance().getString(R.string.latest_release_here_template), downloadLink);
                        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setType("text/plain");
                        intent.putExtra(Intent.EXTRA_SUBJECT, App.getInstance().getString(R.string.share_app_link_title));
                        intent.putExtra(Intent.EXTRA_TEXT, textToShare);
                        Intent chooser = Intent.createChooser(intent, App.getInstance().getString(R.string.share_app_message));
                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(chooser);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    // неверный ответ с сервера
                    Log.d("surprise", "CheckUpdateWorker handleResponse: wrong update server answer");
                }
                return null;
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
