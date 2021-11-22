package net.veldor.flibustaloader.handlers

import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.utils.MyFileReader
import net.veldor.flibustaloader.utils.XMLHandler
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.collections.ArrayList

object BackupSettings {


    fun backup(dir: File, options: BooleanArray): File? {
        try {
            if (dir.exists() && dir.isDirectory) {
                if (!dir.canWrite()) {
                    Log.d("surprise", "can't write in dir $dir")
                } else {
                    val sdf = SimpleDateFormat("yyyy MM dd", Locale.ENGLISH)
                    val filename =
                        "Резервная копия Flibusta downloader от " + sdf.format(Date()) + ".zip"
                    sCompatBackupFile =
                        File(dir, filename)
                    sCompatBackupFile!!.createNewFile()
                    val dest = FileOutputStream(sCompatBackupFile)
                    val out = ZipOutputStream(BufferedOutputStream(dest))
                    val dataBuffer = ByteArray(BUFFER)
                    val backupDir =
                        File(Environment.getExternalStorageDirectory(), App.BACKUP_DIR_NAME)
                    if (!backupDir.exists()) {
                        val result = backupDir.mkdirs()
                        if (result) {
                            Log.d("surprise", "ReserveWorker doWork: dir created")
                        }
                    }
                    if (options[0]) {
                        val sharedPrefsFile: File =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                File(App.instance.dataDir.toString() + "/shared_prefs/net.veldor.flibustaloader_preferences.xml")
                            } else {
                                File(
                                    Environment.getDataDirectory()
                                        .toString() + "/shared_prefs/net.veldor.flibustaloader_preferences.xml"
                                )
                            }
                        writeToZip(out, dataBuffer, sharedPrefsFile, PREF_BACKUP_NAME)
                    }
                    if (options[3]) {
                        // сохраню автозаполнение поиска
                        val autocompleteFile =
                            File(App.instance.filesDir, MyFileReader.SEARCH_AUTOCOMPLETE_FILE)
                        if (autocompleteFile.isFile) {
                            writeToZip(
                                out, dataBuffer, autocompleteFile,
                                AUTOFILL_BACKUP_NAME
                            )
                        }
                    }
                    // сохраню чёрные списки
                    var blacklistFile =
                        File(App.instance.filesDir, MyFileReader.BOOKS_BLACKLIST_FILE)
                    if (options[9]) {
                        if (blacklistFile.isFile) {
                            writeToZip(
                                out, dataBuffer, blacklistFile,
                                BLACKLIST_BOOKS_BACKUP_NAME
                            )
                        }
                    }
                    if (options[10]) {
                        blacklistFile =
                            File(App.instance.filesDir, MyFileReader.AUTHORS_BLACKLIST_FILE)
                        if (blacklistFile.isFile) {
                            writeToZip(
                                out, dataBuffer, blacklistFile,
                                BLACKLIST_AUTHORS_BACKUP_NAME
                            )
                        }
                    }
                    if (options[11]) {
                        blacklistFile =
                            File(App.instance.filesDir, MyFileReader.GENRES_BLACKLIST_FILE)
                        if (blacklistFile.isFile) {
                            writeToZip(
                                out, dataBuffer, blacklistFile,
                                BLACKLIST_GENRES_BACKUP_NAME
                            )
                        }
                    }
                    if (options[12]) {
                        blacklistFile =
                            File(App.instance.filesDir, MyFileReader.SEQUENCES_BLACKLIST_FILE)
                        if (blacklistFile.isFile) {
                            writeToZip(
                                out, dataBuffer, blacklistFile,
                                BLACKLIST_SEQUENCES_BACKUP_NAME
                            )
                        }
                    }
                    if (options[13]) {
                        blacklistFile =
                            File(App.instance.filesDir, MyFileReader.FORMAT_BLACKLIST_FILE)
                        if (blacklistFile.isFile) {
                            writeToZip(
                                out, dataBuffer, blacklistFile,
                                BLACKLIST_FORMAT_BACKUP_NAME
                            )
                        }
                    }
                    var subscriptionFile =
                        File(App.instance.filesDir, MyFileReader.BOOKS_SUBSCRIBE_FILE)
                    if (options[5]) {
                        // сохраню подписки
                        if (subscriptionFile.isFile) {
                            writeToZip(
                                out, dataBuffer, subscriptionFile,
                                BOOKS_SUBSCRIBE_BACKUP_NAME
                            )
                        }
                    }
                    if (options[6]) {
                        subscriptionFile =
                            File(App.instance.filesDir, MyFileReader.AUTHORS_SUBSCRIBE_FILE)
                        if (subscriptionFile.isFile) {
                            writeToZip(
                                out, dataBuffer, subscriptionFile,
                                AUTHORS_SUBSCRIBE_BACKUP_NAME
                            )
                        }
                    }
                    if (options[8]) {
                        subscriptionFile =
                            File(App.instance.filesDir, MyFileReader.SEQUENCES_SUBSCRIBE_FILE)
                        if (subscriptionFile.isFile) {
                            writeToZip(
                                out, dataBuffer, subscriptionFile,
                                SEQUENCES_SUBSCRIBE_BACKUP_NAME
                            )
                        }
                    }
                    if (options[7]) {
                        subscriptionFile =
                            File(App.instance.filesDir, MyFileReader.GENRES_SUBSCRIBE_FILE)
                        if (subscriptionFile.isFile) {
                            writeToZip(
                                out, dataBuffer, subscriptionFile,
                                GENRE_SUBSCRIBE_BACKUP_NAME
                            )
                        }
                    }

                    // первым делом- получу из базы данных списки прочитанных и скачанных книг
                    val db = App.instance.mDatabase
                    val books = db.downloadedBooksDao().allBooks
                    if (books != null && books.isNotEmpty()) {
                        // создам XML
                        val xmlBuilder = StringBuilder()
                        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><downloaded_books>")
                        for (book in books) {
                            xmlBuilder.append("<book id=\"")
                            xmlBuilder.append(book!!.bookId)
                            xmlBuilder.append("\"/>")
                        }
                        xmlBuilder.append("</downloaded_books>")
                        val text = xmlBuilder.toString()
                        Log.d("surprise", "ReserveSettingsWorker doWork $text")
                        val f1 = File(backupDir, "downloaded_books")
                        val writer = FileWriter(f1)
                        writer.append(text)
                        writer.flush()
                        writer.close()
                        if (options[1]) {
                            writeToZip(
                                out, dataBuffer, f1,
                                DOWNLOADED_BOOKS_BACKUP_NAME
                            )
                        }
                        val result = f1.delete()
                        if (!result) {
                            Log.d(
                                "surprise",
                                "ReserveSettingsWorker doWork не удалось удалить временный файл"
                            )
                        }
                    }
                    val rBooks = db.readBooksDao().allBooks
                    if (rBooks != null && rBooks.isNotEmpty()) {
                        // создам XML
                        val xmlBuilder = StringBuilder()
                        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><readed_books>")
                        for (book in rBooks) {
                            xmlBuilder.append("<book id=\"")
                            xmlBuilder.append(book!!.bookId)
                            xmlBuilder.append("\"/>")
                        }
                        xmlBuilder.append("</readed_books>")
                        val text = xmlBuilder.toString()
                        val f1 = File(backupDir, "readed_books")
                        val writer = FileWriter(f1)
                        writer.append(text)
                        writer.flush()
                        writer.close()
                        if (options[2]) {
                            writeToZip(out, dataBuffer, f1, READED_BOOKS_BACKUP_NAME)
                        }
                        val result = f1.delete()
                        if (!result) {
                            Log.d(
                                "surprise",
                                "ReserveSettingsWorker doWork не удалось удалить временный файл"
                            )
                        }
                    }
                    // закладки
                    val rBookmarks = db.bookmarksDao().allBookmarks
                    if (rBookmarks.isNotEmpty()) {
                        // создам XML
                        val xmlBuilder = StringBuilder()
                        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><bookmarks>")
                        for (bookmark in rBookmarks) {
                            xmlBuilder.append("<bookmark name=\"")
                            xmlBuilder.append(bookmark.name)
                            xmlBuilder.append("\" link=\"")
                            xmlBuilder.append(bookmark.link)
                            xmlBuilder.append("\"/>")
                        }
                        xmlBuilder.append("</bookmarks>")
                        val text = xmlBuilder.toString()
                        val f1 = File(backupDir, "bookmarks")
                        val writer = FileWriter(f1)
                        writer.append(text)
                        writer.flush()
                        writer.close()
                        if (options[4]) {
                            writeToZip(out, dataBuffer, f1, BOOKMARKS_BACKUP_NAME)
                        }
                        val result = f1.delete()
                        if (!result) {
                            Log.d(
                                "surprise",
                                "ReserveSettingsWorker doWork не удалось удалить временный файл"
                            )
                        }
                    }
                    // список загрузки
                    val rDownloadSchedule = db.booksDownloadScheduleDao().allBooks
                    if (rDownloadSchedule != null && rDownloadSchedule.isNotEmpty()) {
                        // создам XML
                        val xmlBuilder = StringBuilder()
                        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><schedule>")
                        for (schedule in rDownloadSchedule) {
                            xmlBuilder.append("<item bookId=\"")
                            xmlBuilder.append(schedule.bookId)
                            xmlBuilder.append("\" link=\"")
                            xmlBuilder.append(schedule.link)
                            xmlBuilder.append("\" name=\"")
                            xmlBuilder.append(schedule.name)
                            xmlBuilder.append("\" size=\"")
                            xmlBuilder.append(schedule.size)
                            xmlBuilder.append("\" author=\"")
                            xmlBuilder.append(schedule.author)
                            xmlBuilder.append("\" format=\"")
                            xmlBuilder.append(schedule.format)
                            xmlBuilder.append("\" authorDirName=\"")
                            xmlBuilder.append(schedule.authorDirName)
                            xmlBuilder.append("\" sequenceDirName=\"")
                            xmlBuilder.append(schedule.sequenceDirName)
                            xmlBuilder.append("\" reservedSequenceName=\"")
                            xmlBuilder.append(schedule.reservedSequenceName)
                            xmlBuilder.append("\"/>")
                        }
                        xmlBuilder.append("</schedule>")
                        val text = xmlBuilder.toString()
                        val f1 = File(backupDir, "schedule")
                        val writer = FileWriter(f1)
                        writer.append(text)
                        writer.flush()
                        writer.close()
                        if (options[14]) {
                            writeToZip(
                                out, dataBuffer, f1,
                                DOWNLOAD_SCHEDULE_BACKUP_NAME
                            )
                        }
                        val result = f1.delete()
                        if (!result) {
                            Log.d(
                                "surprise",
                                "ReserveSettingsWorker doWork не удалось удалить временный файл"
                            )
                        }
                    }
                    out.close()
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        Log.d("surprise", "BackupSettings.kt 159  backup: size is ${sCompatBackupFile?.length()}")
        return sCompatBackupFile
    }

    fun backup(dir: DocumentFile, options: BooleanArray): DocumentFile? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // сохраню файл в выбранную директорию
            val sdf = SimpleDateFormat("yyyy/MM/dd HH-mm-ss", Locale.ENGLISH)
            val filename = "Резервная копия Flibusta downloader от " + sdf.format(Date())
            sBackupFile =
                dir.createFile("application/zip", filename)
            var zip: File? = null
            try {
                val backupDir = File(Environment.getExternalStorageDirectory(), App.BACKUP_DIR_NAME)
                if (!backupDir.exists()) {
                    val result = backupDir.mkdirs()
                    if (result) {
                        Log.d("surprise", "ReserveWorker doWork: dir created")
                    }
                }
                zip = File(backupDir, App.BACKUP_FILE_NAME)
                val dest = FileOutputStream(zip)
                val out = ZipOutputStream(BufferedOutputStream(dest))
                val dataBuffer = ByteArray(BUFFER)
                if (options[0]) {
                    val sharedPrefsFile: File =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            File(App.instance.dataDir.toString() + "/shared_prefs/net.veldor.flibustaloader_preferences.xml")
                        } else {
                            File(
                                Environment.getDataDirectory()
                                    .toString() + "/shared_prefs/net.veldor.flibustaloader_preferences.xml"
                            )
                        }
                    writeToZip(out, dataBuffer, sharedPrefsFile, PREF_BACKUP_NAME)
                }
                if (options[3]) {
                    // сохраню автозаполнение поиска
                    val autocompleteFile =
                        File(App.instance.filesDir, MyFileReader.SEARCH_AUTOCOMPLETE_FILE)
                    if (autocompleteFile.isFile) {
                        writeToZip(
                            out, dataBuffer, autocompleteFile,
                            AUTOFILL_BACKUP_NAME
                        )
                    }
                }
                // сохраню чёрные списки
                var blacklistFile =
                    File(App.instance.filesDir, MyFileReader.BOOKS_BLACKLIST_FILE)
                if (options[9]) {
                    if (blacklistFile.isFile) {
                        writeToZip(
                            out, dataBuffer, blacklistFile,
                            BLACKLIST_BOOKS_BACKUP_NAME
                        )
                    }
                }
                if (options[10]) {
                    blacklistFile =
                        File(App.instance.filesDir, MyFileReader.AUTHORS_BLACKLIST_FILE)
                    if (blacklistFile.isFile) {
                        writeToZip(
                            out, dataBuffer, blacklistFile,
                            BLACKLIST_AUTHORS_BACKUP_NAME
                        )
                    }
                }
                if (options[11]) {
                    blacklistFile = File(App.instance.filesDir, MyFileReader.GENRES_BLACKLIST_FILE)
                    if (blacklistFile.isFile) {
                        writeToZip(
                            out, dataBuffer, blacklistFile,
                            BLACKLIST_GENRES_BACKUP_NAME
                        )
                    }
                }
                if (options[12]) {
                    blacklistFile =
                        File(App.instance.filesDir, MyFileReader.SEQUENCES_BLACKLIST_FILE)
                    if (blacklistFile.isFile) {
                        writeToZip(
                            out, dataBuffer, blacklistFile,
                            BLACKLIST_SEQUENCES_BACKUP_NAME
                        )
                    }
                }
                if (options[13]) {
                    blacklistFile =
                        File(App.instance.filesDir, MyFileReader.FORMAT_BLACKLIST_FILE)
                    if (blacklistFile.isFile) {
                        writeToZip(
                            out, dataBuffer, blacklistFile,
                            BLACKLIST_FORMAT_BACKUP_NAME
                        )
                    }
                }
                var subscriptionFile =
                    File(App.instance.filesDir, MyFileReader.BOOKS_SUBSCRIBE_FILE)
                if (options[5]) {
                    // сохраню подписки
                    if (subscriptionFile.isFile) {
                        writeToZip(
                            out, dataBuffer, subscriptionFile,
                            BOOKS_SUBSCRIBE_BACKUP_NAME
                        )
                    }
                }
                if (options[6]) {
                    subscriptionFile =
                        File(App.instance.filesDir, MyFileReader.AUTHORS_SUBSCRIBE_FILE)
                    if (subscriptionFile.isFile) {
                        writeToZip(
                            out, dataBuffer, subscriptionFile,
                            AUTHORS_SUBSCRIBE_BACKUP_NAME
                        )
                    }
                }
                if (options[8]) {
                    subscriptionFile =
                        File(App.instance.filesDir, MyFileReader.SEQUENCES_SUBSCRIBE_FILE)
                    if (subscriptionFile.isFile) {
                        writeToZip(
                            out, dataBuffer, subscriptionFile,
                            SEQUENCES_SUBSCRIBE_BACKUP_NAME
                        )
                    }
                }
                if (options[7]) {
                    subscriptionFile =
                        File(App.instance.filesDir, MyFileReader.GENRES_SUBSCRIBE_FILE)
                    if (subscriptionFile.isFile) {
                        writeToZip(
                            out, dataBuffer, subscriptionFile,
                            GENRE_SUBSCRIBE_BACKUP_NAME
                        )
                    }
                }

                // первым делом- получу из базы данных списки прочитанных и скачанных книг
                val db = App.instance.mDatabase
                val books = db.downloadedBooksDao().allBooks
                if (books != null && books.isNotEmpty()) {
                    // создам XML
                    val xmlBuilder = StringBuilder()
                    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><downloaded_books>")
                    for (book in books) {
                        xmlBuilder.append("<book id=\"")
                        xmlBuilder.append(book!!.bookId)
                        xmlBuilder.append("\"/>")
                    }
                    xmlBuilder.append("</downloaded_books>")
                    val text = xmlBuilder.toString()
                    Log.d("surprise", "ReserveSettingsWorker doWork $text")
                    val f1 = File(backupDir, "downloaded_books")
                    val writer = FileWriter(f1)
                    writer.append(text)
                    writer.flush()
                    writer.close()
                    if (options[1]) {
                        writeToZip(
                            out, dataBuffer, f1,
                            DOWNLOADED_BOOKS_BACKUP_NAME
                        )
                    }
                    val result = f1.delete()
                    if (!result) {
                        Log.d(
                            "surprise",
                            "ReserveSettingsWorker doWork не удалось удалить временный файл"
                        )
                    }
                }
                val rBooks = db.readBooksDao().allBooks
                if (rBooks != null && rBooks.isNotEmpty()) {
                    // создам XML
                    val xmlBuilder = StringBuilder()
                    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><readed_books>")
                    for (book in rBooks) {
                        xmlBuilder.append("<book id=\"")
                        xmlBuilder.append(book!!.bookId)
                        xmlBuilder.append("\"/>")
                    }
                    xmlBuilder.append("</readed_books>")
                    val text = xmlBuilder.toString()
                    val f1 = File(backupDir, "readed_books")
                    val writer = FileWriter(f1)
                    writer.append(text)
                    writer.flush()
                    writer.close()
                    if (options[2]) {
                        writeToZip(out, dataBuffer, f1, READED_BOOKS_BACKUP_NAME)
                    }
                    val result = f1.delete()
                    if (!result) {
                        Log.d(
                            "surprise",
                            "ReserveSettingsWorker doWork не удалось удалить временный файл"
                        )
                    }
                }
                // закладки
                val rBookmarks = db.bookmarksDao().allBookmarks
                if (rBookmarks.isNotEmpty()) {
                    // создам XML
                    val xmlBuilder = StringBuilder()
                    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><bookmarks>")
                    for (bookmark in rBookmarks) {
                        xmlBuilder.append("<bookmark name=\"")
                        xmlBuilder.append(bookmark.name)
                        xmlBuilder.append("\" link=\"")
                        xmlBuilder.append(bookmark.link)
                        xmlBuilder.append("\"/>")
                    }
                    xmlBuilder.append("</bookmarks>")
                    val text = xmlBuilder.toString()
                    val f1 = File(backupDir, "bookmarks")
                    val writer = FileWriter(f1)
                    writer.append(text)
                    writer.flush()
                    writer.close()
                    if (options[4]) {
                        writeToZip(out, dataBuffer, f1, BOOKMARKS_BACKUP_NAME)
                    }
                    val result = f1.delete()
                    if (!result) {
                        Log.d(
                            "surprise",
                            "ReserveSettingsWorker doWork не удалось удалить временный файл"
                        )
                    }
                }
                // список загрузки
                val rDownloadSchedule = db.booksDownloadScheduleDao().allBooks
                if (rDownloadSchedule != null && rDownloadSchedule.isNotEmpty()) {
                    // создам XML
                    val xmlBuilder = StringBuilder()
                    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><schedule>")
                    for (schedule in rDownloadSchedule) {
                        xmlBuilder.append("<item bookId=\"")
                        xmlBuilder.append(schedule.bookId)
                        xmlBuilder.append("\" link=\"")
                        xmlBuilder.append(schedule.link)
                        xmlBuilder.append("\" name=\"")
                        xmlBuilder.append(schedule.name)
                        xmlBuilder.append("\" size=\"")
                        xmlBuilder.append(schedule.size)
                        xmlBuilder.append("\" author=\"")
                        xmlBuilder.append(schedule.author)
                        xmlBuilder.append("\" format=\"")
                        xmlBuilder.append(schedule.format)
                        xmlBuilder.append("\" authorDirName=\"")
                        xmlBuilder.append(schedule.authorDirName)
                        xmlBuilder.append("\" sequenceDirName=\"")
                        xmlBuilder.append(schedule.sequenceDirName)
                        xmlBuilder.append("\" reservedSequenceName=\"")
                        xmlBuilder.append(schedule.reservedSequenceName)
                        xmlBuilder.append("\"/>")
                    }
                    xmlBuilder.append("</schedule>")
                    val text = xmlBuilder.toString()
                    val f1 = File(backupDir, "schedule")
                    val writer = FileWriter(f1)
                    writer.append(text)
                    writer.flush()
                    writer.close()
                    if (options[14]) {
                        writeToZip(
                            out, dataBuffer, f1,
                            DOWNLOAD_SCHEDULE_BACKUP_NAME
                        )
                    }
                    val result = f1.delete()
                    if (!result) {
                        Log.d(
                            "surprise",
                            "ReserveSettingsWorker doWork не удалось удалить временный файл"
                        )
                    }
                }
                out.close()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            try {
                val fileStream = App.instance.contentResolver.openOutputStream(
                    sBackupFile!!.uri
                )
                if (fileStream != null) {
                    val inputStream = FileInputStream(zip)
                    val b = ByteArray(8192)
                    var r: Int
                    while (inputStream.read(b).also { r = it } != -1) {
                        fileStream.write(b, 0, r)
                    }
                    fileStream.close()
                    inputStream.close()
                    val deleteResult = zip!!.delete()
                    Log.d(
                        "surprise",
                        "ReserveSettingsWorker doWork 171: transfer file delete status is $deleteResult"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return sBackupFile
    }

    fun restore(file: DocumentFile, options: BooleanArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val fileData = App.instance.contentResolver.openInputStream(file.uri)
                val zin = ZipInputStream(fileData)
                var ze: ZipEntry?
                var targetFile: File
                while (zin.nextEntry.also { ze = it } != null) {
                    when (ze!!.name) {
                        PREF_BACKUP_NAME -> {
                            if (options[0]) {
                                targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    File(App.instance.dataDir.toString() + "/shared_prefs/net.veldor.flibustaloader_preferences.xml")
                                } else {
                                    File(
                                        Environment.getDataDirectory()
                                            .toString() + "/shared_prefs/net.veldor.flibustaloader_preferences.xml"
                                    )
                                }
                                extractFromZip(zin, targetFile)
                            }
                        }
                        AUTOFILL_BACKUP_NAME -> {
                            if (options[3]) {
                                targetFile = File(
                                    App.instance.filesDir,
                                    MyFileReader.SEARCH_AUTOCOMPLETE_FILE
                                )
                                extractFromZip(zin, targetFile)
                            }
                        }
                        BLACKLIST_BOOKS_BACKUP_NAME -> {
                            if (options[9]) {
                                targetFile = File(
                                    App.instance.filesDir,
                                    MyFileReader.BOOKS_BLACKLIST_FILE
                                )
                                extractFromZip(zin, targetFile)
                            }
                        }
                        BLACKLIST_AUTHORS_BACKUP_NAME -> {
                            if (options[10]) {
                                targetFile = File(
                                    App.instance.filesDir,
                                    MyFileReader.AUTHORS_BLACKLIST_FILE
                                )
                                extractFromZip(zin, targetFile)
                            }
                        }
                        BLACKLIST_GENRES_BACKUP_NAME -> {
                            if (options[11]) {
                                targetFile = File(
                                    App.instance.filesDir,
                                    MyFileReader.GENRES_BLACKLIST_FILE
                                )
                                extractFromZip(zin, targetFile)
                            }
                        }
                        BLACKLIST_SEQUENCES_BACKUP_NAME -> {
                            if (options[12]) {
                                targetFile = File(
                                    App.instance.filesDir,
                                    MyFileReader.SEQUENCES_BLACKLIST_FILE
                                )
                                extractFromZip(zin, targetFile)
                            }
                        }
                        BLACKLIST_FORMAT_BACKUP_NAME -> {
                            if (options[13]) {
                                targetFile = File(
                                    App.instance.filesDir,
                                    MyFileReader.FORMAT_BLACKLIST_FILE
                                )
                                extractFromZip(zin, targetFile)
                            }
                        }
                        BOOKS_SUBSCRIBE_BACKUP_NAME -> {
                            if (options[5]) {
                                targetFile = File(
                                    App.instance.filesDir,
                                    MyFileReader.BOOKS_SUBSCRIBE_FILE
                                )
                                extractFromZip(zin, targetFile)
                            }
                        }
                        AUTHORS_SUBSCRIBE_BACKUP_NAME -> {
                            if (options[6]) {
                                targetFile =
                                    File(App.instance.filesDir, MyFileReader.AUTHORS_SUBSCRIBE_FILE)
                                extractFromZip(zin, targetFile)
                            }
                        }
                        SEQUENCES_SUBSCRIBE_BACKUP_NAME -> {
                            if (options[8]) {
                                targetFile =
                                    File(
                                        App.instance.filesDir,
                                        MyFileReader.SEQUENCES_SUBSCRIBE_FILE
                                    )
                                extractFromZip(zin, targetFile)
                            }
                        }
                        GENRE_SUBSCRIBE_BACKUP_NAME -> {
                            if (options[7]) {
                                targetFile =
                                    File(
                                        App.instance.filesDir,
                                        MyFileReader.GENRES_SUBSCRIBE_FILE
                                    )
                                extractFromZip(zin, targetFile)
                            }
                        }
                        DOWNLOADED_BOOKS_BACKUP_NAME -> {
                            if (options[1]) {
                                XMLHandler.handleBackup(zin)
                            }
                        }
                        READED_BOOKS_BACKUP_NAME -> {
                            if (options[2]) {
                                XMLHandler.handleBackup(zin)
                            }
                        }
                        BOOKMARKS_BACKUP_NAME -> {
                            if (options[4]) {
                                XMLHandler.handleBackup(zin)
                            }
                        }
                        DOWNLOAD_SCHEDULE_BACKUP_NAME -> {
                            if (options[14]) {
                                XMLHandler.handleBackup(zin)
                            }
                        }
                    }
                }
                zin.close()
                Log.d("surprise", "restore: backup restored!")
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    fun restore(file: File, options: BooleanArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val fileData = file.inputStream()
                val zin = ZipInputStream(fileData)
                var ze: ZipEntry?
                var targetFile: File
                while (zin.nextEntry.also { ze = it } != null) {
                    when (ze!!.name) {
                        PREF_BACKUP_NAME -> {
                            if (options[0]) {
                                targetFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    File(App.instance.dataDir.toString() + "/shared_prefs/net.veldor.flibustaloader_preferences.xml")
                                } else {
                                    File(
                                        Environment.getDataDirectory()
                                            .toString() + "/shared_prefs/net.veldor.flibustaloader_preferences.xml"
                                    )
                                }
                                extractFromZip(zin, targetFile)
                            }
                        }
                        AUTOFILL_BACKUP_NAME -> {
                            if (options[3]) {
                                targetFile = File(
                                    App.instance.filesDir,
                                    MyFileReader.SEARCH_AUTOCOMPLETE_FILE
                                )
                                extractFromZip(zin, targetFile)
                            }
                        }
                        BLACKLIST_BOOKS_BACKUP_NAME -> {
                            if (options[9]) {
                                targetFile = File(
                                    App.instance.filesDir,
                                    MyFileReader.BOOKS_BLACKLIST_FILE
                                )
                                extractFromZip(zin, targetFile)
                            }
                        }
                        BLACKLIST_AUTHORS_BACKUP_NAME -> {
                            if (options[10]) {
                                targetFile = File(
                                    App.instance.filesDir,
                                    MyFileReader.AUTHORS_BLACKLIST_FILE
                                )
                                extractFromZip(zin, targetFile)
                            }
                        }
                        BLACKLIST_GENRES_BACKUP_NAME -> {
                            if (options[11]) {
                                targetFile = File(
                                    App.instance.filesDir,
                                    MyFileReader.GENRES_BLACKLIST_FILE
                                )
                                extractFromZip(zin, targetFile)
                            }
                        }
                        BLACKLIST_SEQUENCES_BACKUP_NAME -> {
                            if (options[12]) {
                                targetFile = File(
                                    App.instance.filesDir,
                                    MyFileReader.SEQUENCES_BLACKLIST_FILE
                                )
                                extractFromZip(zin, targetFile)
                            }
                        }
                        BLACKLIST_FORMAT_BACKUP_NAME -> {
                            if (options[13]) {
                                targetFile = File(
                                    App.instance.filesDir,
                                    MyFileReader.FORMAT_BLACKLIST_FILE
                                )
                                extractFromZip(zin, targetFile)
                            }
                        }
                        BOOKS_SUBSCRIBE_BACKUP_NAME -> {
                            if (options[5]) {
                                targetFile = File(
                                    App.instance.filesDir,
                                    MyFileReader.BOOKS_SUBSCRIBE_FILE
                                )
                                extractFromZip(zin, targetFile)
                            }
                        }
                        AUTHORS_SUBSCRIBE_BACKUP_NAME -> {
                            if (options[6]) {
                                targetFile =
                                    File(App.instance.filesDir, MyFileReader.AUTHORS_SUBSCRIBE_FILE)
                                extractFromZip(zin, targetFile)
                            }
                        }
                        SEQUENCES_SUBSCRIBE_BACKUP_NAME -> {
                            if (options[8]) {
                                targetFile =
                                    File(
                                        App.instance.filesDir,
                                        MyFileReader.SEQUENCES_SUBSCRIBE_FILE
                                    )
                                extractFromZip(zin, targetFile)
                            }
                        }
                        GENRE_SUBSCRIBE_BACKUP_NAME -> {
                            if (options[7]) {
                                targetFile =
                                    File(
                                        App.instance.filesDir,
                                        MyFileReader.GENRES_SUBSCRIBE_FILE
                                    )
                                extractFromZip(zin, targetFile)
                            }
                        }
                        DOWNLOADED_BOOKS_BACKUP_NAME -> {
                            if (options[1]) {
                                XMLHandler.handleBackup(zin)
                            }
                        }
                        READED_BOOKS_BACKUP_NAME -> {
                            if (options[2]) {
                                XMLHandler.handleBackup(zin)
                            }
                        }
                        BOOKMARKS_BACKUP_NAME -> {
                            if (options[4]) {
                                XMLHandler.handleBackup(zin)
                            }
                        }
                        DOWNLOAD_SCHEDULE_BACKUP_NAME -> {
                            if (options[14]) {
                                XMLHandler.handleBackup(zin)
                            }
                        }
                    }
                }
                zin.close()
                Log.d("surprise", "restore: backup restored!")
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    private fun writeToZip(
        stream: ZipOutputStream,
        dataBuffer: ByteArray,
        oldFileName: File,
        newFileName: String
    ) {
        if (oldFileName.exists()) {
            val fis: FileInputStream
            try {
                fis = FileInputStream(oldFileName)
                val origin = BufferedInputStream(fis, BUFFER)
                val entry = ZipEntry(newFileName)
                stream.putNextEntry(entry)
                var count: Int
                while (origin.read(dataBuffer, 0, BUFFER)
                        .also { count = it } != -1
                ) {
                    stream.write(dataBuffer, 0, count)
                }
                origin.close()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    private fun extractFromZip(zis: ZipInputStream, fileName: File) {
        try {
            val fout = FileOutputStream(fileName)
            var c = zis.read()
            while (c != -1) {
                fout.write(c)
                c = zis.read()
            }
            zis.closeEntry()
            fout.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getFilesList(dl: DocumentFile): ArrayList<String> {
        val result = ArrayList<String>()
        val fileData = App.instance.contentResolver.openInputStream(dl.uri)
        val zin = ZipInputStream(fileData)
        var ze: ZipEntry?
        while (zin.nextEntry.also { ze = it } != null) {
            result.add(ze!!.name)
        }
        return result
    }

    fun getFilesList(dl: File): ArrayList<String> {
        val result = ArrayList<String>()
        val fileData = dl.inputStream()
        val zin = ZipInputStream(fileData)
        var ze: ZipEntry?
        while (zin.nextEntry.also { ze = it } != null) {
            result.add(ze!!.name)
        }
        return result
    }


    private const val BUFFER = 1024
    const val PREF_BACKUP_NAME = "data1"
    const val DOWNLOADED_BOOKS_BACKUP_NAME = "data2"
    const val READED_BOOKS_BACKUP_NAME = "data3"
    const val AUTOFILL_BACKUP_NAME = "data4"
    const val BOOKS_SUBSCRIBE_BACKUP_NAME = "data5"
    const val AUTHORS_SUBSCRIBE_BACKUP_NAME = "data6"
    const val SEQUENCES_SUBSCRIBE_BACKUP_NAME = "data7"
    const val BOOKMARKS_BACKUP_NAME = "data8"
    const val DOWNLOAD_SCHEDULE_BACKUP_NAME = "data9"
    const val BLACKLIST_BOOKS_BACKUP_NAME = "data10"
    const val BLACKLIST_AUTHORS_BACKUP_NAME = "data11"
    const val BLACKLIST_GENRES_BACKUP_NAME = "data12"
    const val BLACKLIST_SEQUENCES_BACKUP_NAME = "data13"
    const val GENRE_SUBSCRIBE_BACKUP_NAME = "data14"
    const val BLACKLIST_FORMAT_BACKUP_NAME = "data15"

    @JvmField
    var sBackupFile: DocumentFile? = null
    private var sCompatBackupFile: File? = null
}