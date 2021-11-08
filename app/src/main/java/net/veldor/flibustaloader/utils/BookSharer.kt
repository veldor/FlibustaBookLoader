package net.veldor.flibustaloader.utils

import android.content.Context
import android.os.Build
import net.veldor.flibustaloader.App
import android.content.Intent
import android.widget.Toast
import net.veldor.flibustaloader.R
import android.provider.DocumentsContract
import android.os.Environment
import android.os.StrictMode.VmPolicy
import android.os.StrictMode
import android.net.Uri
import android.util.Log
import java.io.File

object BookSharer {


    fun shareBook(name: String) {
        // find the book
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val downloadsDir = PreferencesHandler.instance.getDownloadDir()
            if(downloadsDir != null){
                val targetFile = FilesHandler.find(downloadsDir.listFiles(), name)
                if(targetFile != null){
                    // найден файл, делюсь им
                    val docId: String = DocumentsContract.getDocumentId(targetFile.uri)
                    val split = docId.split(":").toTypedArray()
                    val storage = split[0]
                    val path = "///storage/" + storage + "/" + split[1]
                    var file = File(path)
                    // костыли, проверю существование файла с условием, что он находится на основной флешке
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
                        shareIntent.type = MimeTypes.getFullMime(Grammar.getExtension(name))
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                        App.instance.startActivity(
                            Intent.createChooser(
                                shareIntent,
                                App.instance.getString(R.string.send_book_title)
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } else {
                        Toast.makeText(
                            App.instance,
                            App.instance.getString(R.string.file_not_found_message),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            else{
                Toast.makeText(
                    App.instance,
                    "Не распознана папка загрузок, выберите ещё ещё раз в настройках",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        else{
            val dd: File = PreferencesHandler.instance.compatDownloadDir!!
            dd.walk().filter { it.name == name }
        }
    }

    @kotlin.jvm.JvmStatic
    fun shareLink(link: String?) {
        val context: Context = App.instance
        val i = Intent(Intent.ACTION_SEND)
        i.type = "text/plain"
        i.putExtra(Intent.EXTRA_SUBJECT, R.string.share_url_message)
        i.putExtra(Intent.EXTRA_TEXT, link)
        val starter = Intent.createChooser(i, context.getString(R.string.share_url_title))
        starter.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (TransportUtils.intentCanBeHandled(starter)) {
            context.startActivity(starter)
        } else {
            Toast.makeText(
                context,
                "Упс, не нашлось приложения, которое могло бы это сделать.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}