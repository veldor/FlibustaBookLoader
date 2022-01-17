package net.veldor.flibustaloader.handlers

import android.os.Build
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule
import net.veldor.flibustaloader.ecxeptions.DownloadsDirNotFoundException
import net.veldor.flibustaloader.http.WebResponse
import net.veldor.flibustaloader.notificatons.NotificationHandler
import net.veldor.flibustaloader.utils.Grammar
import net.veldor.flibustaloader.utils.MimeTypes
import net.veldor.flibustaloader.utils.PreferencesHandler
import java.io.File
import java.nio.file.Files

class LoadedBookHandler {
    fun saveBook(book: BooksDownloadSchedule, response: WebResponse, tempFile: File) {
        //val file = FilesHandler.getDownloadFile(book, response)
        var extensionSet = false
        // try to get file name with extension
        val filenameHeader = response.headers!!["Content-Disposition"]
        if (filenameHeader != null) {
            val extension = Grammar.getExtension(filenameHeader.replace("\"", ""))
            // setup extension
            book.format = MimeTypes.getFullMime(extension)!!
            book.name = Grammar.changeExtension(book.name, extension)
            extensionSet = true
        }
        if (!extensionSet) {
            val receivedContentType = response.headers["Content-Type"]!!
            val trueFormatExtension = MimeTypes.getTrueFormatExtension(receivedContentType)
            if (trueFormatExtension != null) {
                book.format = receivedContentType
                book.name = Grammar.changeExtension(book.name, trueFormatExtension)
            }
        }
        // получу конечное расположение файла
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            var downloadsDir = PreferencesHandler.instance.getDownloadDir()
            if (downloadsDir == null || !downloadsDir.isDirectory || !downloadsDir.canWrite()) {
                throw DownloadsDirNotFoundException()
            }
            if (!PreferencesHandler.instance.isCreateSequencesDir() &&
                !PreferencesHandler.instance.isCreateAuthorsDir()
            ) {
                //==== папки не нужны, сохраняю в корень
                Log.d("surprise", "saveBook: no additional dirs required, save to root")
                pullBook(downloadsDir, book, tempFile)
            } else if (PreferencesHandler.instance.isCreateAuthorsDir()
                && !PreferencesHandler.instance.isCreateSequencesDir()
            ) {
                //==== создаю только папку автора
                if (PreferencesHandler.instance.isDifferentDirForAuthorAndSequence()) {
                    downloadsDir = appendDir(downloadsDir, "Авторы")
                    Log.d("surprise", "saveBook: save to authors sub-dir")
                }
                if (book.authorDirName.trim().isNotEmpty()) {
                    downloadsDir = appendDir(downloadsDir, book.authorDirName.trim())
                    Log.d("surprise", "saveBook: save to author dir")
                }
                pullBook(downloadsDir, book, tempFile)
            } else if (!PreferencesHandler.instance.isCreateAuthorsDir()
                && PreferencesHandler.instance.isCreateSequencesDir()
            ) {
                //==== создаю только папку серии
                // придётся копировать файл в папку каждой серии по отдельности
                if (book.sequenceDirName.isNotEmpty()) {
                    if (PreferencesHandler.instance.isDifferentDirForAuthorAndSequence()) {
                        downloadsDir = appendDir(downloadsDir, "Серии")
                        Log.d("surprise", "saveBook: save to sequences sub-dir")
                    }
                    val subDirs = book.sequenceDirName.split("$|$")
                    subDirs.forEach {
                        // create dir if not exists and save file to it
                        if (downloadsDir!!.findFile(it.trim()) == null) {
                            downloadsDir!!.createDirectory(it.trim())
                        }
                        pullBook(downloadsDir!!.findFile(it.trim())!!, book, tempFile)
                        Log.d("surprise", "saveBook: save to sequence dir")
                    }
                } else {
                    pullBook(downloadsDir, book, tempFile)
                    Log.d("surprise", "saveBook: no sequence in book, save it in root")
                }
            } else if (
                PreferencesHandler.instance.isCreateAuthorsDir() &&
                PreferencesHandler.instance.isCreateSequencesDir()
            ) {
                // если есть серия
                if (book.sequenceDirName.isNotEmpty()) {
                    // если выбрано сохранение серий внутри папки автора- сохраню внутри
                    if (PreferencesHandler.instance.isLoadSequencesInAuthorDir()) {
                        if (PreferencesHandler.instance.isDifferentDirForAuthorAndSequence()) {
                            if (PreferencesHandler.instance.isDifferentDirForAuthorAndSequence()) {
                                downloadsDir = appendDir(downloadsDir, "Авторы")
                                Log.d("surprise", "saveBook: save to authors sub-dir")
                            }
                        }
                        if (book.authorDirName.trim().isNotEmpty()) {
                            downloadsDir = appendDir(downloadsDir, book.authorDirName.trim())
                            Log.d("surprise", "saveBook: save to author dir")
                        }
                        val subDirs = book.sequenceDirName.split("$|$")
                        subDirs.forEach {
                            // create dir if not exists and save file to it
                            if (downloadsDir!!.findFile(it.trim()) == null) {
                                downloadsDir!!.createDirectory(it.trim())
                            }
                            pullBook(downloadsDir!!.findFile(it.trim())!!, book, tempFile)
                            Log.d("surprise", "saveBook: save to sequence dir")
                        }
                    } else {
                        if (PreferencesHandler.instance.isDifferentDirForAuthorAndSequence()) {
                            if (PreferencesHandler.instance.isDifferentDirForAuthorAndSequence()) {
                                downloadsDir = appendDir(downloadsDir, "Серии")
                                Log.d("surprise", "saveBook: save to authors sub-dir")
                            }
                        }
                        val subDirs = book.sequenceDirName.split("$|$")
                        subDirs.forEach {
                            // create dir if not exists and save file to it
                            if (downloadsDir!!.findFile(it.trim()) == null) {
                                downloadsDir!!.createDirectory(it.trim())
                            }
                            pullBook(downloadsDir!!.findFile(it.trim())!!, book, tempFile)
                            Log.d("surprise", "saveBook: save to sequence dir")
                        }
                    }
                } else {
                    if (PreferencesHandler.instance.isDifferentDirForAuthorAndSequence()) {
                        if (PreferencesHandler.instance.isDifferentDirForAuthorAndSequence()) {
                            downloadsDir = appendDir(downloadsDir, "Авторы")
                            Log.d("surprise", "saveBook: save to authors sub-dir")
                        }
                    }
                    if (book.authorDirName.trim().isNotEmpty()) {
                        downloadsDir = appendDir(downloadsDir, book.authorDirName.trim())
                        Log.d("surprise", "saveBook: save to author dir")
                    }
                    pullBook(downloadsDir, book, tempFile)
                }
            }
            // create book loaded notification
            NotificationHandler.instance.sendLoadedBookNotification(book.name)
        }
        // COMPAT BLOCK =========================
        else {
            var compatDownloadsDir = PreferencesHandler.instance.getCompatDownloadDir()
            if (compatDownloadsDir == null || !compatDownloadsDir.isDirectory || !compatDownloadsDir.canWrite()) {
                throw DownloadsDirNotFoundException()
            }
            if (!PreferencesHandler.instance.isCreateSequencesDir() &&
                !PreferencesHandler.instance.isCreateAuthorsDir()
            ) {
                //==== папки не нужны, сохраняю в корень
                Log.d("surprise", "saveBook: no additional dirs required, save to root")
                pullBook(compatDownloadsDir, book, tempFile)
            } else if (PreferencesHandler.instance.isCreateAuthorsDir()
                && !PreferencesHandler.instance.isCreateSequencesDir()
            ) {
                //==== создаю только папку автора
                if (PreferencesHandler.instance.isDifferentDirForAuthorAndSequence()) {
                    compatDownloadsDir = appendDir(compatDownloadsDir, "Авторы")
                    Log.d("surprise", "saveBook: save to authors sub-dir")
                }
                if (book.authorDirName.trim().isNotEmpty()) {
                    compatDownloadsDir = appendDir(compatDownloadsDir, book.authorDirName.trim())
                    Log.d("surprise", "saveBook: save to author dir")
                }
                pullBook(compatDownloadsDir, book, tempFile)
            } else if (!PreferencesHandler.instance.isCreateAuthorsDir()
                && PreferencesHandler.instance.isCreateSequencesDir()
            ) {
                //==== создаю только папку серии
                // придётся копировать файл в папку каждой серии по отдельности
                if (book.sequenceDirName.isNotEmpty()) {
                    if (PreferencesHandler.instance.isDifferentDirForAuthorAndSequence()) {
                        compatDownloadsDir = appendDir(compatDownloadsDir, "Серии")
                        Log.d("surprise", "saveBook: save to sequences sub-dir")
                    }
                    val subDirs = book.sequenceDirName.split("$|$")
                    subDirs.forEach {
                        // create dir if not exists and save file to it
                        val targetDir = File(compatDownloadsDir, it.trim())
                        if (!targetDir.exists() || !targetDir.isDirectory) {
                            targetDir.mkdir()
                        }
                        pullBook(targetDir, book, tempFile)
                        Log.d("surprise", "saveBook: save to sequence dir")
                    }
                } else {
                    pullBook(compatDownloadsDir, book, tempFile)
                    Log.d("surprise", "saveBook: no sequence in book, save it in root")
                }
            } else if (
                PreferencesHandler.instance.isCreateAuthorsDir() &&
                PreferencesHandler.instance.isCreateSequencesDir()
            ) {
                // если есть серия
                if (book.sequenceDirName.isNotEmpty()) {
                    // если выбрано сохранение серий внутри папки автора- сохраню внутри
                    if (PreferencesHandler.instance.isLoadSequencesInAuthorDir()) {
                        if (PreferencesHandler.instance.isDifferentDirForAuthorAndSequence()) {
                            if (PreferencesHandler.instance.isDifferentDirForAuthorAndSequence()) {
                                compatDownloadsDir = appendDir(compatDownloadsDir, "Авторы")
                                Log.d("surprise", "saveBook: save to authors sub-dir")
                            }
                        }
                        if (book.authorDirName.trim().isNotEmpty()) {
                            compatDownloadsDir =
                                appendDir(compatDownloadsDir, book.authorDirName.trim())
                            Log.d("surprise", "saveBook: save to author dir")
                        }
                        val subDirs = book.sequenceDirName.split("$|$")
                        subDirs.forEach {
                            val targetDir = File(compatDownloadsDir, it.trim())
                            if (!targetDir.exists() || !targetDir.isDirectory) {
                                targetDir.mkdir()
                            }
                            pullBook(targetDir, book, tempFile)
                            Log.d("surprise", "saveBook: save to sequence dir")
                        }
                    } else {
                        if (PreferencesHandler.instance.isDifferentDirForAuthorAndSequence()) {
                            if (PreferencesHandler.instance.isDifferentDirForAuthorAndSequence()) {
                                compatDownloadsDir = appendDir(compatDownloadsDir, "Серии")
                                Log.d("surprise", "saveBook: save to authors sub-dir")
                            }
                        }
                        val subDirs = book.sequenceDirName.split("$|$")
                        subDirs.forEach {
                            val targetDir = File(compatDownloadsDir, it.trim())
                            if (!targetDir.exists() || !targetDir.isDirectory) {
                                targetDir.mkdir()
                            }
                            pullBook(targetDir, book, tempFile)
                            Log.d("surprise", "saveBook: save to sequence dir")
                        }
                    }
                } else {
                    if (PreferencesHandler.instance.isDifferentDirForAuthorAndSequence()) {
                        if (PreferencesHandler.instance.isDifferentDirForAuthorAndSequence()) {
                            compatDownloadsDir = appendDir(compatDownloadsDir, "Авторы")
                            Log.d("surprise", "saveBook: save to authors sub-dir")
                        }
                    }
                    if (book.authorDirName.trim().isNotEmpty()) {
                        compatDownloadsDir =
                            appendDir(compatDownloadsDir, book.authorDirName.trim())
                        Log.d("surprise", "saveBook: save to author dir")
                    }
                    pullBook(compatDownloadsDir, book, tempFile)
                }
            }
            // create book loaded notification
            NotificationHandler.instance.sendLoadedBookNotification(book.name)
        }
    }

    fun getPath(book: BooksDownloadSchedule): String {
        val sb = StringBuffer()
        // получу конечное расположение файла
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val downloadsDir = PreferencesHandler.instance.getDownloadDir()
            if (downloadsDir == null || !downloadsDir.isDirectory || !downloadsDir.canWrite()) {
                return "Не удалось определить папку загрузок, переназначьте!"
            }
        } else {
            val compatDownloadsDir = PreferencesHandler.instance.getCompatDownloadDir()
            if (compatDownloadsDir == null || !compatDownloadsDir.isDirectory || !compatDownloadsDir.canWrite()) {
                return "Не удалось определить папку загрузок, переназначьте!"
            }
        }

        if (!PreferencesHandler.instance.isCreateSequencesDir() &&
            !PreferencesHandler.instance.isCreateAuthorsDir()
        ) {
            //==== папки не нужны, сохраняю в корень
            return "/${book.name}"
        } else if (PreferencesHandler.instance.isCreateAuthorsDir()
            && !PreferencesHandler.instance.isCreateSequencesDir()
        ) {
            sb.append("/")
            //==== создаю только папку автора
            if (PreferencesHandler.instance.isDifferentDirForAuthorAndSequence()) {
                sb.append("Авторы/")
            }
            if (book.authorDirName.trim().isNotEmpty()) {
                sb.append("${book.authorDirName.trim()}/")
            }
            sb.append(book.name)
            return sb.toString()
        } else if (!PreferencesHandler.instance.isCreateAuthorsDir()
            && PreferencesHandler.instance.isCreateSequencesDir()
        ) {
            sb.append("/")
            //==== создаю только папку серии
            // придётся копировать файл в папку каждой серии по отдельности
            if (book.sequenceDirName.isNotEmpty()) {
                if (PreferencesHandler.instance.isDifferentDirForAuthorAndSequence()) {
                    sb.append("Серии/")
                }
                val subDirs = book.sequenceDirName.split("$|$")
                subDirs.forEach {
                    sb.append("${it.trim()}/")
                    // create dir if not exists and save file to it
                    sb.append(book.name)
                    return sb.toString()
                }
            } else {
                sb.append(book.name)
                return sb.toString()
            }
        } else if (
            PreferencesHandler.instance.isCreateAuthorsDir() &&
            PreferencesHandler.instance.isCreateSequencesDir()
        ) {
            // если есть серия
            if (book.sequenceDirName.isNotEmpty()) {
                // если выбрано сохранение серий внутри папки автора- сохраню внутри
                if (PreferencesHandler.instance.isLoadSequencesInAuthorDir()) {
                    if (PreferencesHandler.instance.isDifferentDirForAuthorAndSequence()) {
                        if (PreferencesHandler.instance.isDifferentDirForAuthorAndSequence()) {
                            sb.append("Авторы/")
                        }
                    }
                    if (book.authorDirName.trim().isNotEmpty()) {
                        sb.append("${book.authorDirName.trim()}/")
                    }
                    val subDirs = book.sequenceDirName.split("$|$")
                    subDirs.forEach {
                        sb.append("${it.trim()}/")
                        // create dir if not exists and save file to it
                        sb.append(book.name)
                        return sb.toString()
                    }
                } else {
                    if (PreferencesHandler.instance.isDifferentDirForAuthorAndSequence()) {
                        if (PreferencesHandler.instance.isDifferentDirForAuthorAndSequence()) {
                            sb.append("Серии/")
                        }
                    }
                    val subDirs = book.sequenceDirName.split("$|$")
                    subDirs.forEach {
                        sb.append("${it.trim()}/")
                        // create dir if not exists and save file to it
                        sb.append(book.name)
                        return sb.toString()
                    }
                }
            } else {
                if (PreferencesHandler.instance.isDifferentDirForAuthorAndSequence()) {
                    if (PreferencesHandler.instance.isDifferentDirForAuthorAndSequence()) {
                        sb.append("Авторы/")
                    }
                }
                if (book.authorDirName.trim().isNotEmpty()) {
                    sb.append("${book.authorDirName.trim()}/")
                }
                sb.append(book.name)
                return sb.toString()
            }
        }
        return "Что-то пошло не так, путь к файлу не определён"
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

    private fun appendDir(srcDir: DocumentFile, innerDirName: String): DocumentFile {
        if (srcDir.findFile(innerDirName) == null) {
            srcDir.createDirectory(innerDirName)
        }
        return srcDir.findFile(innerDirName)!!
    }

    private fun appendDir(srcDir: File, innerDirName: String): File {
        val targetDir = File(srcDir, innerDirName)
        if (!targetDir.exists() || !targetDir.isDirectory) {
            targetDir.mkdir()
        }
        return targetDir
    }

    private fun pullBook(dstDir: DocumentFile, book: BooksDownloadSchedule, tempFile: File) {
        val file = dstDir.createFile(book.format, book.name)
        copyBook(tempFile, file)
    }


    private fun pullBook(dstDir: File, book: BooksDownloadSchedule, tempFile: File) {
        val file = File(dstDir, book.name)
        copyBook(tempFile, file)
    }
}