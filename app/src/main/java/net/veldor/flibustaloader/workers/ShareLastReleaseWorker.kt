package net.veldor.flibustaloader.workers

import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import android.content.Intent
import cz.msebera.android.httpclient.impl.client.HttpClients
import cz.msebera.android.httpclient.client.methods.HttpGet
import net.veldor.flibustaloader.updater.Updater
import cz.msebera.android.httpclient.client.ResponseHandler
import cz.msebera.android.httpclient.util.EntityUtils
import org.json.JSONObject
import android.content.Context
import org.json.JSONException
import android.util.Log
import androidx.work.*
import cz.msebera.android.httpclient.HttpResponse
import java.io.IOException
import java.util.*

class ShareLastReleaseWorker(private val mContext: Context, workerParams: WorkerParameters) :
    Worker(
        mContext, workerParams
    ) {
    override fun doWork(): Result {
        val httpclient = HttpClients.createDefault()
        val httpget = HttpGet(Updater.GITHUB_RELEASES_URL)
        try {
            // кастомный обработчик ответов
            val responseHandler = ResponseHandler<String?> { response: HttpResponse ->
                val status = response.statusLine.statusCode
                if (status in 200..299) {
                    val entity = response.entity
                    try {
                        val body = EntityUtils.toString(entity)
                        val releaseInfo = JSONObject(body)
                        // версии отличаются
                        // получу ссылку на скачивание
                        val releaseAssets = releaseInfo.getJSONArray("assets").getJSONObject(0)
                        val downloadLink = releaseAssets.getString(Updater.GITHUB_DOWNLOAD_LINK)
                        val textToShare = String.format(
                            Locale.ENGLISH,
                            App.instance.getString(R.string.latest_release_here_template),
                            downloadLink
                        )
                        val intent = Intent(Intent.ACTION_SEND)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.type = "text/plain"
                        intent.putExtra(
                            Intent.EXTRA_SUBJECT,
                            App.instance.getString(R.string.share_app_link_title)
                        )
                        intent.putExtra(Intent.EXTRA_TEXT, textToShare)
                        val chooser = Intent.createChooser(
                            intent,
                            App.instance.getString(R.string.share_app_message)
                        )
                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        mContext.startActivity(chooser)
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