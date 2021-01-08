package net.veldor.flibustaloader.view_models;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.workers.CheckFlibustaAvailabilityWorker;
import net.veldor.flibustaloader.workers.LoginWorker;

import static net.veldor.flibustaloader.workers.LoginWorker.LOGIN_ACTION;
import static net.veldor.flibustaloader.workers.LoginWorker.USER_LOGIN;
import static net.veldor.flibustaloader.workers.LoginWorker.USER_PASSWORD;

public class StartViewModel extends ViewModel {
    public LiveData<WorkInfo> checkFlibustaAvailability() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(CheckFlibustaAvailabilityWorker.class).addTag(CheckFlibustaAvailabilityWorker.ACTION).setConstraints(constraints).build();
        WorkManager.getInstance(App.getInstance()).enqueueUniqueWork(CheckFlibustaAvailabilityWorker.ACTION, ExistingWorkPolicy.REPLACE, work);
        return WorkManager.getInstance(App.getInstance()).getWorkInfoByIdLiveData(work.getId());
    }
}
