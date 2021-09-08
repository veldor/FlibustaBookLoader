package net.veldor.flibustaloader.view_models

import net.veldor.flibustaloader.App
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import net.veldor.flibustaloader.workers.LoginWorker
import androidx.work.*

class LoginViewModel : ViewModel() {
    fun logMeIn(login: String?, password: String?): LiveData<WorkInfo> {
        // запущу рабочего, который выполнит вход в аккаунт
        val inputData = Data.Builder()
            .putString(LoginWorker.USER_LOGIN, login)
            .putString(LoginWorker.USER_PASSWORD, password)
            .build()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val loginWork =
            OneTimeWorkRequest.Builder(LoginWorker::class.java).addTag(LoginWorker.LOGIN_ACTION)
                .setInputData(inputData).setConstraints(constraints).build()
        WorkManager.getInstance(App.instance)
            .enqueueUniqueWork(LoginWorker.LOGIN_ACTION, ExistingWorkPolicy.REPLACE, loginWork)
        return WorkManager.getInstance(App.instance).getWorkInfoByIdLiveData(loginWork.id)
    }
}