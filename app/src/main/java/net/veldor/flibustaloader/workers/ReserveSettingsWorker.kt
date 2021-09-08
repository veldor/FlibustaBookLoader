package net.veldor.flibustaloader.workers

import net.veldor.flibustaloader.App
import android.os.Environment
import android.content.Context
import android.os.Build
import net.veldor.flibustaloader.utils.MyFileReader
import androidx.documentfile.provider.DocumentFile
import android.util.Log
import androidx.work.*
import java.io.*
import java.lang.Exception
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ReserveSettingsWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // пересохраню zip в documentfile
            if (sSaveDir != null && sSaveDir!!.isDirectory) {
                // сохраню файл в выбранную директорию
                val sdf = SimpleDateFormat("yyyy/MM/dd HH-mm-ss", Locale.ENGLISH)
                val filename = "Резервная копия Flibusta downloader от " + sdf.format(Date())
                sBackupFile = sSaveDir!!.createFile("application/zip", filename)
            } else {
                return Result.failure()
            }
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
                val sharedPrefsFile: File = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    File(App.instance.dataDir.toString() + "/shared_prefs/net.veldor.flibustaloader_preferences.xml")
                } else {
                    File(
                        Environment.getDataDirectory()
                            .toString() + "/shared_prefs/net.veldor.flibustaloader_preferences.xml"
                    )
                }
                writeToZip(out, dataBuffer, sharedPrefsFile, PREF_BACKUP_NAME)
                // сохраню автозаполнение поиска
                val autocompleteFile =
                    File(App.instance.filesDir, MyFileReader.SEARCH_AUTOCOMPLETE_FILE)
                if (autocompleteFile.isFile) {
                    writeToZip(out, dataBuffer, autocompleteFile, AUTOFILL_BACKUP_NAME)
                } // сохраню чёрные списки
                var blacklistFile =
                    File(App.instance.filesDir, MyFileReader.BOOKS_BLACKLIST_FILE)
                if (autocompleteFile.isFile) {
                    writeToZip(out, dataBuffer, blacklistFile, BLACKLIST_BOOKS_BACKUP_NAME)
                }
                blacklistFile =
                    File(App.instance.filesDir, MyFileReader.AUTHORS_BLACKLIST_FILE)
                if (autocompleteFile.isFile) {
                    writeToZip(out, dataBuffer, blacklistFile, BLACKLIST_AUTHORS_BACKUP_NAME)
                }
                blacklistFile = File(App.instance.filesDir, MyFileReader.GENRES_BLACKLIST_FILE)
                if (autocompleteFile.isFile) {
                    writeToZip(out, dataBuffer, blacklistFile, BLACKLIST_GENRES_BACKUP_NAME)
                }
                blacklistFile =
                    File(App.instance.filesDir, MyFileReader.SEQUENCES_BLACKLIST_FILE)
                if (autocompleteFile.isFile) {
                    writeToZip(out, dataBuffer, blacklistFile, BLACKLIST_SEQUENCES_BACKUP_NAME)
                }
                // сохраню подписки
                var subscriptionFile =
                    File(App.instance.filesDir, MyFileReader.BOOKS_SUBSCRIBE_FILE)
                if (autocompleteFile.isFile) {
                    writeToZip(out, dataBuffer, subscriptionFile, BOOKS_SUBSCRIBE_BACKUP_NAME)
                }
                subscriptionFile =
                    File(App.instance.filesDir, MyFileReader.AUTHORS_SUBSCRIBE_FILE)
                if (autocompleteFile.isFile) {
                    writeToZip(out, dataBuffer, subscriptionFile, AUTHORS_SUBSCRIBE_BACKUP_NAME)
                }
                subscriptionFile =
                    File(App.instance.filesDir, MyFileReader.SEQUENCES_SUBSCRIBE_FILE)
                if (autocompleteFile.isFile) {
                    writeToZip(out, dataBuffer, subscriptionFile, SEQUENCES_SUBSCRIBE_BACKUP_NAME)
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
                    writeToZip(out, dataBuffer, f1, DOWNLOADED_BOOKS_BACKUP_NAME)
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
                    writeToZip(out, dataBuffer, f1, READED_BOOKS_BACKUP_NAME)
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
                if (rBookmarks != null && rBookmarks.isNotEmpty()) {
                    // создам XML
                    val xmlBuilder = StringBuilder()
                    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><bookmarks>")
                    for (bookmark in rBookmarks) {
                        xmlBuilder.append("<bookmark name=\"")
                        xmlBuilder.append(bookmark!!.name)
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
                    writeToZip(out, dataBuffer, f1, BOOKMARKS_BACKUP_NAME)
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
                        xmlBuilder.append(schedule!!.bookId)
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
                    writeToZip(out, dataBuffer, f1, DOWNLOAD_SCHEDULE_BACKUP_NAME)
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
        } else {
            try {
                if (sCompatSaveDir != null && sCompatSaveDir!!.exists() && sCompatSaveDir!!.isDirectory) {
                    val sdf = SimpleDateFormat("yyyy/MM/dd HH-mm-ss", Locale.ENGLISH)
                    val filename = "Резервная копия Flibusta downloader от " + sdf.format(Date())
                    sCompatBackupFile = File(sCompatSaveDir, filename)
                    val dest = FileOutputStream(sCompatBackupFile)
                    val out = ZipOutputStream(BufferedOutputStream(dest))
                    val dataBuffer = ByteArray(BUFFER)
                    val sharedPrefsFile = File(
                        Environment.getDataDirectory()
                            .toString() + "/shared_prefs/net.veldor.flibustaloader_preferences.xml"
                    )
                    writeToZip(out, dataBuffer, sharedPrefsFile, PREF_BACKUP_NAME)

                    // сохраню автозаполнение поиска
                    val autocompleteFile =
                        File(App.instance.filesDir, MyFileReader.SEARCH_AUTOCOMPLETE_FILE)
                    if (autocompleteFile.isFile) {
                        writeToZip(out, dataBuffer, autocompleteFile, AUTOFILL_BACKUP_NAME)
                    }
                    // сохраню подписки
                    var subscriptionFile =
                        File(App.instance.filesDir, MyFileReader.BOOKS_SUBSCRIBE_FILE)
                    if (autocompleteFile.isFile) {
                        writeToZip(out, dataBuffer, subscriptionFile, BOOKS_SUBSCRIBE_BACKUP_NAME)
                    }
                    subscriptionFile =
                        File(App.instance.filesDir, MyFileReader.AUTHORS_SUBSCRIBE_FILE)
                    if (autocompleteFile.isFile) {
                        writeToZip(out, dataBuffer, subscriptionFile, AUTHORS_SUBSCRIBE_BACKUP_NAME)
                    }
                    subscriptionFile =
                        File(App.instance.filesDir, MyFileReader.SEQUENCES_SUBSCRIBE_FILE)
                    if (autocompleteFile.isFile) {
                        writeToZip(
                            out,
                            dataBuffer,
                            subscriptionFile,
                            SEQUENCES_SUBSCRIBE_BACKUP_NAME
                        )
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
                        val f1 = File(sCompatSaveDir, "downloaded_books")
                        val writer = FileWriter(f1)
                        writer.append(text)
                        writer.flush()
                        writer.close()
                        writeToZip(out, dataBuffer, f1, DOWNLOADED_BOOKS_BACKUP_NAME)
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
                        val f1 = File(sCompatSaveDir, "readed_books")
                        val writer = FileWriter(f1)
                        writer.append(text)
                        writer.flush()
                        writer.close()
                        writeToZip(out, dataBuffer, f1, READED_BOOKS_BACKUP_NAME)
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
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return Result.success()
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
                while (origin.read(dataBuffer, 0, BUFFER).also { count = it } != -1) {
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

    companion object {
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

        @JvmField
        var sSaveDir: DocumentFile? = null

        @JvmField
        var sBackupFile: DocumentFile? = null

        @JvmField
        var sCompatSaveDir: File? = null

        @JvmField
        var sCompatBackupFile: File? = null
    }
}