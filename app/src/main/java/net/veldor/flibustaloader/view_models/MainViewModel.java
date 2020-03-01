package net.veldor.flibustaloader.view_models;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.annotation.NonNull;

import android.net.Uri;
import android.util.Log;
import android.util.SparseBooleanArray;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.MyWebView;
import net.veldor.flibustaloader.OPDSActivity;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.selections.FoundedItem;
import net.veldor.flibustaloader.updater.Updater;
import net.veldor.flibustaloader.utils.BookSharer;
import net.veldor.flibustaloader.utils.MimeTypes;
import net.veldor.flibustaloader.utils.MyFileReader;
import net.veldor.flibustaloader.utils.XMLHandler;
import net.veldor.flibustaloader.workers.DatabaseWorker;
import net.veldor.flibustaloader.workers.DownloadBooksWorker;
import net.veldor.flibustaloader.workers.AddBooksToDownloadQueueWorker;
import net.veldor.flibustaloader.workers.ReserveSettingsWorker;
import net.veldor.flibustaloader.workers.RestoreSettingsWorker;

import java.util.ArrayList;
import java.util.Random;

@SuppressWarnings("unchecked")
public class MainViewModel extends AndroidViewModel {

    private static final String MULTIPLY_DOWNLOAD = "multiply download";
    private static final String ADD_TO_DOWNLOAD_QUEUE_ACTION = "add to download queue";

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

    public void setBookRead(FoundedBook book) {
        // запущу рабочего, который отметит книгу как прочитанную
        Data inputData = new Data.Builder()
                .putString(OPDSActivity.BOOK_ID, book.id)
                .putInt(DatabaseWorker.WORK_TYPE, DatabaseWorker.INSERT_BOOK)
                .build();
        // запущу рабочего, загружающего страницу
        OneTimeWorkRequest getPageWorker = new OneTimeWorkRequest.Builder(DatabaseWorker.class).setInputData(inputData).build();
        WorkManager.getInstance(App.getInstance()).enqueue(getPageWorker);
    }

    public void clearHistory() {
        MyFileReader.clearAutocomplete();
    }

    public void reserveSettings(){
        // запущу рабочего, сохраняющего базу данных прочитанного и скачанного в XML
        OneTimeWorkRequest reserveWorker = new OneTimeWorkRequest.Builder(ReserveSettingsWorker.class).build();
        WorkManager.getInstance(App.getInstance()).enqueue(reserveWorker);
    }

    public void restore(Uri uri) {
        Data inputData = new Data.Builder()
                .putString(RestoreSettingsWorker.URI, uri.toString())
                .build();
        OneTimeWorkRequest restoreWorker = new OneTimeWorkRequest.Builder(RestoreSettingsWorker.class).setInputData(inputData).build();
        WorkManager.getInstance(App.getInstance()).enqueue(restoreWorker);
    }

    public LiveData<WorkInfo> downloadSelected(SparseBooleanArray ids) {
        App.getInstance().mDownloadSelectedBooks = ids;
        OneTimeWorkRequest downloadSelected = new OneTimeWorkRequest.Builder(AddBooksToDownloadQueueWorker.class).addTag(ADD_TO_DOWNLOAD_QUEUE_ACTION).build();
        WorkManager.getInstance(App.getInstance()).enqueueUniqueWork(ADD_TO_DOWNLOAD_QUEUE_ACTION, ExistingWorkPolicy.REPLACE, downloadSelected);
        return WorkManager.getInstance(App.getInstance()).getWorkInfoByIdLiveData(downloadSelected.getId());
    }
    public LiveData<WorkInfo> downloadAll() {
        App.getInstance().mDownloadSelectedBooks = null;
        OneTimeWorkRequest downloadSelected = new OneTimeWorkRequest.Builder(AddBooksToDownloadQueueWorker.class).addTag(ADD_TO_DOWNLOAD_QUEUE_ACTION).build();
        WorkManager.getInstance(App.getInstance()).enqueueUniqueWork(ADD_TO_DOWNLOAD_QUEUE_ACTION, ExistingWorkPolicy.REPLACE, downloadSelected);
        return WorkManager.getInstance(App.getInstance()).getWorkInfoByIdLiveData(downloadSelected.getId());
    }

    public void initiateMassDownload() {
        // проверю, не запущен ли уже рабочий, загружающий книги
        LiveData<WorkInfo> statusContainer = App.getInstance().mDownloadAllWork;
        if(statusContainer == null || statusContainer.getValue() == null ||  statusContainer.getValue().getState() == WorkInfo.State.SUCCEEDED){
            // запущу рабочего, который загрузит все книги
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();
            OneTimeWorkRequest downloadAllWorker = new OneTimeWorkRequest.Builder(DownloadBooksWorker.class).addTag(MULTIPLY_DOWNLOAD).setConstraints(constraints).build();
            WorkManager.getInstance(App.getInstance()).enqueue(downloadAllWorker);
            App.getInstance().mDownloadAllWork = WorkManager.getInstance(App.getInstance()).getWorkInfoByIdLiveData(downloadAllWorker.getId());
        }
    }
}
