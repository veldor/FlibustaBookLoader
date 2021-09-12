package net.veldor.flibustaloader.workers

import net.veldor.flibustaloader.utils.XMLParser.handleSearchResults
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.http.TorWebClient
import net.veldor.flibustaloader.ui.OPDSActivity
import net.veldor.flibustaloader.selections.FoundedBook
import android.content.Context
import net.veldor.flibustaloader.ecxeptions.ConnectionLostException
import net.veldor.flibustaloader.selections.FoundedItem
import android.util.Log
import androidx.work.*
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.utils.URLHelper
import java.util.*

class LoadSubscriptionsWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        val subscribes = App.instance.booksSubscribe.getSubscribes()
        val authorSubscribes = App.instance.authorsSubscribe.getSubscribes()
        val sequenceSubscribes = App.instance.sequencesSubscribe.getSubscribes()
        if (subscribes.size > 0 || authorSubscribes.size > 0 || sequenceSubscribes.size > 0) {
            // получу последний проверенный ID
            val lastCheckedId = PreferencesHandler.instance.lastCheckedBookId
            var firstCheckedId: String? = null
            Log.d(
                "surprise",
                "LoadSubscriptionsWorker doWork last previous checked id is $lastCheckedId"
            )
            val booksResult = ArrayList<FoundedBook>()
            // проверю, запустился ли TOR
            var tor = App.instance.mLoadedTor.value
            while (tor == null) {
                try {
                    Thread.sleep(1000)
                    Log.d("surprise", "CheckSubscriptionsWorker doWork wait tor")
                } catch (e: InterruptedException) {
                    Log.d("surprise", "CheckSubscriptionsWorker doWork thread interrupted")
                    e.printStackTrace()
                }
                tor = App.instance.mLoadedTor.value
            }
            // теперь подожду, пока TOR дозагрузится
            while (!tor.isBootstrapped) {
                try {
                    Thread.sleep(1000)
                    Log.d("surprise", "CheckSubscriptionsWorker doWork wait tor boostrap")
                } catch (e: InterruptedException) {
                    Log.d("surprise", "CheckSubscriptionsWorker doWork thread interrupted")
                    e.printStackTrace()
                }
            }
            // получу список подписок
            App.sSearchType = OPDSActivity.SEARCH_BOOKS
            // получу список книг
            // создам новый экземпляр веб-клиента
            var webClient: TorWebClient?
            webClient = try {
                TorWebClient()
            } catch (e: ConnectionLostException) {
                e.printStackTrace()
                return Result.success()
            }
            val result = ArrayList<FoundedItem>()
            var answer = webClient!!.request(URLHelper.getFlibustaUrl() + "/opds/new/0/new")
            // сразу же обработаю результат
            if (answer != null && answer.isNotEmpty()) {
                handleSearchResults(result, answer)
                // найду id последней добавленной книги
                if (result.size > 0) {
                    val book = result[0]
                    firstCheckedId = (book as FoundedBook).id
                    Log.d("surprise", "LoadSubscriptionsWorker doWork last id is $firstCheckedId")
                }
                while (sNextPage != null) {
                    Log.d("surprise", "LoadSubscriptionsWorker doWork load next results page")
                    webClient = try {
                        TorWebClient()
                    } catch (e: ConnectionLostException) {
                        e.printStackTrace()
                        return Result.success()
                    }
                    answer = webClient!!.request(URLHelper.getFlibustaUrl() + sNextPage)
                    handleSearchResults(result, answer!!)
                }
            }
            // теперь каждую сущность нужно проверить на соответствие поисковому шаблону
            if (result.size > 0) {
                var realBook: FoundedBook
                Log.d("surprise", "CheckSubscriptionsWorker doWork найдены книги, проверяю")
                for (book in result) {
                    realBook = book as FoundedBook
                    for (needle in subscribes) {
                        if (realBook.name!!.lowercase(Locale.ROOT).contains(needle.name!!.lowercase(
                                Locale.getDefault()
                            ))) {
                            // найдено совпадение
                            // добавлю книгу в список найденных
                            booksResult.add(realBook)
                        }
                    }
                    for (needle in authorSubscribes) {
                        // если есть автор
                        if (realBook.author != null && realBook.author!!.isNotEmpty()) {
                            if (realBook.author!!.lowercase(Locale.getDefault())
                                    .contains(needle.name!!.lowercase(Locale.getDefault()))
                            ) {
                                // найдено совпадение
                                // добавлю книгу в список найденных
                                booksResult.add(realBook)
                            }
                        }
                    }
                    for (needle in sequenceSubscribes) {
                        // если есть автор
                        if (realBook.sequenceComplex != null && realBook.sequenceComplex!!.isNotEmpty()) {
                            if (realBook.sequenceComplex!!.lowercase(Locale.getDefault())
                                    .contains(needle.name!!.lowercase(Locale.getDefault()))
                            ) {
                                // найдено совпадение
                                // добавлю книгу в список найденных
                                booksResult.add(realBook)
                            }
                        }
                    }
                }
            } else {
                Log.d("surprise", "CheckSubscriptionsWorker doWork книги не найдены")
            }
            if (booksResult.size > 0) {
                // отправлю результат
                App.instance.mSubscribeResults.postValue(booksResult)
                Log.d("surprise", "LoadSubscriptionsWorker doWork список книг отправлен")
            }
            if (firstCheckedId != null) {
                PreferencesHandler.instance.lastCheckedBookId = firstCheckedId
            }
        } else {
            Log.d("surprise", "CheckSubscriptionsWorker doWork подписки не найдены!")
        }

        // сохраню первый отсканированный id как последний проверенный
        return Result.success()
    }

    companion object {
        var sNextPage: String? = null
    }
}