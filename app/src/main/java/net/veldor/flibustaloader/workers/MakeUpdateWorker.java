package net.veldor.flibustaloader.workers;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.BuildConfig;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.services.UpdateWaitService;
import net.veldor.flibustaloader.updater.Updater;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
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

public class MakeUpdateWorker extends Worker {
    private final Context mContext;

    public MakeUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
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
                                // получу ссылку на скачивание
                                JSONObject releaseAssets = releaseInfo.getJSONArray("assets").getJSONObject(0);
                                String downloadLink = releaseAssets.getString(Updater.GITHUB_DOWNLOAD_LINK);
                                String downloadName = releaseAssets.getString(Updater.GITHUB_APP_NAME);
                                String downloadedApkFilePath = mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/" + downloadName;
                                File downloadedApkFile = new File(downloadedApkFilePath);
                                Uri downloadUri = Uri.parse("file://" + downloadedApkFile);
                                App.getInstance().updateDownloadUri = downloadUri;
                                if (downloadedApkFile.exists()) {
                                    boolean deleteResult = downloadedApkFile.delete();
                                    if(!deleteResult){
                                        Log.d("surprise", "MakeUpdateWorker handleResponse: Не смог удалить предыдущий файл");
                                    }
                                }
                                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadLink));
                                request.setTitle(mContext.getString(R.string.update_file_name));
                                request.setDestinationUri(downloadUri);
                                DownloadManager manager = (DownloadManager) mContext.getSystemService(
                                        Context.DOWNLOAD_SERVICE);
                                long startedDownloadId = manager.enqueue(request);
                                // загрузка начата, отправлю идентификатор загрузки менеджеру
                                Updater.updateDownloadIdentification.postValue(startedDownloadId);
                                App.getInstance().downloadedApkFile = downloadedApkFile;
                                // запущу сервис отслеживания окончания загрузки
                                mContext.startService(new Intent(mContext, UpdateWaitService.class));
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
