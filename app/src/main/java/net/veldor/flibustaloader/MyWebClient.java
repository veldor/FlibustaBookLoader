package net.veldor.flibustaloader;

import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.utils.MimeTypes;
import net.veldor.flibustaloader.workers.DownloadBookWorker;
import net.veldor.flibustaloader.workers.GetPageWorker;

public class MyWebClient {

    public static final String LOADED_URL = "loaded_url";
    public static final String DOWNLOAD_ATTRIBUTES = "download attributes";

    void search(String s) {
        Data inputData = new Data.Builder()
                .putString(LOADED_URL, s)
                .build();
        // запущу рабочего, загружающего страницу
        OneTimeWorkRequest getPageWorker = new OneTimeWorkRequest.Builder(GetPageWorker.class).setInputData(inputData).build();
        WorkManager.getInstance().enqueue(getPageWorker);
    }

    void download(DownloadLink item) {
        // запущу рабочего, который загрузит книгу
        String[] data = new String[3];
        data[0] = MimeTypes.getMime(item.mime);
        data[1] = item.url;
        data[2] = item.name;
        Data inputData = new Data.Builder()
                .putStringArray(DOWNLOAD_ATTRIBUTES,data)
                .build();
        // запущу рабочего, загружающего файл
        OneTimeWorkRequest downloadBookWorker = new OneTimeWorkRequest.Builder(DownloadBookWorker.class).setInputData(inputData).build();
        WorkManager.getInstance().enqueue(downloadBookWorker);
    }
}
