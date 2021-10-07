package net.veldor.flibustaloader.updater

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.MutableLiveData
import cz.msebera.android.httpclient.HttpEntity
import cz.msebera.android.httpclient.HttpResponse
import cz.msebera.android.httpclient.client.ResponseHandler
import cz.msebera.android.httpclient.client.methods.HttpGet
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient
import cz.msebera.android.httpclient.impl.client.HttpClients
import cz.msebera.android.httpclient.util.EntityUtils
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.BuildConfig
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.services.UpdateWaitService
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException

object Updater {
    const val GITHUB_RELEASES_URL =
        "https://api.github.com/repos/veldor/FlibustaBookLoader/releases/latest"
    private const val GITHUB_APP_VERSION = "tag_name"
    const val GITHUB_DOWNLOAD_LINK = "browser_download_url"
    private const val GITHUB_APP_NAME = "name"
    @JvmField
    val newVersion = MutableLiveData<Boolean>()

    // место для хранения идентификатора загрузки обновления
    @JvmField
    val updateDownloadIdentification = MutableLiveData<Long>()
    @JvmStatic
    fun checkUpdate(): Boolean {
        var updateAvailable = false
        // даю задание worker-у
//        if (App.isTestVersion) {
//            return false
//        }
        val httpclient: CloseableHttpClient = HttpClients.createDefault()
        val httpget = HttpGet(GITHUB_RELEASES_URL)
        try {
            // кастомный обработчик ответов
            val responseHandler: ResponseHandler<String> =
                ResponseHandler<String> { response: HttpResponse ->
                    val status = response.statusLine.statusCode
                    if (status in 200..299) {
                        val entity: HttpEntity = response.entity
                        try {
                            val body: String = EntityUtils.toString(entity)
                            val releaseInfo = JSONObject(body)
                            val lastVersion: String =
                                releaseInfo.getString(GITHUB_APP_VERSION)
                            val currentVersion: String = BuildConfig.VERSION_NAME
                            if (lastVersion != currentVersion) {
                                updateAvailable = true
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    } else {
                        // неверный ответ с сервера
                        Log.d(
                            "surprise",
                            "CheckUpdateWorker handleResponse: wrong update server answer"
                        )
                    }
                    null
                }
            // выполню запрос
            httpclient.execute(httpget, responseHandler)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                // по-любому закрою клиент
                httpclient.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return updateAvailable
    }

    @JvmStatic
    fun update() {
        val httpclient: CloseableHttpClient = HttpClients.createDefault()
        val httpget = HttpGet(GITHUB_RELEASES_URL)
        try {
            // кастомный обработчик ответов
            val responseHandler: ResponseHandler<String> =
                ResponseHandler<String> { response: HttpResponse ->
                    val status = response.statusLine.statusCode
                    if (status in 200..299) {
                        val entity: HttpEntity = response.entity
                        try {
                            val body: String = EntityUtils.toString(entity)
                            val releaseInfo = JSONObject(body)
                            val lastVersion: String =
                                releaseInfo.getString(GITHUB_APP_VERSION)
                            val currentVersion: String = BuildConfig.VERSION_NAME
                            if (lastVersion != currentVersion) {
                                // версии отличаются
                                // получу ссылку на скачивание
                                val releaseAssets: JSONObject =
                                    releaseInfo.getJSONArray("assets").getJSONObject(0)
                                val downloadLink: String =
                                    releaseAssets.getString(GITHUB_DOWNLOAD_LINK)
                                val downloadName: String =
                                    releaseAssets.getString(GITHUB_APP_NAME)
                                val downloadedApkFilePath =
                                    App.instance.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                                        .toString() + "/" + downloadName
                                val downloadedApkFile = File(downloadedApkFilePath)
                                val downloadUri = Uri.parse("file://$downloadedApkFile")
                                App.instance.updateDownloadUri = downloadUri
                                if (downloadedApkFile.exists()) {
                                    val deleteResult = downloadedApkFile.delete()
                                    if (!deleteResult) {
                                        Log.d(
                                            "surprise",
                                            "MakeUpdateWorker handleResponse: Не смог удалить предыдущий файл"
                                        )
                                    }
                                }
                                val request: DownloadManager.Request =
                                    DownloadManager.Request(Uri.parse(downloadLink))
                                request.setTitle(App.instance.getString(R.string.update_file_name))
                                request.setDestinationUri(downloadUri)
                                val manager: DownloadManager = App.instance.getSystemService(
                                    Context.DOWNLOAD_SERVICE
                                ) as DownloadManager
                                val startedDownloadId: Long = manager.enqueue(request)
                                // загрузка начата, отправлю идентификатор загрузки менеджеру
                                updateDownloadIdentification.postValue(startedDownloadId)
                                App.instance.downloadedApkFile = downloadedApkFile
                                // запущу сервис отслеживания окончания загрузки
                                App.instance.startService(
                                    Intent(
                                        App.instance,
                                        UpdateWaitService::class.java
                                    )
                                )
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    } else {
                        // неверный ответ с сервера
                        Log.d(
                            "surprise",
                            "CheckUpdateWorker handleResponse: wrong update server answer"
                        )
                    }
                    null
                }
            // выполню запрос
            httpclient.execute(httpget, responseHandler)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                // по-любому закрою клиент
                httpclient.close()
                // отправлю оповещение об отсутствии новой версии
                newVersion.postValue(false)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}