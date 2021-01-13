package net.veldor.flibustaloader.view_models;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.workers.CheckFlibustaAvailabilityWorker;

public class StartViewModel extends ViewModel {
    public LiveData<WorkInfo> checkFlibustaAvailability() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(CheckFlibustaAvailabilityWorker.class).addTag(CheckFlibustaAvailabilityWorker.ACTION).setConstraints(constraints).build();
        WorkManager.getInstance(App.getInstance()).enqueueUniqueWork(CheckFlibustaAvailabilityWorker.ACTION, ExistingWorkPolicy.KEEP, work);
        return WorkManager.getInstance(App.getInstance()).getWorkInfoByIdLiveData(work.getId());
    }
}
