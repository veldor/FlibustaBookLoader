package net.veldor.flibustaloader.view_models

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.veldor.flibustaloader.selections.Book
import net.veldor.flibustaloader.utils.Grammar
import net.veldor.flibustaloader.utils.PreferencesHandler
import java.util.*

class DirContentViewModel(application: Application) : GlobalViewModel(application) {
    fun loadDirContent() {
        viewModelScope.launch(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val downloadsDir = PreferencesHandler.instance.getDownloadDir()
                if (downloadsDir != null && downloadsDir.isDirectory) {
                    val files = downloadsDir.listFiles()
                    val books = recursiveScan(files, "")
                    Log.d(
                        "surprise",
                        "ShowDownloadFolderContentActivity onCreate 37: books size is " + books.size
                    )
                    _liveFilesLoaded.postValue(books)
                }
            } else {
                val downloadDir = PreferencesHandler.instance.getDownloadDir()
                if (downloadDir != null && downloadDir.isDirectory) {
                    val files = downloadDir.listFiles()
                    val books = recursiveScan(files, "")
                    Log.d(
                        "surprise",
                        "ShowDownloadFolderContentActivity onCreate 57: len is " + books.size
                    )
                    if (books.size > 0) {
                        _liveFilesLoaded.postValue(books)
                    }
                }
            }
        }
    }

    private var lastSortOption: Int = -1
    private val _liveFilesLoaded = MutableLiveData<ArrayList<Book>>()
    val liveFilesLoaded: LiveData<ArrayList<Book>> = _liveFilesLoaded


    private fun recursiveScan(files: Array<DocumentFile>, prefix: String): ArrayList<Book> {
        val answer = ArrayList<Book>()
        if (files.isNotEmpty()) {
            var bookItem: Book
            var value: String
            var value1: String
            for (df in files) {
                if (df.isFile) {
                    value = df.name!!
                    bookItem = Book()
                    // получу имя автора- это значение до первого слеша
                    val index = value.indexOf("_")
                    val lastIndex = value.lastIndexOf("_")
                    if (index > 0 && lastIndex > 0 && index != lastIndex) {
                        value1 = value.substring(0, index)
                        bookItem.author = prefix + value1
                        value1 = value.substring(index + 1, lastIndex)
                        bookItem.name = value1
                    } else {
                        bookItem.author = prefix + "Неизвестно"
                        bookItem.name = value
                    }
                    bookItem.size = Grammar.getLiteralSize(df.length())
                    bookItem.file = df
                    bookItem.extension = Grammar.getExtension(value)
                    answer.add(bookItem)
                } else if (df.isDirectory) {
                    answer.addAll(recursiveScan(df.listFiles(), prefix + df.name + "/"))
                }
            }
        }
        return answer
    }

    fun loadDirContent(sortOption: Int) {
        Log.d("surprise", "loadDirContent: $sortOption")
        // получу контент и отсортирую его
        viewModelScope.launch(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val downloadsDir = PreferencesHandler.instance.getDownloadDir()
                if (downloadsDir != null && downloadsDir.isDirectory) {
                    val files = downloadsDir.listFiles()
                    val books = recursiveScan(files, "")
                    when (sortOption) {
                        0 -> {
                            if (lastSortOption == 0) {
                                books.sortByDescending { list -> list.name }
                            } else {
                                books.sortBy { list -> list.name }
                            }
                        }
                        1 -> {
                            if (lastSortOption == 1) {
                                books.sortByDescending { list -> list.file!!.length() }
                            } else {
                                books.sortBy { list -> list.file!!.length() }
                            }
                        }
                        2 -> {
                            if (lastSortOption == 2) {
                                books.sortByDescending { list -> list.author }
                            } else {
                                books.sortBy { list -> list.author }
                            }
                        }
                        3 -> {
                            if (lastSortOption == 3) {
                                books.sortByDescending { list -> list.extension }
                            } else {
                                books.sortBy { list -> list.extension }
                            }
                        }
                        4 -> {
                            if (lastSortOption == 3) {
                                books.sortByDescending { list -> list.file!!.lastModified() }
                            } else {
                                books.sortBy { list -> list.file!!.lastModified() }
                            }
                        }
                    }
                    _liveFilesLoaded.postValue(books)
                    lastSortOption = if (lastSortOption == sortOption) {
                        -1
                    } else {
                        sortOption
                    }
                }
            } else {
                val downloadDir = PreferencesHandler.instance.getDownloadDir()
                if (downloadDir != null && downloadDir.isDirectory) {
                    val files = downloadDir.listFiles()
                    val books = recursiveScan(files, "")
                    Log.d(
                        "surprise",
                        "ShowDownloadFolderContentActivity onCreate 57: len is " + books.size
                    )
                    if (books.size > 0) {
                        _liveFilesLoaded.postValue(books)
                    }
                }
            }
        }
    }
}