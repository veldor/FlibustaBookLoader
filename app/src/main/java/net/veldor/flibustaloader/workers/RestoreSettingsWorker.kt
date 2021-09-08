package net.veldor.flibustaloader.workers

import net.veldor.flibustaloader.utils.XMLHandler.handleBackup
import net.veldor.flibustaloader.App
import android.os.Environment
import android.content.Context
import android.os.Build
import net.veldor.flibustaloader.utils.MyFileReader
import android.net.Uri
import android.util.Log
import androidx.work.*
import net.veldor.flibustaloader.utils.MyFileReader.AUTHORS_SUBSCRIBE_FILE
import net.veldor.flibustaloader.utils.MyFileReader.BOOKS_SUBSCRIBE_FILE
import net.veldor.flibustaloader.utils.MyFileReader.SEQUENCES_SUBSCRIBE_FILE
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class RestoreSettingsWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        try {
            // получу дату
            val data = inputData
            val uriString = data.getString(URI)
            if (uriString != null && uriString.isNotEmpty()) {
                val uri = Uri.parse(uriString)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    val fileData = App.instance.contentResolver.openInputStream(uri)
                    val zin = ZipInputStream(fileData)
                    var ze: ZipEntry
                    var targetFile: File
                    while (zin.nextEntry.also { ze = it } != null) {
                        Log.d("surprise", "doWork: found file " + ze.name)
                        when (ze.name) {
                            ReserveSettingsWorker.PREF_BACKUP_NAME -> {
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
                            ReserveSettingsWorker.AUTOFILL_BACKUP_NAME -> {
                                targetFile = File(
                                    App.instance.filesDir,
                                    MyFileReader.SEARCH_AUTOCOMPLETE_FILE
                                )
                                extractFromZip(zin, targetFile)
                            }
                            ReserveSettingsWorker.BLACKLIST_BOOKS_BACKUP_NAME -> {
                                targetFile = File(
                                    App.instance.filesDir,
                                    MyFileReader.BOOKS_BLACKLIST_FILE
                                )
                                extractFromZip(zin, targetFile)
                                targetFile = File(
                                    App.instance.filesDir,
                                    MyFileReader.AUTHORS_BLACKLIST_FILE
                                )
                                extractFromZip(zin, targetFile)
                                targetFile = File(
                                    App.instance.filesDir,
                                    MyFileReader.GENRES_BLACKLIST_FILE
                                )
                                extractFromZip(zin, targetFile)
                                targetFile = File(
                                    App.instance.filesDir,
                                    MyFileReader.SEQUENCES_BLACKLIST_FILE
                                )
                                extractFromZip(zin, targetFile)
                            }
                            ReserveSettingsWorker.BLACKLIST_AUTHORS_BACKUP_NAME -> {
                                targetFile = File(
                                    App.instance.filesDir,
                                    MyFileReader.AUTHORS_BLACKLIST_FILE
                                )
                                extractFromZip(zin, targetFile)
                                targetFile = File(
                                    App.instance.filesDir,
                                    MyFileReader.GENRES_BLACKLIST_FILE
                                )
                                extractFromZip(zin, targetFile)
                                targetFile = File(
                                    App.instance.filesDir,
                                    MyFileReader.SEQUENCES_BLACKLIST_FILE
                                )
                                extractFromZip(zin, targetFile)
                            }
                            ReserveSettingsWorker.BLACKLIST_GENRES_BACKUP_NAME -> {
                                targetFile = File(
                                    App.instance.filesDir,
                                    MyFileReader.GENRES_BLACKLIST_FILE
                                )
                                extractFromZip(zin, targetFile)
                                targetFile = File(
                                    App.instance.filesDir,
                                    MyFileReader.SEQUENCES_BLACKLIST_FILE
                                )
                                extractFromZip(zin, targetFile)
                            }
                            ReserveSettingsWorker.BLACKLIST_SEQUENCES_BACKUP_NAME -> {
                                targetFile = File(
                                    App.instance.filesDir,
                                    MyFileReader.SEQUENCES_BLACKLIST_FILE
                                )
                                extractFromZip(zin, targetFile)
                            }
                            ReserveSettingsWorker.BOOKS_SUBSCRIBE_BACKUP_NAME -> {
                                targetFile = File(App.instance.filesDir, BOOKS_SUBSCRIBE_FILE)
                                extractFromZip(zin, targetFile)
                            }
                            ReserveSettingsWorker.AUTHORS_SUBSCRIBE_BACKUP_NAME -> {
                                targetFile =
                                    File(App.instance.filesDir, AUTHORS_SUBSCRIBE_FILE)
                                extractFromZip(zin, targetFile)
                            }
                            ReserveSettingsWorker.SEQUENCES_SUBSCRIBE_BACKUP_NAME -> {
                                targetFile =
                                    File(App.instance.filesDir, SEQUENCES_SUBSCRIBE_FILE)
                                extractFromZip(zin, targetFile)
                            }
                            ReserveSettingsWorker.DOWNLOADED_BOOKS_BACKUP_NAME, ReserveSettingsWorker.READED_BOOKS_BACKUP_NAME, ReserveSettingsWorker.BOOKMARKS_BACKUP_NAME, ReserveSettingsWorker.DOWNLOAD_SCHEDULE_BACKUP_NAME ->                                 // преобразую файл из XML в массив значений
                                handleBackup(zin)
                        }
                    }
                    zin.close()
                    return Result.success()
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return Result.failure()
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

    companion object {
        const val URI = "uri"
    }
}