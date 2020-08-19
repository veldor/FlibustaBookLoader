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
import net.veldor.flibustaloader.workers.LoginWorker;

import static net.veldor.flibustaloader.workers.LoginWorker.LOGIN_ACTION;
import static net.veldor.flibustaloader.workers.LoginWorker.USER_LOGIN;
import static net.veldor.flibustaloader.workers.LoginWorker.USER_PASSWORD;

public class LoginViewModel extends ViewModel {
    public LiveData<WorkInfo> logMeIn(String login, String password) {
        // запущу рабочего, который выполнит вход в аккаунт
        Data inputData = new Data.Builder()
                .putString(USER_LOGIN, login)
                .putString(USER_PASSWORD, password)
                .build();
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest loginWork = new OneTimeWorkRequest.Builder(LoginWorker.class).addTag(LOGIN_ACTION).setInputData(inputData).setConstraints(constraints).build();
        WorkManager.getInstance(App.getInstance()).enqueueUniqueWork(LOGIN_ACTION, ExistingWorkPolicy.REPLACE, loginWork);
        return WorkManager.getInstance(App.getInstance()).getWorkInfoByIdLiveData(loginWork.getId());
    }
}
