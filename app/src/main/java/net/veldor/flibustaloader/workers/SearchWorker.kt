package net.veldor.flibustaloader.workers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.MyWebViewClient
import net.veldor.flibustaloader.http.GlobalWebClient
import net.veldor.flibustaloader.http.TorWebClient
import net.veldor.flibustaloader.parsers.SearchResponseParser
import net.veldor.flibustaloader.selections.Author
import net.veldor.flibustaloader.selections.FoundedBook
import net.veldor.flibustaloader.selections.FoundedSequence
import net.veldor.flibustaloader.selections.Genre
import net.veldor.flibustaloader.ui.OPDSActivity
import net.veldor.flibustaloader.utils.Grammar.clearDirName
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.utils.URLHelper.getBaseOPDSUrl
import java.util.*

class SearchWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        var reservedSequenceName: String? = null
        // если есть имя серии- сохраню книги в папку с данным имененм
        if (sSequenceName != null) {
            reservedSequenceName = clearDirName(sSequenceName!!)
            sSequenceName = null
            if (reservedSequenceName.startsWith("Все книги серии ")) {
                reservedSequenceName = reservedSequenceName.substring(16)
            }
        }
        try {
            // оповещу о начале нового поиска
            OPDSActivity.sNewSearch.postValue(true)
            // сброшу указатель на следующую страницу
            OPDSActivity.sNextPage = null
            var pageCounter = 1
            // получу данные поиска
            val data = inputData
            var request = data.getString(REQUEST)
            if (request != null) {
                // сделаю первый запрос по-любому
                var answer = GlobalWebClient.request(request)
                if (answer == null || answer.isEmpty()) {
                    OPDSActivity.isLoadError.postValue(true)
                }
                if (!isStopped) {
                    var result: ArrayList<*>
                    // получу DOM
                    var parser = SearchResponseParser(answer!!)
                    val resultType = parser.type
                    result = parser.parseResponse(reservedSequenceName)!!
                    if (!isStopped) {
                        if (result.size == 0) {
                            // Ничего не найдено, уведомлю об этом
                            OPDSActivity.sNothingFound.postValue(true)
                        }
                        when (resultType) {
                            OPDSActivity.SEARCH_GENRE -> {
                                OPDSActivity.sLiveGenresFound.postValue(result as ArrayList<Genre>)
                                OPDSActivity.sSearchType = OPDSActivity.SEARCH_TYPE_GENRE
                                do {
                                    ++pageCounter
                                    // запрошу следующую страницу, если она есть
                                    request = if (answer != null) {
                                        App.instance.mLoadAllStatus.postValue("Загружаю страницу $pageCounter")
                                        getNextPageLink(answer)
                                    } else {
                                        null
                                    }
                                    if (request != null) {
                                        answer = GlobalWebClient.request(request)
                                        if (answer != null && !isStopped) {
                                            parser = SearchResponseParser(answer)
                                            result = parser.parseResponse(reservedSequenceName)!!
                                            if (result.size > 0 && !isStopped) {
                                                OPDSActivity.sLiveGenresFound.postValue(result as ArrayList<Genre>)
                                            }
                                        }
                                    }
                                } while (request != null && !isStopped)
                            }
                            OPDSActivity.SEARCH_SEQUENCE -> {
                                OPDSActivity.sSearchType = OPDSActivity.SEARCH_TYPE_SEQUENCES
                                OPDSActivity.sLiveSequencesFound.postValue(result as ArrayList<FoundedSequence>)
                                do {
                                    ++pageCounter
                                    // запрошу следующую страницу, если она есть
                                    request = if (answer != null) {
                                        App.instance.mLoadAllStatus.postValue("Загружаю страницу $pageCounter")
                                        getNextPageLink(answer)
                                    } else {
                                        null
                                    }
                                    if (request != null) {
                                        answer = GlobalWebClient.request(request)
                                        if (answer != null && !isStopped) {
                                            parser = SearchResponseParser(answer)
                                            result = parser.parseResponse(reservedSequenceName)!!
                                            if (result.size > 0 && !isStopped) {
                                                OPDSActivity.sLiveSequencesFound.postValue(result as ArrayList<FoundedSequence>)
                                            }
                                        }
                                    }
                                } while (request != null && !isStopped)
                            }
                            OPDSActivity.SEARCH_AUTHORS, OPDSActivity.SEARCH_NEW_AUTHORS -> {
                                OPDSActivity.sSearchType = OPDSActivity.SEARCH_TYPE_AUTHORS
                                Log.d("surprise", "SearchWorker doWork 110: load authors")
                                // сброшу предыдущие значения
                                Log.d("surprise", "SearchWorker doWork 65: post new authors")
                                OPDSActivity.sLiveAuthorsFound.postValue(result as ArrayList<Author>)
                                do {
                                    ++pageCounter
                                    Log.d("surprise", "SearchWorker doWork 110: load authors")
                                    // запрошу следующую страницу, если она есть
                                    request = if (answer != null) {
                                        App.instance.mLoadAllStatus.postValue("Загружаю страницу $pageCounter")
                                        getNextPageLink(answer)
                                    } else {
                                        null
                                    }
                                    if (request != null) {
                                        answer = GlobalWebClient.request(request)
                                        if (answer != null && !isStopped) {
                                            parser = SearchResponseParser(answer)
                                            result = parser.parseResponse(reservedSequenceName)!!
                                            if (result.size > 0 && !isStopped) {
                                                OPDSActivity.sLiveAuthorsFound.postValue(result as ArrayList<Author>)
                                            }
                                        }
                                    }
                                } while (request != null && !isStopped)
                            }
                            OPDSActivity.SEARCH_BOOKS -> {
                                OPDSActivity.sSearchType = OPDSActivity.SEARCH_TYPE_BOOKS
                                // сброшу предыдущие значения
                                OPDSActivity.sLiveBooksFound.postValue(null)
                                OPDSActivity.sLiveBooksFound.postValue(result as ArrayList<FoundedBook>)
                                // если выбрана загрузка сразу всего- гружу все результаты
                                if (PreferencesHandler.instance.isDownloadAll) {
                                    do {
                                        ++pageCounter
                                        // запрошу следующую страницу, если она есть
                                        if (answer != null) {
                                            request = getNextPageLink(answer)
                                            App.instance.mLoadAllStatus.postValue("Загружаю страницу $pageCounter")
                                        } else {
                                            request = null
                                        }
                                        if (request != null) {
                                            answer = GlobalWebClient.request(request)
                                            if (answer != null && !isStopped) {
                                                parser = SearchResponseParser(answer)
                                                result = parser.parseResponse(reservedSequenceName)!!
                                                if (result.size > 0 && !isStopped) {
                                                    OPDSActivity.sLiveBooksFound.postValue(result as ArrayList<FoundedBook>)
                                                }
                                            }
                                        }
                                    } while (request != null && !isStopped)
                                } else {
                                    OPDSActivity.sNextPage = getNextPageLink(answer)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val finishLoadingIntent = Intent(MyWebViewClient.TOR_CONNECT_ERROR_ACTION)
            finishLoadingIntent.putExtra(
                TorWebClient.ERROR_DETAILS,
                " Не удалось обработать полученные данные. Проблемы с соединением или сервером Флибусты"
            )
            App.instance.sendBroadcast(finishLoadingIntent)
            return Result.failure()
        }
        OPDSActivity.sNewSearch.postValue(false)
        return Result.success()
    }

    private fun getNextPageLink(answer: String?): String? {
        var s = "rel=\"next\""
        if (answer!!.contains(s)) {
            val finishValue = answer.indexOf(s) - 2
            s = answer.substring(0, finishValue)
            return getBaseOPDSUrl() + s.substring(s.lastIndexOf("\"") + 1).replace("&amp;", "&")
        }
        return null
    }

    companion object {
        const val REQUEST = "request"
        const val WORK_TAG = "search worker"
        @JvmField
        var sSequenceName: String? = null
    }
}