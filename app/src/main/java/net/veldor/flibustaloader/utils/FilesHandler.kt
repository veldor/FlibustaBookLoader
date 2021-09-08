package net.veldor.flibustaloader.utils

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.provider.DocumentsContract
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import cz.msebera.android.httpclient.HttpResponse
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

object FilesHandler {
    fun shareFile(zip: File) {
        //todo По возможности- разобраться и заменить на валидное решение
        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        // отправлю запрос на открытие файла
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(zip))
        shareIntent.type = getMimeType(zip.name)
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        App.instance.startActivity(
            Intent.createChooser(
                shareIntent,
                App.instance.getString(R.string.send_book_title)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    @kotlin.jvm.JvmStatic
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    fun shareFile(incomingDocumentFile: DocumentFile) {
        val mime = getMimeType(incomingDocumentFile.name)
        if (mime != null && mime.isNotEmpty()) {
            if (mime != "application/x-mobipocket-ebook") {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.putExtra(Intent.EXTRA_STREAM, incomingDocumentFile.uri)
                shareIntent.type = getMimeType(incomingDocumentFile.name)
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                App.instance.startActivity(
                    Intent.createChooser(
                        shareIntent,
                        App.instance.getString(R.string.send_book_title)
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } else {
                val docId = DocumentsContract.getDocumentId(incomingDocumentFile.uri)
                val split = docId.split(":").toTypedArray()
                val storage = split[0]
                // получу файл из documentFile и отправлю его
                var file = getFileFromDocumentFile(incomingDocumentFile)
                if (file != null) {
                    if (!file.exists()) {
                        file = File(
                            Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                        )
                    }
                    if (file.exists()) {
                        //todo По возможности- разобраться и заменить на валидное решение
                        val builder = VmPolicy.Builder()
                        StrictMode.setVmPolicy(builder.build())
                        // отправлю запрос на открытие файла
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
                        shareIntent.type = mime
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                        App.instance.startActivity(
                            Intent.createChooser(
                                shareIntent,
                                App.instance.getString(R.string.send_book_title)
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            }
        }
    }

    private fun getFileFromDocumentFile(df: DocumentFile): File? {
        val docId: String
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            docId = DocumentsContract.getDocumentId(df.uri)
            val split = docId.split(":").toTypedArray()
            val storage = split[0]
            val path = "///storage/" + storage + "/" + split[1]
            return File(path)
        }
        return null
    }

    // url = file path or whatever suitable URL you want.
    private fun getMimeType(url: String?): String? {
        if (url != null && url.isNotEmpty()) {
            val extension = url.substring(url.lastIndexOf(".") + 1)
            return MimeTypes.getFullMime(extension)
        }
        return null
    }

    @kotlin.jvm.JvmStatic
    fun getChangeText(): String {
        try {
            val textFileStream = App.instance.assets.open("   changes.txt")
            val r = BufferedReader(InputStreamReader(textFileStream))
            val total = StringBuilder()
            var line: String?
            while (r.readLine().also { line = it } != null) {
                total.append('\u25CF').append(' ').append(line).append("\n\n")
            }
            return total.toString()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return "no changes"
    }

    @kotlin.jvm.JvmStatic
    fun getDownloadFile(book: BooksDownloadSchedule, response: HttpResponse): DocumentFile {
        var extensionSet = false

//        Header[] headers = response.getAllHeaders();
//        for (Header h :
//                headers) {
//            Log.d("surprise", "FilesHandler: 127 " + h.getName());
//            Log.d("surprise", "FilesHandler: 127 " + h.getValue());
//        }

        // try to get file name with extension
        val filenameHeader = response.getLastHeader("Content-Disposition")
        if (filenameHeader != null) {
            val value = filenameHeader.value
            if (value != null) {
                val extension = Grammar.getExtension(value.replace("\"", ""))
                // setup extension
                book.format = MimeTypes.getFullMime(extension)!!
                book.name = Grammar.changeExtension(book.name, extension)
                //                Log.d("surprise", "*******FilesHandler: v1 EXTENSION " + extension);
//                Log.d("surprise", "*******FilesHandler: v1 MIME " + book.format);
//                Log.d("surprise", "*******FilesHandler: v1 CONVERTED NAME " + book.name);
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
                //                Log.d("surprise", "*******FilesHandler: v2 EXTENSION " + trueFormatExtension);
//                Log.d("surprise", "*******FilesHandler: v2 MIME " + book.format);
//                Log.d("surprise", "*******FilesHandler: v2 CONVERTED NAME " + book.name);
            }
        }
        // получу имя файла
        var downloadsDir = App.instance.downloadDir
        if (PreferencesHandler.instance
                .isCreateSequencesDir() && book.reservedSequenceName.isNotEmpty()
        ) {
            if (PreferencesHandler.instance.isCreateAdditionalDir()) {
                val seriesDir = downloadsDir!!.findFile("Серии")
                downloadsDir = if (seriesDir == null || !seriesDir.exists()) {
                    downloadsDir.createDirectory("Серии")
                } else {
                    seriesDir
                }
            }
            if (downloadsDir != null && downloadsDir.findFile(book.reservedSequenceName) == null) {
                downloadsDir = downloadsDir.createDirectory(book.reservedSequenceName)
            } else if (downloadsDir != null) {
                downloadsDir = downloadsDir.findFile(book.reservedSequenceName)
            }
            if (downloadsDir == null) {
                downloadsDir = App.instance.downloadDir
            }
        } else {
            // проверю, нужно ли создавать папку под автора
            if (PreferencesHandler.instance.isCreateAuthorsDir()) {
                if (PreferencesHandler.instance.isCreateAdditionalDir()) {
                    val authorsDir = downloadsDir!!.findFile("Авторы")
                    downloadsDir = if (authorsDir == null || !authorsDir.exists()) {
                        downloadsDir.createDirectory("Авторы")
                    } else {
                        authorsDir
                    }
                }
                // создам папку
                if (downloadsDir != null && downloadsDir.findFile(book.authorDirName) == null) {
                    downloadsDir = downloadsDir.createDirectory(book.authorDirName)
                } else if (downloadsDir != null) {
                    downloadsDir = downloadsDir.findFile(book.authorDirName)
                }
                if (downloadsDir == null) {
                    downloadsDir = App.instance.downloadDir
                }
            }
            if (PreferencesHandler.instance
                    .isCreateSequencesDir() && book.sequenceDirName.isNotEmpty() && book.sequenceDirName.isNotEmpty()
            ) {
                downloadsDir = if (downloadsDir!!.findFile(book.sequenceDirName) == null) {
                    downloadsDir.createDirectory(book.sequenceDirName)
                } else {
                    downloadsDir.findFile(book.sequenceDirName)
                }
                if (downloadsDir == null) {
                    downloadsDir = App.instance.downloadDir
                }
            }
        }
        // проверю, нет ли ещё файла с таким именем, если есть- удалю
        val files = downloadsDir!!.listFiles()
        var fileCounter = 0
        var oneFile: DocumentFile
        if (files.isNotEmpty()) {
            while (fileCounter < files.size - 1) {
                fileCounter++
                oneFile = files[fileCounter]
                if (oneFile.isFile) {
                    if (oneFile.name!!.startsWith(book.name)) {
                        oneFile.delete()
                    }
                }
            }
        }
        val existentFile = downloadsDir.findFile(book.name)
        existentFile?.delete()
        val file = downloadsDir.createFile(book.format, book.name)
        Log.d("surprise", "FilesHandler: 217 created file " + file!!.name)
        return file
    }

    @kotlin.jvm.JvmStatic
    fun getCompatDownloadFile(book: BooksDownloadSchedule): File {
        var file: File? = PreferencesHandler.instance.compatDownloadDir()
        // проверю, нужно ли создавать папку под автора
        if (PreferencesHandler.instance
                .isCreateSequencesDir() && book.reservedSequenceName.isNotEmpty()
        ) {
            file = File(file, book.reservedSequenceName)
        } else {
            if (PreferencesHandler.instance.isCreateAuthorsDir()) {
                file = File(file, book.authorDirName)
            }
            if (PreferencesHandler.instance
                    .isCreateSequencesDir() && book.sequenceDirName.isNotEmpty()
            ) {
                file = File(file, book.sequenceDirName)
            }
        }
        return File(file, book.name)
    }

    @kotlin.jvm.JvmStatic
    fun getBaseDownloadFile(book: BooksDownloadSchedule): File {
        val file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(file, book.name)
    }

    @kotlin.jvm.JvmStatic
    fun openFile(file: DocumentFile) {
        val mime = getMimeType(file.name)
        if (mime != null) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(file.uri, mime)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            App.instance.startActivity(
                Intent.createChooser(
                    intent,
                    App.instance.getString(R.string.open_with_menu_item)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun isBookDownloaded(newBook: BooksDownloadSchedule): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            var downloadsDir = App.instance.downloadDir
            if (PreferencesHandler.instance
                    .isCreateSequencesDir() && newBook.reservedSequenceName.isNotEmpty()
            ) {
                if (PreferencesHandler.instance.isCreateAdditionalDir()) {
                    val sequencesDir = downloadsDir!!.findFile("Серии")
                    if (sequencesDir != null && sequencesDir.exists()) {
                        downloadsDir = sequencesDir
                    }
                }
                downloadsDir = downloadsDir!!.findFile(newBook.reservedSequenceName)
            } else {
                if (PreferencesHandler.instance
                        .isCreateAuthorsDir() && newBook.authorDirName.isNotEmpty() && newBook.authorDirName.isNotEmpty()
                ) {
                    val sequencesDir = downloadsDir!!.findFile("Авторы")
                    if (sequencesDir != null && sequencesDir.exists()) {
                        downloadsDir = sequencesDir
                    }
                    downloadsDir = downloadsDir.findFile(newBook.authorDirName)
                }
                if (PreferencesHandler.instance
                        .isCreateSequencesDir() && downloadsDir != null && newBook.sequenceDirName.isNotEmpty() && newBook.reservedSequenceName.isNotEmpty() && !newBook.reservedSequenceName.isEmpty()
                ) {
                    downloadsDir = downloadsDir.findFile(newBook.sequenceDirName)
                }
            }
            if (downloadsDir != null) {
                val file = downloadsDir.findFile(newBook.name)
                if (file != null && file.isFile && file.canRead() && file.length() > 0) {
                    return true
                }
            }
        } else {
            var dd: File? = PreferencesHandler.instance.compatDownloadDir()
            if (PreferencesHandler.instance
                    .isCreateAuthorsDir() && newBook.authorDirName.isNotEmpty() && newBook.authorDirName.isNotEmpty()
            ) {
                dd = File(dd, newBook.authorDirName)
            }
            if (PreferencesHandler.instance
                    .isCreateSequencesDir() && newBook.sequenceDirName.isNotEmpty() && newBook.sequenceDirName.isNotEmpty()
            ) {
                dd = File(dd, newBook.sequenceDirName)
            }
            val file = File(dd, newBook.name)
            if (file.isFile && file.canRead() && file.length() > 0) {
                return true
            }
        }
        return false
    }
}