package net.veldor.flibustaloader.utils

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.provider.DocumentsContract
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

object FilesHandler {

    @kotlin.jvm.JvmStatic
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    fun shareFile(incomingDocumentFile: DocumentFile) {
        shareFile(
                incomingDocumentFile,
                App.instance.getString(R.string.send_book_title)
        )
    }

    @kotlin.jvm.JvmStatic
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    fun shareFile(file: DocumentFile, chooserPromise: String) {
        val mime = getMimeType(file.name)
        if (mime != null && mime.isNotEmpty()) {
            if (mime != "application/x-mobipocket-ebook") {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.putExtra(Intent.EXTRA_STREAM, file.uri)
                shareIntent.type = getMimeType(file.name)
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                App.instance.startActivity(
                        Intent.createChooser(
                                shareIntent,
                                chooserPromise
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } else {
                val docId = DocumentsContract.getDocumentId(file.uri)
                val split = docId.split(":").toTypedArray()
                // получу файл из documentFile и отправлю его
                var file1 = getFileFromDocumentFile(file)
                if (file1 != null) {
                    if (!file1.exists()) {
                        file1 = File(
                                Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                        )
                    }
                    if (file1.exists()) {
                        //todo По возможности- разобраться и заменить на валидное решение
                        val builder = VmPolicy.Builder()
                        StrictMode.setVmPolicy(builder.build())
                        // отправлю запрос на открытие файла
                        val shareIntent = Intent(Intent.ACTION_SEND)
                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file1))
                        shareIntent.type = mime
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                        App.instance.startActivity(
                                Intent.createChooser(
                                        shareIntent,
                                        chooserPromise
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
            val textFileStream = App.instance.assets.open("changes.txt")
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

    fun find(files: Array<DocumentFile>, name: String): DocumentFile? {
        for (df in files) {
            if (df.isFile && df.name == name) {
                return df
            } else if (df.isDirectory) {
                val result = find(df.listFiles(), name)
                if (result != null) {
                    return result
                }
            }
        }
        return null
    }


    fun shareFile(zip: File) {
        shareFile(zip, App.instance.getString(R.string.send_book_title))
    }

    fun shareFile(file: File, chooserPromise: String) {
        //todo По возможности- разобраться и заменить на валидное решение
        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        // отправлю запрос на открытие файла
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
        shareIntent.type = getMimeType(file.name)
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        App.instance.startActivity(
                Intent.createChooser(
                        shareIntent,
                        chooserPromise
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }


    /*   fun isBookDownloaded(newBook: BooksDownloadSchedule): Boolean {
           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
               var downloadsDir = PreferencesHandler.instance.downloadDir
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
                           .isCreateSequencesDir() && downloadsDir != null && newBook.sequenceDirName.isNotEmpty() && newBook.reservedSequenceName.isNotEmpty() && newBook.reservedSequenceName.isNotEmpty()
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
               var dd: File? = PreferencesHandler.instance.compatDownloadDir
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
       }*/
}