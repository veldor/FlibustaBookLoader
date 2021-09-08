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
    fun shareBook(
        name: String?,
        type: String?,
        authorDir: String?,
        sequenceDir: String?,
        reservedSequenceFolder: String?
    ) {
        var file: File
        // ========================================================================================
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
                val downloadFile = downloadsDir.findFile(name!!)
                if (downloadFile != null) {
                    val docId: String = DocumentsContract.getDocumentId(downloadFile.uri)
                    Log.d("surprise", "BookSharer shareBook $docId")
                    val split = docId.split(":").toTypedArray()
                    val storage = split[0]
                    val path = "///storage/" + storage + "/" + split[1]
                    file = File(path)
                    Log.d("surprise", "BookSharer shareBook $file")
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
                        shareIntent.type = MimeTypes.getFullMime(type)
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                        App.instance.startActivity(
                            Intent.createChooser(
                                shareIntent,
                                App.instance.getString(R.string.send_book_title)
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.file_not_found_message),
                            Toast.LENGTH_LONG
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
                    .isCreateSequencesDir() && reservedSequenceFolder != null
            ) {
                dd = File(dd, reservedSequenceFolder)
            } else {
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
            }
            val bookFile = File(dd, name)
            if (bookFile.isFile) {
                //todo По возможности- разобраться и заменить на валидное решение
                val builder = VmPolicy.Builder()
                StrictMode.setVmPolicy(builder.build())
                // отправлю запрос на открытие файла
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(bookFile))
                shareIntent.type = MimeTypes.getFullMime(type)
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                App.instance.startActivity(
                    Intent.createChooser(
                        shareIntent,
                        App.instance.getString(R.string.send_book_title)
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