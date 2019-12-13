package net.veldor.flibustaloader.view_models;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;
import android.util.Log;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.MyWebView;
import net.veldor.flibustaloader.ODPSActivity;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.updater.Updater;
import net.veldor.flibustaloader.utils.BookSharer;
import net.veldor.flibustaloader.utils.MimeTypes;
import net.veldor.flibustaloader.utils.MyFileReader;
import net.veldor.flibustaloader.utils.XMLHandler;
import net.veldor.flibustaloader.workers.DatabaseWorker;
import net.veldor.flibustaloader.workers.DownloadBooksWorker;

import java.util.ArrayList;
import java.util.Random;

public class MainViewModel extends AndroidViewModel {

    private static final String MULTIPLY_DOWNLOAD = "multiply download";

    public MainViewModel(@NonNull Application application) {
        super(application);
    }

    public int getViewMode(){
        return App.getInstance().getViewMode();
    }
    public void switchViewMode(int type){
        App.getInstance().switchViewMode(type);
    }

    // загрузка ядра TOR
    public LiveData<AndroidOnionProxyManager> getTor(){
        return App.getInstance().mTorManager;
    }

    public LiveData<Boolean> startCheckUpdate() {
        return Updater.checkUpdate();
    }

    public void initializeUpdate() {
        Updater.update();
    }

    public void switchNightMode() {
        App.getInstance().switchNightMode();
    }

    public boolean getNightModeEnabled() {
        return App.getInstance().getNightMode();
    }

    public ArrayList<String> getSearchAutocomplete() {
        String content = MyFileReader.getSearchAutocomplete();
        return XMLHandler.getSearchAutocomplete(content);
    }

    public String getRandomBookUrl() {
        Random random = new Random();
        return App.BASE_BOOK_URL + random.nextInt(App.MAX_BOOK_NUMBER);
    }

    public void shareLink(MyWebView mWebView) {
        BookSharer.shareLink(mWebView.getUrl());
    }

    public void switchODPSMode() {
        App.getInstance().switchODPSMode();
    }

    public void setBookRead(FoundedBook book) {
        // запущу рабочего, который отметит книгу как прочитанную
        Data inputData = new Data.Builder()
                .putString(ODPSActivity.BOOK_ID, book.id)
                .putInt(DatabaseWorker.WORK_TYPE, DatabaseWorker.INSERT_BOOK)
                .build();
        // запущу рабочего, загружающего страницу
        OneTimeWorkRequest getPageWorker = new OneTimeWorkRequest.Builder(DatabaseWorker.class).setInputData(inputData).build();
        WorkManager.getInstance().enqueue(getPageWorker);
    }

    public void downloadMultiply(int i) {
        Log.d("surprise", "MainViewModel downloadMultiply initiate download");
        // отменю все остальные работы
        WorkManager.getInstance().cancelAllWork();
        // запущу рабочего, который загрузит книги
        Data inputData = new Data.Builder()
                .putInt(MimeTypes.MIME_TYPE, i)
                .build();
        OneTimeWorkRequest downloadAllWorker = new OneTimeWorkRequest.Builder(DownloadBooksWorker.class).setInputData(inputData).build();
        WorkManager.getInstance().beginUniqueWork(MULTIPLY_DOWNLOAD, ExistingWorkPolicy.KEEP, downloadAllWorker).enqueue();
        App.getInstance().mDownloadAllWork = WorkManager.getInstance().getWorkInfoByIdLiveData(downloadAllWorker.getId());
    }
}
