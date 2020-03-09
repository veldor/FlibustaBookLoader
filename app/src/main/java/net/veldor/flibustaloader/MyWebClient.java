package net.veldor.flibustaloader;

import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import net.veldor.flibustaloader.workers.GetAllPagesWorker;
import net.veldor.flibustaloader.workers.GetPageWorker;

public class MyWebClient {

    private static final String PAGE_LOAD_WORKER = "page load worker";

    MyWebClient() {
    }

    public static final String LOADED_URL = "loaded_url";
    public static final String DOWNLOAD_ATTRIBUTES = "download attributes";

    void search(String s) {
        // сброшу обложку
        App.getInstance().mShowCover.postValue(null);
        // отменю остальные работы
        Log.d("surprise", "MyWebClient search search " + s);
        Data inputData = new Data.Builder()
                .putString(LOADED_URL, s)
                .build();
        OneTimeWorkRequest getPageWorker;
        // запущу рабочего, загружающего страницу
        if (App.getInstance().isDownloadAll()) {
            getPageWorker = new OneTimeWorkRequest.Builder(GetAllPagesWorker.class).addTag(PAGE_LOAD_WORKER).setInputData(inputData).build();
            WorkManager.getInstance(App.getInstance()).enqueue(getPageWorker);
        } else {
            getPageWorker = new OneTimeWorkRequest.Builder(GetPageWorker.class).addTag(PAGE_LOAD_WORKER).setInputData(inputData).build();
            WorkManager.getInstance(App.getInstance()).enqueue(getPageWorker);
        }
        // отмечу, что выполняется работа по загрузке контента
        App.getInstance().mSearchWork = WorkManager.getInstance(App.getInstance()).getWorkInfoByIdLiveData(getPageWorker.getId());
        // сохраню активный процесс
        App.getInstance().mProcess = getPageWorker;
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
