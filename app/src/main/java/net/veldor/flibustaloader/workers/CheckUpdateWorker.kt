package net.veldor.flibustaloader.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import cz.msebera.android.httpclient.HttpEntity
import cz.msebera.android.httpclient.HttpResponse
import cz.msebera.android.httpclient.client.ResponseHandler
import cz.msebera.android.httpclient.client.methods.HttpGet
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient
import cz.msebera.android.httpclient.impl.client.HttpClients
import cz.msebera.android.httpclient.util.EntityUtils
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.BuildConfig
import net.veldor.flibustaloader.updater.Updater
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class CheckUpdateWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        if (App.isTestVersion) {
            return Result.success()
        }
        val httpclient: CloseableHttpClient = HttpClients.createDefault()
        val httpget = HttpGet(Updater.GITHUB_RELEASES_URL)
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
                                releaseInfo.getString(Updater.GITHUB_APP_VERSION)
                            val currentVersion: String = BuildConfig.VERSION_NAME
                            if (lastVersion != currentVersion) {
                                // версии отличаются
                                Updater.newVersion.postValue(true)
                                Log.d(
                                    "surprise",
                                    "CheckUpdateWorker handleResponse 57: have new version"
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
                Updater.newVersion.postValue(false)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return Result.success()
    }
}