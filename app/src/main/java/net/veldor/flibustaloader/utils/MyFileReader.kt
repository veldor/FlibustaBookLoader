package net.veldor.flibustaloader.utils

import android.util.Log
import net.veldor.flibustaloader.App
import java.io.*

object MyFileReader {
    const val SEARCH_AUTOCOMPLETE_FILE = "searchAutocomplete.xml"
    const val SUBSCRIPTIONS_FILE = "subscriptions.list"
    private const val SEARCH_AUTOCOMPLETE_NEW =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><search> </search>"
    private const val SUBSCRIBE_NEW =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><subscribe> </subscribe>"
    private const val BLACKLIST_NEW =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><blacklist> </blacklist>"
    const val BOOKS_SUBSCRIBE_FILE = "booksSubscribe.xml"
    const val BOOKS_BLACKLIST_FILE = "booksBlacklist.xml"
    const val AUTHORS_SUBSCRIBE_FILE = "authorsSubscribe.xml"
    const val AUTHORS_BLACKLIST_FILE = "authorsBlacklist.xml"
    const val SEQUENCES_SUBSCRIBE_FILE = "sequencesSubscribe.xml"
    const val SEQUENCES_BLACKLIST_FILE = "sequencesBlacklist.xml"
    const val GENRES_SUBSCRIBE_FILE = "genresSubscribe.xml"
    const val GENRES_BLACKLIST_FILE = "genresBlacklist.xml"
    const val FORMAT_BLACKLIST_FILE = "formatBlacklist.xml"

    @kotlin.jvm.JvmStatic
    fun getSearchAutocomplete(): String {
        val autocompleteFile = File(App.instance.filesDir, SEARCH_AUTOCOMPLETE_FILE)
        if (!autocompleteFile.exists()) {
            makeFile(autocompleteFile, SEARCH_AUTOCOMPLETE_NEW)
        }
        val text = StringBuilder()
        try {
            val br = BufferedReader(FileReader(autocompleteFile))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                text.append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return text.toString()
    }

    private fun getBooksSubscribe(): String {
        val booksSubscribeFile = File(App.instance.filesDir, BOOKS_SUBSCRIBE_FILE)
        if (!booksSubscribeFile.exists()) {
            makeFile(booksSubscribeFile, SUBSCRIBE_NEW)
        }
        val text = StringBuilder()
        try {
            val br = BufferedReader(FileReader(booksSubscribeFile))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                text.append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return text.toString()
    }

    private fun getBooksBlacklist(): String {
        val booksBlacklistFile = File(App.instance.filesDir, BOOKS_BLACKLIST_FILE)
        if (!booksBlacklistFile.exists()) {
            makeFile(booksBlacklistFile, BLACKLIST_NEW)
        }
        val text = StringBuilder()
        try {
            val br = BufferedReader(FileReader(booksBlacklistFile))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                text.append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return text.toString()
    }

    private fun getAuthorsSubscribe(): String {
        val authorsSubscribeFile = File(App.instance.filesDir, AUTHORS_SUBSCRIBE_FILE)
        if (!authorsSubscribeFile.exists()) {
            makeFile(authorsSubscribeFile, SUBSCRIBE_NEW)
        }
        val text = StringBuilder()
        try {
            val br = BufferedReader(FileReader(authorsSubscribeFile))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                text.append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return text.toString()
    }

    private fun getAuthorsBlacklist(): String {
        val authorsBlacklistFile = File(App.instance.filesDir, AUTHORS_BLACKLIST_FILE)
        if (!authorsBlacklistFile.exists()) {
            makeFile(authorsBlacklistFile, BLACKLIST_NEW)
        }
        val text = StringBuilder()
        try {
            val br = BufferedReader(FileReader(authorsBlacklistFile))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                text.append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return text.toString()
    }

    private fun getSequencesBlacklist(): String {
        val blacklistFile = File(App.instance.filesDir, SEQUENCES_BLACKLIST_FILE)
        if (!blacklistFile.exists()) {
            makeFile(blacklistFile, BLACKLIST_NEW)
        }
        val text = StringBuilder()
        try {
            val br = BufferedReader(FileReader(blacklistFile))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                text.append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return text.toString()
    }
    private fun getFormatBlacklist(): String {
        val blacklistFile = File(App.instance.filesDir, FORMAT_BLACKLIST_FILE)
        if (!blacklistFile.exists()) {
            makeFile(blacklistFile, BLACKLIST_NEW)
        }
        val text = StringBuilder()
        try {
            val br = BufferedReader(FileReader(blacklistFile))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                text.append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return text.toString()
    }

    private fun getGenresBlacklist(): String {
        val blacklistFile = File(App.instance.filesDir, GENRES_BLACKLIST_FILE)
        if (!blacklistFile.exists()) {
            makeFile(blacklistFile, BLACKLIST_NEW)
        }
        val text = StringBuilder()
        try {
            val br = BufferedReader(FileReader(blacklistFile))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                text.append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return text.toString()
    }

    private fun getGenresSubscribe(): String {
        val sequencesSubscribeFile = File(App.instance.filesDir, GENRES_SUBSCRIBE_FILE)
        if (!sequencesSubscribeFile.exists()) {
            makeFile(sequencesSubscribeFile, SUBSCRIBE_NEW)
        }
        val text = StringBuilder()
        try {
            val br = BufferedReader(FileReader(sequencesSubscribeFile))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                text.append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return text.toString()
    }
    private fun getSequencesSubscribe(): String {
        val sequencesSubscribeFile = File(App.instance.filesDir, SEQUENCES_SUBSCRIBE_FILE)
        if (!sequencesSubscribeFile.exists()) {
            makeFile(sequencesSubscribeFile, SUBSCRIBE_NEW)
        }
        val text = StringBuilder()
        try {
            val br = BufferedReader(FileReader(sequencesSubscribeFile))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                text.append(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return text.toString()
    }

    fun saveSearchAutocomplete(value: String) {
        val autocompleteFile = File(App.instance.filesDir, SEARCH_AUTOCOMPLETE_FILE)
        makeFile(autocompleteFile, value)
        Log.d("surprise", "MyFileReader saveSearchAutocomplete: save file $autocompleteFile")
    }

    @kotlin.jvm.JvmStatic
    fun clearAutocomplete() {
        val autocompleteFile = File(App.instance.filesDir, SEARCH_AUTOCOMPLETE_FILE)
        makeFile(autocompleteFile, SEARCH_AUTOCOMPLETE_NEW)
    }

    private fun makeFile(file: File, content: String) {
        try {
            val writer = FileWriter(file)
            writer.append(content)
            writer.flush()
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun saveBooksSubscription(value: String) {
        val subscriptionFile = File(App.instance.filesDir, BOOKS_SUBSCRIBE_FILE)
        makeFile(subscriptionFile, value)
    }

    private fun saveBooksBlacklist(value: String) {
        val blacklistFile = File(App.instance.filesDir, BOOKS_BLACKLIST_FILE)
        makeFile(blacklistFile, value)
    }

    private fun saveAuthorsSubscription(value: String) {
        val subscriptionFile = File(App.instance.filesDir, AUTHORS_SUBSCRIBE_FILE)
        makeFile(subscriptionFile, value)
    }

    private fun saveAuthorsBlacklist(value: String) {
        val blacklistFile = File(App.instance.filesDir, AUTHORS_BLACKLIST_FILE)
        makeFile(blacklistFile, value)
    }

    private fun saveSequencesSubscription(value: String) {
        val subscriptionFile = File(App.instance.filesDir, SEQUENCES_SUBSCRIBE_FILE)
        makeFile(subscriptionFile, value)
    }

    private fun saveSequencesBlacklist(value: String) {
        val blacklistFile = File(App.instance.filesDir, SEQUENCES_BLACKLIST_FILE)
        makeFile(blacklistFile, value)
    }

    private fun saveGenresBlacklist(value: String) {
        val blacklistFile = File(App.instance.filesDir, GENRES_BLACKLIST_FILE)
        makeFile(blacklistFile, value)
    }
    private fun saveFormatBlacklist(value: String) {
        val blacklistFile = File(App.instance.filesDir, FORMAT_BLACKLIST_FILE)
        makeFile(blacklistFile, value)
    }
    private fun saveGenresSubscription(value: String) {
        val blacklistFile = File(App.instance.filesDir, GENRES_SUBSCRIBE_FILE)
        makeFile(blacklistFile, value)
    }

    fun getBlacklist(blacklistFileName: String): String {
        return when (blacklistFileName) {
            BOOKS_BLACKLIST_FILE -> getBooksBlacklist()
            AUTHORS_BLACKLIST_FILE -> getAuthorsBlacklist()
            SEQUENCES_BLACKLIST_FILE -> getSequencesBlacklist()
            FORMAT_BLACKLIST_FILE -> getFormatBlacklist()
            else -> getGenresBlacklist()
        }
    }

    fun getSubscribe(fileName: String): String {
        return when (fileName) {
            BOOKS_SUBSCRIBE_FILE -> getBooksSubscribe()
            AUTHORS_SUBSCRIBE_FILE -> getAuthorsSubscribe()
            SEQUENCES_SUBSCRIBE_FILE -> getSequencesSubscribe()
            else -> getGenresSubscribe()
        }
    }

    fun saveBlacklist(blacklistFileName: String, content: String) {
        when(blacklistFileName){
            BOOKS_BLACKLIST_FILE -> saveBooksBlacklist(content)
            AUTHORS_BLACKLIST_FILE -> saveAuthorsBlacklist(content)
            SEQUENCES_BLACKLIST_FILE -> saveSequencesBlacklist(content)
            GENRES_BLACKLIST_FILE -> saveGenresBlacklist(content)
            FORMAT_BLACKLIST_FILE -> saveFormatBlacklist(content)
        }
    }
    fun saveSubscription(fileName: String, content: String) {
        when(fileName){
            BOOKS_SUBSCRIBE_FILE -> saveBooksSubscription(content)
            AUTHORS_SUBSCRIBE_FILE -> saveAuthorsSubscription(content)
            SEQUENCES_SUBSCRIBE_FILE -> saveSequencesSubscription(content)
            GENRES_SUBSCRIBE_FILE -> saveGenresSubscription(content)
        }
    }
}