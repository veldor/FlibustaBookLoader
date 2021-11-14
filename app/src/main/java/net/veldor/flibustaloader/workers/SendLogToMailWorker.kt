package net.veldor.flibustaloader.workers

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.work.Worker
import androidx.work.WorkerParameters
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.utils.ZipManager
import java.io.File


class SendLogToMailWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
// создам временный файл
        val outputDir =
            File(Environment.getExternalStorageDirectory().absolutePath + "/flibusta_log/")
        if (outputDir.mkdirs() || outputDir.isDirectory) {
            val outputFile = File(outputDir, "flibusta_log.zip")
            // получу список файлов из папки логов
            val logDir =
                File(Environment.getExternalStorageDirectory().absolutePath + "/flibusta_logcat/")
            if (logDir.mkdirs() || logDir.isDirectory) {
                val existentFiles = logDir.listFiles()
                val zipManager = ZipManager()
                zipManager.zip(existentFiles, outputFile)

                applicationContext.sendEmail("somedevf33434@protonmail.com", "Лог с ошибками", "Опишите ошибку тут", outputFile)
                //shareFile(outputFile)
                if (existentFiles.isNotEmpty()) {
                    for (f in existentFiles) {
                        f.delete()
                    }
                }
            }
        }
        return Result.success()
    }
}

fun Context.sendEmail(
    address: String?,
    subject: String?,
    body: String?,
    attachment: File?
) {
    val selectorIntent = Intent(Intent.ACTION_SENDTO)
        .setData("mailto:$address".toUri())
    val emailIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
        selector = selectorIntent
    }
    emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (attachment != null) {
        val fileUri = FileProvider.getUriForFile(
            applicationContext,
            "net.veldor.flibustaloader.provider",  //(use your app signature + ".provider" )
            attachment)
        emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
    }
    val chooser = Intent.createChooser(emailIntent, getString(R.string.send_email))
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(chooser)

}
