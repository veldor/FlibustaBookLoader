package net.veldor.flibustaloader.view_models

import net.veldor.flibustaloader.App
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import net.veldor.flibustaloader.workers.CheckFlibustaAvailabilityWorker
import androidx.work.*

class StartViewModel : ViewModel() {
    fun checkFlibustaAvailability(): LiveData<WorkInfo> {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val work = OneTimeWorkRequest.Builder(CheckFlibustaAvailabilityWorker::class.java)
            .addTag(CheckFlibustaAvailabilityWorker.ACTION).setConstraints(constraints).build()
        WorkManager.getInstance(App.instance).enqueueUniqueWork(
            CheckFlibustaAvailabilityWorker.ACTION,
            ExistingWorkPolicy.KEEP,
            work
        )
        return WorkManager.getInstance(App.instance).getWorkInfoByIdLiveData(work.id)
    }
}