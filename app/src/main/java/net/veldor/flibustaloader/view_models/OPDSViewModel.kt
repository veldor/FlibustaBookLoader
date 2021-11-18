package net.veldor.flibustaloader.view_models

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration.*
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.MyWebView
import net.veldor.flibustaloader.database.entity.Bookmark
import net.veldor.flibustaloader.database.entity.DownloadedBooks
import net.veldor.flibustaloader.database.entity.ReadedBooks
import net.veldor.flibustaloader.delegates.BooksAddedToQueueDelegate
import net.veldor.flibustaloader.delegates.ResultsReceivedDelegate
import net.veldor.flibustaloader.handlers.DownloadLinkHandler
import net.veldor.flibustaloader.handlers.PicHandler
import net.veldor.flibustaloader.http.UniversalWebClient
import net.veldor.flibustaloader.interfaces.MyViewModelInterface
import net.veldor.flibustaloader.parsers.TestParser
import net.veldor.flibustaloader.parsers.TestParser.Companion.TYPE_BOOK
import net.veldor.flibustaloader.selections.DownloadLink
import net.veldor.flibustaloader.selections.FoundedEntity
import net.veldor.flibustaloader.selections.SearchResult
import net.veldor.flibustaloader.updater.Updater
import net.veldor.flibustaloader.utils.*
import net.veldor.flibustaloader.utils.BookSharer.shareLink
import net.veldor.flibustaloader.utils.MyFileReader.clearAutocomplete
import net.veldor.flibustaloader.utils.MyFileReader.getSearchAutocomplete
import net.veldor.flibustaloader.utils.XMLHandler.getSearchAutocomplete
import java.io.File
import java.util.*

open class OPDSViewModel(application: Application) : GlobalViewModel(application),
    MyViewModelInterface {

    private var currentWork: Job? = null

    private var currentPageUrl: String? = null

    val viewMode: Int
        get() = PreferencesHandler.instance.viewMode

    // ошибка загрузки
    private val _isLoadError = MutableLiveData<Boolean>()
    val isLoadError: LiveData<Boolean> = _isLoadError

    // наличие обновления ПО
    private val _isUpdateAvailable = MutableLiveData<Boolean>()
    val isUpdateAvailable: LiveData<Boolean> = _isUpdateAvailable

    fun switchViewMode(type: Int) {
        PreferencesHandler.instance.viewMode = type
    }

    fun checkUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            _isUpdateAvailable.postValue(Updater.checkUpdate())
        }
    }

    fun initializeUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            Updater.update()
        }
    }

    fun switchNightMode() {
        PreferencesHandler.instance.nightMode = !PreferencesHandler.instance.nightMode
    }

    fun isDarkTheme(activity: Activity): Boolean {
        when (activity.resources.configuration.uiMode and UI_MODE_NIGHT_MASK) {
            UI_MODE_NIGHT_NO -> {
                return PreferencesHandler.instance.nightMode
            } // Night mode is not active, we're using the light theme
            UI_MODE_NIGHT_YES -> {
                return true
            } // Night mode is active, we're using dark theme
        }
        return PreferencesHandler.instance.nightMode
    }

    val searchAutocomplete: ArrayList<String>
        get() {
            val content = getSearchAutocomplete()
            return getSearchAutocomplete(content)
        }
    val randomBookUrl: String
        get() {
            val random = Random()
            return URLHelper.getFlibustaUrl() + BASE_BOOK_URL + random.nextInt(MAX_BOOK_NUMBER)
        }

    fun shareLink(mWebView: MyWebView) {
        shareLink(mWebView.url)
    }

    fun setBookRead(book: FoundedEntity) {
        val readedBook = ReadedBooks()
        readedBook.bookId = book.id!!
        App.instance.mDatabase.readBooksDao().insert(readedBook)
    }

    fun setBookDownloaded(book: FoundedEntity) {
        val downloadedBook = DownloadedBooks()
        downloadedBook.bookId = book.id!!
        App.instance.mDatabase.downloadedBooksDao().insert(downloadedBook)
    }

    fun clearHistory() {
        clearAutocomplete()
    }

    fun downloadAll(
        books: ArrayList<FoundedEntity>,
        format: String,
        onlyUnloaded: Boolean,
        strictFormat: Boolean,
        delegate: BooksAddedToQueueDelegate,
        userSequenceName: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            var counter = 0
            val longFormat = MimeTypes.getFullMime(format)
            // проверю, нужно ли загружать книги только в выбранном формате
            if (books.isNotEmpty()) {
                Log.d("surprise", "downloadAll: books for add: ${books.size}")
                books.forEach { foundedEntity ->
                    var linkFound = false
                    // ищу книгу в выбранном формате
                    if (foundedEntity.downloadLinks.isNotEmpty()) {
                        foundedEntity.downloadLinks.forEach {
                            if (it.mime == longFormat) {
                                // найдена ссылка на формат
                                if (onlyUnloaded) {
                                    if (!foundedEntity.downloaded) {
                                        if (userSequenceName != null) {
                                            it.sequenceDirName = userSequenceName
                                            it.reservedSequenceName = userSequenceName
                                        }
                                        DownloadLinkHandler().addLink(it)
                                        counter++
                                        linkFound = true
                                    }
                                } else {
                                    if (userSequenceName != null) {
                                        it.sequenceDirName = userSequenceName
                                        it.reservedSequenceName = userSequenceName
                                    }
                                    DownloadLinkHandler().addLink(it)
                                    counter++
                                    linkFound = true
                                }
                            }
                        }
                        if (!linkFound && !strictFormat) {
                            if (userSequenceName != null) {
                                foundedEntity.downloadLinks[0].sequenceDirName = userSequenceName
                                foundedEntity.downloadLinks[0].reservedSequenceName =
                                    userSequenceName
                            }
                            DownloadLinkHandler().addLink(foundedEntity.downloadLinks[0])
                            counter++
                        }
                    } else {
                        Log.d(
                            "surprise",
                            "downloadAll: ============== NO LINKS ================= ${foundedEntity.name}"
                        )
                    }
                }
                App.instance.requestDownloadBooksStart()
                delegate.booksAdded(counter)
            }
        }
    }


    fun addToDownloadQueue(downloadLink: DownloadLink, delegate: BooksAddedToQueueDelegate) {
        viewModelScope.launch(Dispatchers.IO) {
            DownloadLinkHandler().addLink(downloadLink)
            App.instance.requestDownloadBooksStart()
            delegate.booksAdded(1)
        }
    }

    fun request(
        s: String,
        append: Boolean,
        addToHistory: Boolean,
        clickedElementIndex: Int,
        delegate: ResultsReceivedDelegate
    ) {
        currentRequestState.postValue("Формирую запрос")
        Log.d("surprise", "request: request $s")
        if (currentWork != null) {
            currentWork!!.cancel()
        }
        if (addToHistory && currentPageUrl != null) {
            Log.d("surprise", "request: add to history $currentPageUrl")
            History.instance!!.addToHistory(currentPageUrl!!)
            History.instance!!.addToClickHistory(clickedElementIndex)
            Log.d("surprise", "request: save click on $clickedElementIndex")
        }

        if (!append) {
            currentPageUrl = s
        }
        // запрошу данные
        currentWork = viewModelScope.launch(Dispatchers.IO) {
            // clear previously loaded images
            FilesHandler.clearCache()

            var previousSearchRequestResult: SearchResult? = null
            previousSearchRequestResult =
                makeRequest(s, append, if (addToHistory) -1 else clickedElementIndex)
            previousSearchRequestResult?.let { delegate.resultsReceived(it) }
            while (true) {
                if (isActive) {
                    if (previousSearchRequestResult?.nextPageLink != null) {
                        Log.d("surprise", "request: found next page")
                        // если найдены книги- проверю, нужно ли загружать все результаты сразу
                        if ((!PreferencesHandler.instance.opdsPagedResultsLoad && previousSearchRequestResult.type == TYPE_BOOK) ||
                            previousSearchRequestResult.type != TYPE_BOOK
                        ) {
                            previousSearchRequestResult = makeRequest(
                                previousSearchRequestResult.nextPageLink!!,
                                true,
                                if (addToHistory) -1 else clickedElementIndex
                            ) ?: break
                            previousSearchRequestResult.let { delegate.resultsReceived(it) }
                        } else {
                            Log.d("surprise", "request: can't load next")
                            break
                        }
                    } else {
                        Log.d("surprise", "request: no next page")
                        break
                    }
                } else {
                    Log.d("surprise", "request: work not active")
                    break
                }
            }
            return@launch
        }
    }

    private fun makeRequest(s: String, append: Boolean, lastClicked: Int): SearchResult? {
        try {
            val response = UniversalWebClient().rawRequest(s)
            currentRequestState.postValue("Получен ответ")
            if (response != null) {
                val answer = UniversalWebClient().responseToString(response)
                if (answer != null) {
                    // попробую с помощью нового парсера разобрать ответ
                    val parser = TestParser(answer)
                    val results = parser.parse()
                    currentRequestState.postValue("Ответ обработан")
                    val searchResult = SearchResult()
                    searchResult.appended = append
                    searchResult.size = results.size
                    if (results.isNotEmpty()) {
                        searchResult.type = results[0].type
                    }
                    searchResult.results = results
                    searchResult.filteredList = parser.filteredList
                    searchResult.nextPageLink = parser.nextPageLink
                    searchResult.filtered = parser.filtered
                    if (lastClicked >= 0) {
                        searchResult.clickedElementIndex = lastClicked
                        searchResult.isBackSearch = true
                    }
                    currentRequestState.postValue("Отображаю данные")
                    return searchResult
                }
            }
        } catch (_: Exception) {
        }
        _isLoadError.postValue(true)
        return null
    }

    fun loadImage(imageContainer: ImageView?, item: FoundedEntity) {
        if (imageContainer != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    PicHandler().downloadPic(item)
                    if (item.cover != null && item.cover!!.isFile && item.cover!!.exists() && item.cover!!.canRead()) {
                        imageContainer.setImageBitmap(BitmapFactory.decodeFile(item.cover!!.path))
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    fun saveBookmark(bookmarkName: String) {
        if (bookmarkName.isNotEmpty()) {
            if (currentPageUrl != null) {
                // сохраню закладку
                val bookmark = Bookmark()
                bookmark.link = currentPageUrl!!
                bookmark.name = bookmarkName
                App.instance.mDatabase.bookmarksDao().insert(bookmark)
                Toast.makeText(App.instance, "Закладка добавлена", Toast.LENGTH_LONG)
                    .show()
            } else {
                Toast.makeText(
                    App.instance,
                    "Нужно заполнить имя закладки и произвести поиск",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun getCurrentPage(): String? {
        return currentPageUrl
    }

    fun saveLoaded(list: ArrayList<FoundedEntity>) {
        Log.d("surprise", "saveLoaded: save ${list.size} elements")
        savedList = list
    }

    fun getPreviouslyLoaded(): ArrayList<FoundedEntity> {
        Log.d("surprise", "getPreviouslyLoaded: load ${savedList.size} elements")
        return savedList
    }

    fun cancelLoad() {
        if (currentWork != null) {
            currentWork!!.cancel()
        }
    }

    fun loadInProgress(): Boolean {
        return currentWork?.isActive == true
    }

    fun saveScrolledPosition(s: Int) {
        lastScrolled = s
    }

    fun getScrolledPosition(): Int {
        return lastScrolled
    }

    val height: Int
        get() {
            val displayMetrics = App.instance.resources.displayMetrics
            val dpHeight = (displayMetrics.heightPixels / displayMetrics.density).toInt()
            return dpHeight - 50
        }

    companion object {
        const val MULTIPLY_DOWNLOAD = "multiply download"
        const val BASE_BOOK_URL = "/b/"
        const val MAX_BOOK_NUMBER = 548398
        private var savedList: ArrayList<FoundedEntity> = arrayListOf()
        private var lastScrolled = -1
        val currentRequestState = MutableLiveData<String>()
    }
}