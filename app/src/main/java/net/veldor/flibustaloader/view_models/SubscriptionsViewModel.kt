package net.veldor.flibustaloader.view_models

import net.veldor.flibustaloader.workers.AddBooksToDownloadQueueWorker.Companion.addLink
import net.veldor.flibustaloader.utils.SubscribesHandler.getAllSubscribes
import net.veldor.flibustaloader.App
import androidx.lifecycle.LiveData
import net.veldor.flibustaloader.selections.DownloadLink
import androidx.lifecycle.ViewModel
import android.widget.Toast
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.workers.CheckSubscriptionsWorker
import androidx.lifecycle.MutableLiveData
import androidx.work.*
import net.veldor.flibustaloader.utils.PreferencesHandler
import java.util.concurrent.TimeUnit

class SubscriptionsViewModel : ViewModel() {
    fun switchSubscriptionsAutoCheck() {
        PreferencesHandler.instance.isSubscriptionsAutoCheck = !PreferencesHandler.instance.isSubscriptionsAutoCheck
        if (PreferencesHandler.instance.isSubscriptionsAutoCheck) {
            Toast.makeText(
                App.instance,
                App.instance.getString(R.string.autocheck_enabled_message),
                Toast.LENGTH_SHORT
            ).show()
            // Запланирую проверку подписок
            val startPeriodicalPlanner = PeriodicWorkRequest.Builder(
                CheckSubscriptionsWorker::class.java, 24, TimeUnit.HOURS
            ).addTag(
                CheckSubscriptionsWorker.PERIODIC_CHECK_TAG
            )
            val wm = WorkManager.getInstance(App.instance)
            wm.cancelAllWorkByTag(CheckSubscriptionsWorker.PERIODIC_CHECK_TAG)
            wm.enqueue(startPeriodicalPlanner.build())
        } else {
            Toast.makeText(
                App.instance,
                App.instance.getString(R.string.autocheck_disabled_message),
                Toast.LENGTH_SHORT
            ).show()
            val wm = WorkManager.getInstance(App.instance)
            wm.cancelAllWorkByTag(CheckSubscriptionsWorker.PERIODIC_CHECK_TAG)
        }
    }

    fun checkSubscribes() {
        // проверю, подписан ли я на новинки
        val subscribes = getAllSubscribes()
        if (subscribes.size > 0) {
            // запущу рабочего, который проверит все новинки
            val checkSubscribes = OneTimeWorkRequest.Builder(
                CheckSubscriptionsWorker::class.java
            ).addTag(CheckSubscriptionsWorker.CHECK_SUBSCRIBES).build()
            WorkManager.getInstance(App.instance).enqueueUniqueWork(
                CheckSubscriptionsWorker.CHECK_SUBSCRIBES,
                ExistingWorkPolicy.REPLACE,
                checkSubscribes
            )
        } else {
            Toast.makeText(
                App.instance,
                App.instance.getString(R.string.not_found_subscribes_message),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun fullCheckSubscribes() {
        val subscribes = getAllSubscribes()
        if (subscribes.size > 0) {
            val inputData = Data.Builder()
                .putBoolean(CheckSubscriptionsWorker.FULL_CHECK, true)
                .build()
            // запущу рабочего, который проверит все новинки
            val checkSubscribes = OneTimeWorkRequest.Builder(
                CheckSubscriptionsWorker::class.java
            ).setInputData(inputData).addTag(CheckSubscriptionsWorker.CHECK_SUBSCRIBES).build()
            WorkManager.getInstance(App.instance).enqueueUniqueWork(
                CheckSubscriptionsWorker.CHECK_SUBSCRIBES,
                ExistingWorkPolicy.REPLACE,
                checkSubscribes
            )
        } else {
            Toast.makeText(
                App.instance,
                App.instance.getString(R.string.not_found_subscribes_message),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val checkData: LiveData<Boolean>
        get() = sSubscriptionsChecked

    fun addToDownloadQueue(downloadLink: DownloadLink?) {
        addLink(downloadLink!!)
        if (PreferencesHandler.instance.isDownloadAutostart) {
            App.instance.initializeDownload()
        }
    }

    companion object {
        val sSubscriptionsChecked = MutableLiveData<Boolean>()
    }
}