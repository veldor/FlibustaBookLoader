package net.veldor.flibustaloader.updater

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.workers.CheckUpdateWorker
import net.veldor.flibustaloader.workers.MakeUpdateWorker

object Updater {
    const val GITHUB_RELEASES_URL =
        "https://api.github.com/repos/veldor/FlibustaBookLoader/releases/latest"
    const val GITHUB_APP_VERSION = "tag_name"
    const val GITHUB_DOWNLOAD_LINK = "browser_download_url"
    const val GITHUB_APP_NAME = "name"
    @JvmField
    val newVersion = MutableLiveData<Boolean>()

    // место для хранения идентификатора загрузки обновления
    @JvmField
    val updateDownloadIdentification = MutableLiveData<Long>()
    @JvmStatic
    fun checkUpdate(): LiveData<Boolean> {
        // даю задание worker-у
        val startUpdateWorker = OneTimeWorkRequest.Builder(CheckUpdateWorker::class.java).build()
        WorkManager.getInstance(App.instance).enqueue(startUpdateWorker)
        return newVersion
    }

    @JvmStatic
    fun update() {
        // даю задание worker-у
        val startUpdateWorker = OneTimeWorkRequest.Builder(MakeUpdateWorker::class.java).build()
        WorkManager.getInstance(App.instance).enqueue(startUpdateWorker)
    }
}