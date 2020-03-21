package net.veldor.flibustaloader.http;

import android.util.Log;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.utils.URLHandler;
import net.veldor.flibustaloader.workers.GetAllPagesWorker;
import net.veldor.flibustaloader.workers.GetPageWorker;

import static net.veldor.flibustaloader.MyWebClient.LOADED_URL;
import static net.veldor.flibustaloader.MyWebClient.PAGE_LOAD_WORKER;

public class ExternalVpnClient {
    public static void search(String s) {
        // сброшу обложку
        App.getInstance().mShowCover.postValue(null);
        // отменю остальные работы
        Data inputData = new Data.Builder()
                .putString(LOADED_URL, s)
                .build();
        OneTimeWorkRequest getPageWorker;
        // запущу рабочего, загружающего страницу
        if (App.getInstance().isDownloadAll()) {
            getPageWorker = new OneTimeWorkRequest.Builder(GetAllPagesWorker.class).addTag(PAGE_LOAD_WORKER).setInputData(inputData).build();
            WorkManager.getInstance(App.getInstance()).enqueueUniqueWork(PAGE_LOAD_WORKER, ExistingWorkPolicy.REPLACE, getPageWorker);
        } else {
            getPageWorker = new OneTimeWorkRequest.Builder(GetPageWorker.class).addTag(PAGE_LOAD_WORKER).setInputData(inputData).build();
            WorkManager.getInstance(App.getInstance()).enqueueUniqueWork(PAGE_LOAD_WORKER, ExistingWorkPolicy.REPLACE, getPageWorker);
        }
        // отмечу, что выполняется работа по загрузке контента
        App.getInstance().mSearchWork = WorkManager.getInstance(App.getInstance()).getWorkInfoByIdLiveData(getPageWorker.getId());
        // сохраню активный процесс
        App.getInstance().mProcess = getPageWorker;
    }

    public static void loadNextPage() {
        String nextPageLink = App.getInstance().mNextPageUrl;
        if (nextPageLink != null && !nextPageLink.isEmpty()) {
            Log.d("surprise", "MyWebClient loadNextPage: " + nextPageLink);
            search(URLHandler.getBaseUrl() + nextPageLink);
        } else {
            // видимо, какая то ошибка, делаю вид, что ничего не найдено
            App.getInstance().mSearchResult.postValue(null);
        }
    }
}
