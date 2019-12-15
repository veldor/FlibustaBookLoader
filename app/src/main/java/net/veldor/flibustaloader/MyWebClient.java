package net.veldor.flibustaloader;

import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.utils.MimeTypes;
import net.veldor.flibustaloader.workers.DownloadBookWorker;
import net.veldor.flibustaloader.workers.GetAllPagesWorker;
import net.veldor.flibustaloader.workers.GetPageWorker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.protocol.HttpClientContext;

public class MyWebClient {

    static final String PAGE_LOAD_WORKER = "page load worker";
    public HttpClientContext mContext;
    public HttpClient mHttpClient;

    MyWebClient() {
    }

    public static final String LOADED_URL = "loaded_url";
    public static final String DOWNLOAD_ATTRIBUTES = "download attributes";

    void search(String s) {
        // отменю остальные работы
        Log.d("surprise", "MyWebClient search search " + s);
        Data inputData = new Data.Builder()
                .putString(LOADED_URL, s)
                .build();
        OneTimeWorkRequest getPageWorker;
        // запущу рабочего, загружающего страницу
        if (App.getInstance().isDownloadAll()) {
            getPageWorker = new OneTimeWorkRequest.Builder(GetAllPagesWorker.class).addTag(PAGE_LOAD_WORKER).setInputData(inputData).build();
            WorkManager.getInstance().enqueue(getPageWorker);
        } else {
            getPageWorker = new OneTimeWorkRequest.Builder(GetPageWorker.class).addTag(PAGE_LOAD_WORKER).setInputData(inputData).build();
            WorkManager.getInstance().enqueue(getPageWorker);
        }
        // отмечу, что выполняется работа по загрузке контента
        App.getInstance().mSearchWork = WorkManager.getInstance().getWorkInfoByIdLiveData(getPageWorker.getId());
        // сохраню активный процесс
        App.getInstance().mProcess = getPageWorker;
    }

    void download(DownloadLink item) {
        // запущу рабочего, который загрузит книгу
        String[] data = new String[4];
        data[0] = MimeTypes.getDownloadMime(item.mime);
        data[1] = item.url;
        data[2] = item.name;
        data[3] = item.author;
        Data inputData = new Data.Builder()
                .putStringArray(DOWNLOAD_ATTRIBUTES, data)
                .build();
        // запущу рабочего, загружающего файл
        OneTimeWorkRequest downloadBookWorker = new OneTimeWorkRequest.Builder(DownloadBookWorker.class).setInputData(inputData).build();
        WorkManager.getInstance().enqueue(downloadBookWorker);
    }


    public String request(String text) {
        try {
            HttpGet httpGet = new HttpGet(text);
            httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.119 Safari/537.36");
            httpGet.setHeader("X-Compress", "null");
            HttpResponse httpResponse = mHttpClient.execute(httpGet, mContext);
            InputStream is;
            is = httpResponse.getEntity().getContent();
            return inputStreamToString(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private String inputStreamToString(InputStream is) {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            StringBuilder total = new StringBuilder();
            for (String line; (line = r.readLine()) != null; ) {
                total.append(line).append('\n');
            }
            return total.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    void loadNextPage() {
        // если есть ссылка на следующую страницу- гружу её
        String nextPageLink = App.getInstance().mNextPageUrl;
        if (nextPageLink != null && !nextPageLink.isEmpty()) {
            Log.d("surprise", "MyWebClient loadNextPage: " + nextPageLink);
            search(App.BASE_URL + nextPageLink);
        } else {
            // видимо, какая то ошибка, делаю вид, что ничего не найдено
            App.getInstance().mSearchResult.postValue(null);
        }
    }
}
