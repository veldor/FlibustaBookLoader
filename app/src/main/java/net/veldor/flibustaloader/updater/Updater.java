package net.veldor.flibustaloader.updater;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.util.Log;

import net.veldor.flibustaloader.workers.CheckUpdateWorker;
import net.veldor.flibustaloader.workers.MakeUpdateWorker;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class Updater {

    public static final String GITHUB_RELEASES_URL = "https://api.github.com/repos/veldor/FlibustaBookLoader/releases/latest";
    public static final String GITHUB_APP_VERSION = "tag_name";
    public static final String GITHUB_DOWNLOAD_LINK = "browser_download_url";
    public static final String GITHUB_APP_NAME = "name";

    // место для хранения TOR клиента
    public static MutableLiveData<Boolean> newVersion = new MutableLiveData<>();
    // место для хранения идентификатора загрузки обновления
    public static MutableLiveData<Long> updateDownloadIdentificator = new MutableLiveData<>();

    public static LiveData<Boolean> checkUpdate(){
        Log.d("surprise", "Updater checkUpdate: checking update");
        // даю задание worker-у
        OneTimeWorkRequest startUpdateWorker = new OneTimeWorkRequest.Builder(CheckUpdateWorker.class).build();
        WorkManager.getInstance().enqueue(startUpdateWorker);
        return newVersion;
    }

    public static void update() {
        // даю задание worker-у
        OneTimeWorkRequest startUpdateWorker = new OneTimeWorkRequest.Builder(MakeUpdateWorker.class).build();
        WorkManager.getInstance().enqueue(startUpdateWorker);
    }
}