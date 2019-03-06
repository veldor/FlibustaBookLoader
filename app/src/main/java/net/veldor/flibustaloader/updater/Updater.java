package net.veldor.flibustaloader.updater;

import android.arch.lifecycle.MutableLiveData;

import net.veldor.flibustaloader.workers.CheckUpdateWorker;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;


public class Updater {

    public static final String GITHUB_RELEASES_URL = "https://api.github.com/repos/veldor/FlibustaBookLoader/releases/latest";

    // место для хранения TOR клиента
    public static MutableLiveData<Boolean> newVersion = new MutableLiveData<>();

    public static void checkUpdate(){
        // даю задание worker-у
        OneTimeWorkRequest startUpdateWorker = new OneTimeWorkRequest.Builder(CheckUpdateWorker.class).build();
        WorkManager.getInstance().enqueue(startUpdateWorker);
    }
}
