package net.veldor.flibustaloader.view_models

import android.util.Log
import net.veldor.flibustaloader.App
import androidx.lifecycle.ViewModel
import android.widget.Toast
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.workers.CheckSubscriptionsWorker
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.veldor.flibustaloader.handlers.DownloadLinkHandler
import net.veldor.flibustaloader.selections.DownloadLink
import net.veldor.flibustaloader.selections.FoundedEntity
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.utils.SubscribesHandler
import java.util.concurrent.TimeUnit

class SubscriptionsViewModel : ViewModel() {

    var liveFoundedSubscription = MutableLiveData<FoundedEntity>()

    fun switchSubscriptionsAutoCheck() {
        PreferencesHandler.instance.isSubscriptionsAutoCheck =
            !PreferencesHandler.instance.isSubscriptionsAutoCheck
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
        viewModelScope.launch(Dispatchers.IO) {
            foundedSubscribes.postValue(arrayListOf())
            liveCheckInProgress.postValue(true)
            val lastCheckedBookId = SubscribesHandler.checkSubscribes(liveFoundedSubscription, true)
            if(lastCheckedBookId != null){
                PreferencesHandler.instance.lastCheckedBookId = lastCheckedBookId
            }
        }
    }

    fun fullCheckSubscribes() {
        viewModelScope.launch(Dispatchers.IO) {
            foundedSubscribes.postValue(arrayListOf())
            liveCheckInProgress.postValue(true)
            SubscribesHandler.checkSubscribes(liveFoundedSubscription, false)
        }
    }

    fun addToDownloadQueue(downloadLink: DownloadLink) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("surprise", "addToDownloadQueue: adding download link: " + downloadLink.url)
            DownloadLinkHandler().addLink(downloadLink)
        }
    }

    companion object {
        val liveCheckInProgress = MutableLiveData(false)
        val foundedSubscribes = MutableLiveData<ArrayList<FoundedEntity>>(arrayListOf())
    }
}