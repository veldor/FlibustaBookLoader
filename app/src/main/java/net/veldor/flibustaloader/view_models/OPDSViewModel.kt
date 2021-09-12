package net.veldor.flibustaloader.view_models

import android.app.Application
import android.util.Log
import android.util.SparseBooleanArray
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.MyWebView
import net.veldor.flibustaloader.database.entity.ReadedBooks
import net.veldor.flibustaloader.http.GlobalWebClient
import net.veldor.flibustaloader.interfaces.MyViewModelInterface
import net.veldor.flibustaloader.parsers.TestParser
import net.veldor.flibustaloader.selections.DownloadLink
import net.veldor.flibustaloader.selections.FoundedBook
import net.veldor.flibustaloader.selections.SearchResult
import net.veldor.flibustaloader.updater.Updater.checkUpdate
import net.veldor.flibustaloader.updater.Updater.update
import net.veldor.flibustaloader.utils.BookSharer.shareLink
import net.veldor.flibustaloader.utils.MyFileReader.clearAutocomplete
import net.veldor.flibustaloader.utils.MyFileReader.getSearchAutocomplete
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.utils.URLHelper
import net.veldor.flibustaloader.utils.XMLHandler.getSearchAutocomplete
import net.veldor.flibustaloader.workers.AddBooksToDownloadQueueWorker
import net.veldor.flibustaloader.workers.AddBooksToDownloadQueueWorker.Companion.addLink
import java.util.*

class OPDSViewModel(application: Application) : GlobalViewModel(application), MyViewModelInterface {

    // ошибка загрузки
    private val _isLoadError = MutableLiveData<Boolean>()
    val isLoadError: LiveData<Boolean> = _isLoadError

    // статус запроса
    private val _requestStatus = MutableLiveData<String>()
    val requestStatus: LiveData<String> = _requestStatus

    // результаты поиска
    private val _searchResults = MutableLiveData<SearchResult>()
    val searchResults: LiveData<SearchResult> = _searchResults

    var searchCancelled: Boolean = false
    private val mClickedItemsStack = Stack<Int>()
    val viewMode: Int
        get() = PreferencesHandler.instance.viewMode

    fun switchViewMode(type: Int) {
        PreferencesHandler.instance.viewMode = type
    }

    fun startCheckUpdate(): LiveData<Boolean> {
        return checkUpdate()
    }

    fun initializeUpdate() {
        update()
    }

    fun switchNightMode() {
        PreferencesHandler.instance.nightMode = !PreferencesHandler.instance.nightMode
    }

    val nightModeEnabled: Boolean
        get() = PreferencesHandler.instance.nightMode
    val searchAutocomplete: ArrayList<String>
        get() {
            val content = getSearchAutocomplete()
            return getSearchAutocomplete(content)
        }
    val randomBookUrl: String
        get() {
            val random = Random()
            return URLHelper.getFlibustaUrl() +  App.BASE_BOOK_URL + random.nextInt(App.MAX_BOOK_NUMBER)
        }

    fun shareLink(mWebView: MyWebView) {
        shareLink(mWebView.url)
    }

    fun setBookRead(book: FoundedBook) {
        // запущу рабочего, который отметит книгу как прочитанную
        val readedBook = ReadedBooks()
        readedBook.bookId = book.id!!
        App.instance.mDatabase.readBooksDao().insert(readedBook)
    }

    fun clearHistory() {
        clearAutocomplete()
    }

    fun downloadSelected(ids: SparseBooleanArray?): LiveData<WorkInfo> {
        App.instance.mDownloadSelectedBooks = ids
        val downloadSelected = OneTimeWorkRequest.Builder(
            AddBooksToDownloadQueueWorker::class.java
        ).addTag(ADD_TO_DOWNLOAD_QUEUE_ACTION).build()
        WorkManager.getInstance(App.instance).enqueueUniqueWork(
            ADD_TO_DOWNLOAD_QUEUE_ACTION,
            ExistingWorkPolicy.REPLACE,
            downloadSelected
        )
        return WorkManager.getInstance(App.instance)
            .getWorkInfoByIdLiveData(downloadSelected.id)
    }

    fun downloadAll(unloaded: Boolean): LiveData<WorkInfo> {
        App.instance.mDownloadSelectedBooks = null
        App.instance.mDownloadUnloaded = unloaded
        val downloadSelected = OneTimeWorkRequest.Builder(
            AddBooksToDownloadQueueWorker::class.java
        ).addTag(ADD_TO_DOWNLOAD_QUEUE_ACTION).build()
        WorkManager.getInstance(App.instance).enqueueUniqueWork(
            ADD_TO_DOWNLOAD_QUEUE_ACTION,
            ExistingWorkPolicy.REPLACE,
            downloadSelected
        )
        return WorkManager.getInstance(App.instance)
            .getWorkInfoByIdLiveData(downloadSelected.id)
    }

    fun initiateMassDownload() {
        App.instance.initializeDownload()
    }

    fun cancelMassDownload() {
        WorkManager.getInstance(App.instance).cancelAllWorkByTag(MULTIPLY_DOWNLOAD)
    }

    fun checkDownloadQueue(): Boolean {
        return App.instance.checkDownloadQueue()
    }

    fun addToDownloadQueue(downloadLink: DownloadLink?) {
        addLink(downloadLink!!)
        if (PreferencesHandler.instance.isDownloadAutostart) {
            App.instance.initializeDownload()
        }
    }

    fun request(s: String) {
        // запрошу данные
        viewModelScope.launch(Dispatchers.IO) {
            _requestStatus.postValue("Запрашиваю страницу")
            val answer = GlobalWebClient.request(URLHelper.getBaseUrl() + s)
            if (answer == null || answer.isEmpty()) {
                _isLoadError.postValue(true)
                return@launch
            }
            _requestStatus.postValue("Данные загружены, обработка")
            // попробую с помощью нового парсера разобрать ответ
            val parser = TestParser(answer)
            val results = parser.parse()
            _requestStatus.postValue("Данные обработаны")
            val searchResult = SearchResult()
            searchResult.appended = false
            searchResult.size = results.size
            searchResult.results = results
            searchResult.nextPageLink = parser.nextPageLink
            _searchResults.postValue(searchResult)

        }
//        val inputData = Data.Builder()
//            .putString(SearchWorker.REQUEST, s)
//            .build()
//        // запущу рабочего, который выполнит запрос
//        val searchWorkRequest =
//            OneTimeWorkRequest.Builder(SearchWorker::class.java).addTag(SearchWorker.WORK_TAG)
//                .setInputData(inputData).build()
//        WorkManager.getInstance(App.instance)
//            .enqueueUniqueWork(SearchWorker.WORK_TAG, ExistingWorkPolicy.REPLACE, searchWorkRequest)
//        return searchWorkRequest.id
    }

    fun saveClickedIndex(sClickedItemIndex: Int) {
        mClickedItemsStack.push(sClickedItemIndex)
        Log.d(
            "surprise",
            "MainViewModel saveClickedIndex 140: to stack added " + sClickedItemIndex + ", stack size is " + mClickedItemsStack.size
        )
    }

    val lastClickedElement: Int
        get() = if (!mClickedItemsStack.isEmpty()) {
            mClickedItemsStack.pop()
        } else -1
    val height: Int
        get() {
            val displayMetrics = App.instance.resources.displayMetrics
            val dpHeight = (displayMetrics.heightPixels / displayMetrics.density).toInt()
            return dpHeight - 50
        }

    companion object {
        private const val ADD_TO_DOWNLOAD_QUEUE_ACTION = "add to download queue"
        const val MULTIPLY_DOWNLOAD = "multiply download"
    }
}