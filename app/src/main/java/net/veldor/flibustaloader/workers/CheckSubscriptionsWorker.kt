package net.veldor.flibustaloader.workers

import net.veldor.flibustaloader.utils.SubscribesHandler.getAllSubscribes
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.http.TorWebClient
import net.veldor.flibustaloader.selections.FoundedBook
import cz.msebera.android.httpclient.util.EntityUtils
import android.content.Context
import net.veldor.flibustaloader.notificatons.NotificationHandler
import net.veldor.flibustaloader.ui.BaseActivity
import kotlin.Throws
import net.veldor.flibustaloader.http.ExternalVpnVewClient
import net.veldor.flibustaloader.ecxeptions.ConnectionLostException
import net.veldor.flibustaloader.selections.SubscriptionItem
import net.veldor.flibustaloader.view_models.SubscriptionsViewModel
import net.veldor.flibustaloader.parsers.SubscriptionsParser
import org.xmlpull.v1.XmlPullParserFactory
import android.content.res.XmlResourceParser
import android.util.Log
import androidx.work.*
import net.veldor.flibustaloader.utils.MyFileReader.SUBSCRIPTIONS_FILE
import net.veldor.flibustaloader.utils.PreferencesHandler
import java.io.*
import java.lang.Exception
import java.util.*

class CheckSubscriptionsWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    private var mNotifier: NotificationHandler? = null
    private var mNextPageLink: String? = null
    private var mSubscribes: ArrayList<SubscriptionItem> = arrayListOf()
    private val result = ArrayList<FoundedBook>()
    private var mLastCheckedBookId: String? = null
    private var mCheckFor: String? = null
    private var mStopCheck = false
    override fun doWork(): Result {
        // список подписок
        mSubscribes = getAllSubscribes()
        if (mSubscribes.size > 0) {
            // проверю, полная ли проверка или частичная
            val data = inputData
            val fullCheck = data.getBoolean(FULL_CHECK, false)
            if (!fullCheck) {
                // получу последнюю проверенную книгу
                mCheckFor = PreferencesHandler.instance.lastCheckedBookId
            }

            // буду загружать страницы с новинками
            var webClient: TorWebClient
            mNextPageLink = "/opds/new/0/new"
            mNotifier = NotificationHandler.instance
            // помечу рабочего важным
            val info = createForegroundInfo()
            setForegroundAsync(info)
            var answer: String? = null
            do {
                if (!isStopped) {
                    // пока буду загружать все страницы, которые есть
                    if (PreferencesHandler.instance.isExternalVpn) {
                        val response = ExternalVpnVewClient.rawRequest(PreferencesHandler.BASE_URL + mNextPageLink)
                        if (response != null) {
                            try {
                                answer = EntityUtils.toString(response.entity)
                            } catch (e: IOException) {
                                Log.d(
                                    "surprise",
                                    "CheckSubscriptionsWorker doWork error when receive subscription"
                                )
                                e.printStackTrace()
                            }
                        }
                    } else {
                        // создам новый экземпляр веб-клиента
                        webClient = try {
                            TorWebClient()
                        } catch (e: ConnectionLostException) {
                            e.printStackTrace()
                            return Result.success()
                        }
                        if (!isStopped) {
                            answer = webClient.request(App.BASE_URL + mNextPageLink)
                        }
                    }
                    mNextPageLink = null
                    if (answer != null) {
                        try {
                            handleAnswer(answer)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } while (mNextPageLink != null && !mStopCheck)
        }
        if (result.size > 0) {
            // найдены книги
            NotificationHandler.instance.sendFoundSubscribesNotification()
            serializeResult()
        } else {
            NotificationHandler.instance.sendNotFoundSubscribesNotification()
        }
        if (mLastCheckedBookId != null) {
            // сохраню последнюю проверенную книгу
            PreferencesHandler.instance.lastCheckedBookId = mLastCheckedBookId!!
        }
        // оповещу об окончании процесса загрузки подписок
        SubscriptionsViewModel.sSubscriptionsChecked.postValue(true)
        return Result.success()
    }

    private fun serializeResult() {
        try {
            Log.d(
                "surprise",
                "CheckSubscriptionsWorker serializeResult: serialize " + result.size + " books"
            )
            val autocompleteFile = File(App.instance.filesDir, SUBSCRIPTIONS_FILE)
            val fos = FileOutputStream(autocompleteFile)
            val oos = ObjectOutputStream(fos)
            oos.writeObject(result)
            oos.close()
            fos.close()
            // оповещу об изменении списка
            BaseActivity.sLiveFoundedSubscriptionsCount.postValue(true)
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }
    }

    @Throws(Exception::class)
    private fun handleAnswer(answer: String) {

        // найду id последней книги на странице
        val lastId = answer.substring(
            answer.lastIndexOf("tag:book:"),
            answer.indexOf("<", answer.lastIndexOf("tag:book:"))
        )
        if (mCheckFor != null && mCheckFor!! > lastId) {
            mStopCheck = true
        }
        mNextPageLink = null
        var foundKeyWords = false
        // проверю, есть ли в тексте упоминания слов, входящих в подписки. Если их нет- проверяю только ссылку на следующую страницу
        for (si in mSubscribes) {
            if (answer.lowercase(Locale.getDefault()).contains(si.name!!.lowercase(Locale.getDefault()))) {
                foundKeyWords = true
                Log.d(
                    "surprise",
                    "CheckSubscriptionsWorker handleAnswer 163: found subscription results"
                )
                break
            }
        }

        // если найдены слова, входящие в подписки- проверю страницу
        if (foundKeyWords) {
            SubscriptionsParser.handleSearchResults(result, answer, mSubscribes)
        }
        var value: String
        var value1: String
        var foundFirstBook = false
        var nextPageLinkFound = false
        // прогоню строку через парсер
        val factory = XmlPullParserFactory
            .newInstance()
        factory.isNamespaceAware = true
        val xrp = factory.newPullParser()
        xrp.setInput(StringReader(answer))
        xrp.next()
        var eventType = xrp.eventType
        while (eventType != XmlResourceParser.END_DOCUMENT) {
            if (eventType == XmlResourceParser.START_TAG) {
                value = xrp.name
                if (value != null) {
                    if (!nextPageLinkFound && value == "link") {
                        // проверю, возможно, это ссылка на следующую страницу
                        value1 = xrp.getAttributeValue(null, "rel")
                        if (value1 != null && value1 == "next") {
                            // найдена ссылка на следующую страницу
                            mNextPageLink = xrp.getAttributeValue(null, "href")
                            nextPageLinkFound = true
                        }
                    }
                    if (mLastCheckedBookId == null && value == "entry") {
                        // найдена книга, если это первая проверенная- запишу её id
                        foundFirstBook = true
                    } else if (foundFirstBook && mLastCheckedBookId == null && value == "id") {
                        mLastCheckedBookId = xrp.nextText()
                        return
                    }
                }
            }
            eventType = xrp.next()
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = mNotifier!!.checkSubscribesNotification
        return ForegroundInfo(NotificationHandler.CHECK_SUBSCRIBES_WORKER_NOTIFICATION, notification)
    }

    companion object {
        const val CHECK_SUBSCRIBES = "check subscribes"
        const val FULL_CHECK = "full check"
        const val PERIODIC_CHECK_TAG = "periodic check subscriptions"
    }
}