package net.veldor.flibustaloader.utils

import android.content.Context
import android.os.Build
import net.veldor.flibustaloader.App
import android.content.Intent
import android.widget.Toast
import net.veldor.flibustaloader.R
import android.net.Uri
import android.util.Log
import java.io.File

object BookOpener {
    @kotlin.jvm.JvmStatic
    fun openBook(
        name: String?,
        type: String?,
        authorDir: String?,
        sequenceDir: String?,
        reservedSequenceFolder: String?
    ) {
        Log.d("surprise", "BookOpener openBook 24: sequence dir is $sequenceDir")
        val context: Context = App.instance
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            var downloadsDir = App.instance.downloadDir
            if (PreferencesHandler.instance
                    .isCreateSequencesDir() && reservedSequenceFolder != null
            ) {
                if (PreferencesHandler.instance.isCreateAdditionalDir()) {
                    val sequencesDir = downloadsDir!!.findFile("Серии")
                    if (sequencesDir != null && sequencesDir.exists()) {
                        downloadsDir = sequencesDir
                    }
                }
                downloadsDir = downloadsDir!!.findFile(reservedSequenceFolder)
            } else {
                if (PreferencesHandler.instance
                        .isCreateAuthorsDir() && authorDir != null && authorDir.isNotEmpty()
                ) {
                    val sequencesDir = downloadsDir!!.findFile("Авторы")
                    if (sequencesDir != null && sequencesDir.exists()) {
                        downloadsDir = sequencesDir
                    }
                    downloadsDir = downloadsDir.findFile(authorDir)
                }
                if (PreferencesHandler.instance
                        .isCreateSequencesDir() && downloadsDir != null && sequenceDir != null && sequenceDir.isNotEmpty()
                ) {
                    downloadsDir = downloadsDir.findFile(sequenceDir)
                }
            }
            if (downloadsDir != null) {
                val file = downloadsDir.findFile(name!!)
                if (file != null) {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(file.uri, MimeTypes.getFullMime(type))
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
                        context,
                        context.getString(R.string.file_not_found_message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            var dd: File? = PreferencesHandler.instance.compatDownloadDir()
            if (PreferencesHandler.instance
                    .isCreateAuthorsDir() && authorDir != null && authorDir.isNotEmpty()
            ) {
                dd = File(dd, authorDir)
            }
            if (PreferencesHandler.instance
                    .isCreateSequencesDir() && sequenceDir != null && sequenceDir.isNotEmpty()
            ) {
                dd = File(dd, sequenceDir)
            }
            val file = File(dd, name)
            if (file.isFile) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(Uri.fromFile(file), MimeTypes.getFullMime(type))
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                App.instance.startActivity(
                    Intent.createChooser(
                        intent,
                        App.instance.getString(R.string.open_with_menu_item)
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } else {
                Toast.makeText(
                    context,
                    context.getString(R.string.file_not_found_message),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    @kotlin.jvm.JvmStatic
    fun intentCanBeHandled(intent: Intent): Boolean {
        val packageManager = App.instance.packageManager
        return intent.resolveActivity(packageManager) != null
    }
}