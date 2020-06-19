package net.veldor.flibustaloader.view_models;

import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.selections.SubscriptionItem;
import net.veldor.flibustaloader.utils.MyPreferences;
import net.veldor.flibustaloader.utils.SubscribesHandler;
import net.veldor.flibustaloader.workers.AddBooksToDownloadQueueWorker;
import net.veldor.flibustaloader.workers.CheckSubscriptionsWorker;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static net.veldor.flibustaloader.workers.CheckSubscriptionsWorker.PERIODIC_CHECK_TAG;

public class SubscriptionsViewModel extends ViewModel {

    public static final MutableLiveData<Boolean> sSubscriptionsChecked = new MutableLiveData<>();

    public void switchSubscriptionsAutoCheck() {
        MyPreferences.getInstance().switchSubscriptionsAutoCheck();
        if(MyPreferences.getInstance().isSubscriptionsAutoCheck()){
            Toast.makeText(App.getInstance(), App.getInstance().getString(R.string.autocheck_enabled_message),Toast.LENGTH_SHORT).show();
            // Запланирую проверку подписок
            PeriodicWorkRequest.Builder startPeriodicalPlanner = new PeriodicWorkRequest.Builder(CheckSubscriptionsWorker.class, 24, TimeUnit.HOURS).addTag(PERIODIC_CHECK_TAG);
            WorkManager wm = WorkManager.getInstance(App.getInstance());
            wm.cancelAllWorkByTag(PERIODIC_CHECK_TAG);
            wm.enqueue(startPeriodicalPlanner.build());
        }
        else{
            Toast.makeText(App.getInstance(), App.getInstance().getString(R.string.autocheck_disabled_message),Toast.LENGTH_SHORT).show();
            WorkManager wm = WorkManager.getInstance(App.getInstance());
            wm.cancelAllWorkByTag(PERIODIC_CHECK_TAG);
        }
    }

    public void checkSubscribes() {
        // проверю, подписан ли я на новинки
        ArrayList<SubscriptionItem> subscribes = SubscribesHandler.getAllSubscribes();
        if(subscribes.size() > 0){
            // запущу рабочего, который проверит все новинки
            OneTimeWorkRequest checkSubscribes = new OneTimeWorkRequest.Builder(CheckSubscriptionsWorker.class).addTag(CheckSubscriptionsWorker.CHECK_SUBSCRIBES).build();
            WorkManager.getInstance(App.getInstance()).enqueueUniqueWork(CheckSubscriptionsWorker.CHECK_SUBSCRIBES, ExistingWorkPolicy.REPLACE, checkSubscribes);
        }
        else {
            Toast.makeText(App.getInstance(), App.getInstance().getString(R.string.not_found_subscribes_message), Toast.LENGTH_SHORT).show();
        }
    }

    public void fullCheckSubscribes() {
        ArrayList<SubscriptionItem> subscribes = SubscribesHandler.getAllSubscribes();
        if(subscribes.size() > 0){
            Data inputData = new Data.Builder()
                    .putBoolean(CheckSubscriptionsWorker.FULL_CHECK, true)
                    .build();
            // запущу рабочего, который проверит все новинки
            OneTimeWorkRequest checkSubscribes = new OneTimeWorkRequest.Builder(CheckSubscriptionsWorker.class).setInputData(inputData).addTag(CheckSubscriptionsWorker.CHECK_SUBSCRIBES).build();
            WorkManager.getInstance(App.getInstance()).enqueueUniqueWork(CheckSubscriptionsWorker.CHECK_SUBSCRIBES, ExistingWorkPolicy.REPLACE, checkSubscribes);
        }
        else {
            Toast.makeText(App.getInstance(), App.getInstance().getString(R.string.not_found_subscribes_message), Toast.LENGTH_SHORT).show();
        }
    }

    public LiveData<Boolean> getCheckData() {
        return sSubscriptionsChecked;
    }

    public void addToDownloadQueue(DownloadLink downloadLink) {
        AddBooksToDownloadQueueWorker.addLink(downloadLink);
        if(MyPreferences.getInstance().isDownloadAutostart()){
            App.getInstance().initializeDownload();
        }
    }
}
