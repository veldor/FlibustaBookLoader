package net.veldor.flibustaloader.utils

import android.content.Intent
import android.os.Build
import android.widget.Toast
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import java.io.File

object BookOpener {

    fun openBook(name: String) {
        // find the book
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val downloadsDir = PreferencesHandler.instance.getDownloadDir()
            if(downloadsDir != null){
                val targetFile = FilesHandler.find(downloadsDir.listFiles(), name)
                if (targetFile != null && targetFile.isFile) {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(
                        targetFile.uri,
                        MimeTypes.getFullMime(Grammar.getExtension(targetFile.name!!))
                    )
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (intentCanBeHandled(intent)) {
                        App.instance.startActivity(intent)
                    } else {
                        Toast.makeText(
                            App.instance,
                            "Не найдено приложение, открывающее данный файл",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        App.instance,
                        App.instance.getString(R.string.file_not_found_message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            else{
                Toast.makeText(
                    App.instance,
                    "Не распознана папка загрузок, выберите ещё ещё раз в настройках",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            val dd: File = PreferencesHandler.instance.getCompatDownloadDir()!!
            dd.walk().filter { it.name == name }
        }
    }

    @kotlin.jvm.JvmStatic
    fun intentCanBeHandled(intent: Intent): Boolean {
        val packageManager = App.instance.packageManager
        return intent.resolveActivity(packageManager) != null
    }
}