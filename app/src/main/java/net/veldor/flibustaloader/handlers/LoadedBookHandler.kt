package net.veldor.flibustaloader.handlers

import android.os.Build
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import cz.msebera.android.httpclient.HttpResponse
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule
import net.veldor.flibustaloader.notificatons.NotificationHandler
import net.veldor.flibustaloader.utils.Grammar
import net.veldor.flibustaloader.utils.MimeTypes
import net.veldor.flibustaloader.utils.PreferencesHandler
import java.io.File
import java.nio.file.Files

class LoadedBookHandler {
    fun saveBook(book: BooksDownloadSchedule, response: HttpResponse, tempFile: File) {
        // получу конечное расположение файла
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //val file = FilesHandler.getDownloadFile(book, response)
            var extensionSet = false
            // try to get file name with extension
            val filenameHeader = response.getLastHeader("Content-Disposition")
            if (filenameHeader != null) {
                val value = filenameHeader.value
                if (value != null) {
                    val extension = Grammar.getExtension(value.replace("\"", ""))
                    // setup extension
                    book.format = MimeTypes.getFullMime(extension)!!
                    book.name = Grammar.changeExtension(book.name, extension)
                    extensionSet = true
                }
            }
            if (!extensionSet) {
                val receivedContentType = response.getLastHeader("Content-Type")
                val trueFormat = receivedContentType.value
                val trueFormatExtension = MimeTypes.getTrueFormatExtension(trueFormat)
                if (trueFormatExtension != null) {
                    book.format = trueFormat
                    book.name = Grammar.changeExtension(book.name, trueFormatExtension)
                }
            }
            var file: DocumentFile?
            var downloadsDir = PreferencesHandler.instance.downloadDir
            // если не нужно создавать папки- просто сохраню книгу в корень
            if (!PreferencesHandler.instance.isCreateSequencesDir() &&
                !PreferencesHandler.instance.isCreateAuthorsDir()
            ) {
                file = downloadsDir?.createFile(book.format, book.name)
                copyBook(tempFile, file)
            } else if (PreferencesHandler.instance.isCreateAuthorsDir() && !PreferencesHandler.instance.isCreateSequencesDir()) {
                Log.d("surprise", "saveBook: save book to author dir")
                if (book.authorDirName.isNotEmpty()) {
                    // создам папку автора при её отсутствии, и сохраню файл туда
                    if (downloadsDir!!.findFile(book.authorDirName) == null) {
                        downloadsDir.createDirectory(book.authorDirName)
                    }
                    file = downloadsDir.findFile(book.authorDirName)!!
                        .createFile(book.format, book.name)
                    copyBook(tempFile, file)
                    Log.d("surprise", "saveBook: book saved")
                } else {
                    file = downloadsDir?.createFile(book.format, book.name)
                    copyBook(tempFile, file)
                }
            } else if (!PreferencesHandler.instance.isCreateAuthorsDir() && PreferencesHandler.instance.isCreateSequencesDir()) {
                // придётся копировать файл в папку каждой серии по отдельности
                if (book.sequenceDirName.isNotEmpty()) {
                    Log.d("surprise", "saveBook: sequence string is ${book.sequenceDirName}")
                    val subDirs = book.sequenceDirName.split("$|$")
                    subDirs.forEach {
                        Log.d("surprise", "saveBook: sequence dir $it")
                        // create dir if not exists and save file to it
                        if (downloadsDir!!.findFile(it) == null) {
                            downloadsDir!!.createDirectory(it)
                        }
                        file = downloadsDir!!.findFile(it)!!
                            .createFile(book.format, book.name)
                        copyBook(tempFile, file)
                    }
                } else {
                    file = downloadsDir?.createFile(book.format, book.name)
                    copyBook(tempFile, file)
                }
            } else if (
                PreferencesHandler.instance.isCreateAuthorsDir() &&
                PreferencesHandler.instance.isCreateSequencesDir()
            ) {
                // если не надо создавать отдельные папки- создам папку автора, в ней- папки серий
                if (PreferencesHandler.instance.isCreateAdditionalDir()) {
                    // создам папку для серий и папку для авторов. Авторов буду качать в авторов, серии- в серии
                    when {
                        book.sequenceDirName.isNotEmpty() -> {
                            if (downloadsDir!!.findFile("Серии") == null) {
                                downloadsDir.createDirectory("Серии")
                            }
                            downloadsDir = downloadsDir.findFile("Серии")
                            val subDirs = book.sequenceDirName.split("$|$")
                            subDirs.forEach {
                                Log.d("surprise", "saveBook: sequence dir $it")
                                // create dir if not exists and save file to it
                                if (downloadsDir!!.findFile(it) == null) {
                                    downloadsDir!!.createDirectory(it)
                                }
                                file = downloadsDir!!.findFile(it)!!
                                    .createFile(book.format, book.name)
                                copyBook(tempFile, file)
                            }
                        }
                        book.authorDirName.isNotEmpty() -> {
                            if (downloadsDir!!.findFile("Авторы") == null) {
                                downloadsDir.createDirectory("Авторы")
                            }
                            downloadsDir = downloadsDir.findFile("Авторы")
                            // создам папку автора при её отсутствии, и сохраню файл туда
                            if (downloadsDir!!.findFile(book.authorDirName) == null) {
                                downloadsDir.createDirectory(book.authorDirName)
                            }
                            file = downloadsDir.findFile(book.authorDirName)!!
                                .createFile(book.format, book.name)
                            copyBook(tempFile, file)
                        }
                        else -> {
                            file = downloadsDir?.createFile(book.format, book.name)
                            copyBook(tempFile, file)
                        }
                    }
                } else {
                    if (book.authorDirName.isNotEmpty()) {
                        if (downloadsDir!!.findFile(book.authorDirName) == null) {
                            downloadsDir.createDirectory(book.authorDirName)
                        }
                        downloadsDir = downloadsDir.findFile(book.authorDirName)
                    }
                    if (book.sequenceDirName.isNotEmpty()) {
                        Log.d("surprise", "saveBook: sequence string is ${book.sequenceDirName}")
                        val subDirs = book.sequenceDirName.split("$|$")
                        subDirs.forEach {
                            Log.d("surprise", "saveBook: sequence dir $it")
                            // create dir if not exists and save file to it
                            if (downloadsDir!!.findFile(it) == null) {
                                downloadsDir!!.createDirectory(it)
                            }
                            file = downloadsDir!!.findFile(it)!!
                                .createFile(book.format, book.name)
                            copyBook(tempFile, file)
                        }
                    } else {
                        file = downloadsDir?.createFile(book.format, book.name)
                        copyBook(tempFile, file)
                    }
                }
            }
            // create book loaded notification
            NotificationHandler.instance.sendLoadedBookNotification(book.name)
        }
        // COMPAT BLOCK =========================
        else {
            var extensionSet = false
            // try to get file name with extension
            val filenameHeader = response.getLastHeader("Content-Disposition")
            if (filenameHeader != null) {
                val value = filenameHeader.value
                if (value != null) {
                    val extension = Grammar.getExtension(value.replace("\"", ""))
                    // setup extension
                    book.format = MimeTypes.getFullMime(extension)!!
                    book.name = Grammar.changeExtension(book.name, extension)
                    extensionSet = true
                }
            }
            if (!extensionSet) {
                val receivedContentType = response.getLastHeader("Content-Type")
                val trueFormat = receivedContentType.value
                val trueFormatExtension = MimeTypes.getTrueFormatExtension(trueFormat)
                if (trueFormatExtension != null) {
                    book.format = trueFormat
                    book.name = Grammar.changeExtension(book.name, trueFormatExtension)
                }
            }

            var file: File?
            var downloadsDir = PreferencesHandler.instance.compatDownloadDir
            // если не нужно создавать папки- просто сохраню книгу в корень
            if (!PreferencesHandler.instance.isCreateSequencesDir() &&
                !PreferencesHandler.instance.isCreateAuthorsDir()
            ) {
                file = File(downloadsDir, book.name)
                copyBook(tempFile, file)
            } else if (PreferencesHandler.instance.isCreateAuthorsDir() && !PreferencesHandler.instance.isCreateSequencesDir()) {
                Log.d("surprise", "saveBook: save book to author dir")
                if (book.authorDirName.isNotEmpty()) {
                    // создам папку автора при её отсутствии, и сохраню файл туда
                    if (!File(downloadsDir, book.authorDirName).isDirectory) {
                        File(downloadsDir, book.authorDirName).mkdir()
                    }
                    downloadsDir = File(downloadsDir, book.authorDirName)
                    file = File(downloadsDir, book.name)
                    copyBook(tempFile, file)
                    Log.d("surprise", "saveBook: book saved")
                } else {
                    file = File(downloadsDir, book.name)
                    copyBook(tempFile, file)
                }
            } else if (!PreferencesHandler.instance.isCreateAuthorsDir() && PreferencesHandler.instance.isCreateSequencesDir()) {
                // придётся копировать файл в папку каждой серии по отдельности
                if (book.sequenceDirName.isNotEmpty()) {
                    Log.d("surprise", "saveBook: sequence string is ${book.sequenceDirName}")
                    val subDirs = book.sequenceDirName.split("$|$")
                    subDirs.forEach {
                        Log.d("surprise", "saveBook: sequence dir $it")
                        // create dir if not exists and save file to it
                        if (!File(downloadsDir, it).isDirectory) {
                            File(downloadsDir, it).mkdir()
                        }
                        downloadsDir = File(downloadsDir, it)
                        file = File(downloadsDir, book.name)
                        copyBook(tempFile, file)
                        downloadsDir = PreferencesHandler.instance.compatDownloadDir
                    }
                } else {
                    file = File(downloadsDir, book.name)
                    copyBook(tempFile, file)
                }
            } else if (
                PreferencesHandler.instance.isCreateAuthorsDir() &&
                PreferencesHandler.instance.isCreateSequencesDir()
            ) {
                // если не надо создавать отдельные папки- создам папку автора, в ней- папки серий
                if (PreferencesHandler.instance.isCreateAdditionalDir()) {
                    // создам папку для серий и папку для авторов. Авторов буду качать в авторов, серии- в серии
                    when {
                        book.sequenceDirName.isNotEmpty() -> {
                            if (!File(downloadsDir, "Серии").isDirectory) {
                                File(downloadsDir, "Серии").mkdir()
                            }
                            downloadsDir = File(downloadsDir, "Серии")


                            val subDirs = book.sequenceDirName.split("$|$")
                            subDirs.forEach {
                                if (!File(downloadsDir, it).isDirectory) {
                                    File(downloadsDir, it).mkdir()
                                }
                                downloadsDir = File(downloadsDir, it)
                                file = File(downloadsDir, book.name)
                                copyBook(tempFile, file)
                                downloadsDir = downloadsDir!!.parentFile
                            }
                        }
                        book.authorDirName.isNotEmpty() -> {
                            if (!File(downloadsDir, "Авторы").isDirectory) {
                                File(downloadsDir, "Авторы").mkdir()
                            }
                            downloadsDir = File(downloadsDir, "Авторы")
                            if (!File(downloadsDir, book.authorDirName).isDirectory) {
                                File(downloadsDir, book.authorDirName).mkdir()
                            }
                            downloadsDir = File(downloadsDir, book.authorDirName)
                            file = File(downloadsDir, book.name)
                            copyBook(tempFile, file)
                        }
                        else -> {
                            file = File(downloadsDir, book.name)
                            copyBook(tempFile, file)
                        }
                    }
                } else {
                    if (!File(downloadsDir, book.authorDirName).isDirectory) {
                        File(downloadsDir, book.authorDirName).mkdir()
                    }
                    downloadsDir = File(downloadsDir, book.authorDirName)
                    if (book.sequenceDirName.isNotEmpty()) {
                        Log.d("surprise", "saveBook: sequence string is ${book.sequenceDirName}")
                        val subDirs = book.sequenceDirName.split("$|$")
                        subDirs.forEach {
                            if (!File(downloadsDir, it).isDirectory) {
                                File(downloadsDir, it).mkdir()
                            }
                            downloadsDir = File(downloadsDir, it)
                            file = File(downloadsDir, book.name)
                            copyBook(tempFile, file)
                            downloadsDir = downloadsDir!!.parentFile
                        }
                    } else {
                        file = File(downloadsDir, book.name)
                        copyBook(tempFile, file)
                    }
                }
            }
            // create book loaded notification
            NotificationHandler.instance.sendLoadedBookNotification(book.name)
        }
    }

    private fun copyBook(tempFile: File, file: DocumentFile?) {
        if (file != null) {
            val uri = file.uri
            val stream = App.instance.contentResolver.openOutputStream(uri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Files.copy(tempFile.toPath(), stream)
            } else {
                tempFile.inputStream().copyTo(stream!!)
            }
            stream?.flush()
            stream?.close()
        }
    }

    private fun copyBook(tempFile: File, file: File?) {
        if (file != null) {
            tempFile.copyTo(file, true)
        }
    }
}