package net.veldor.flibustaloader.workers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.work.Worker
import androidx.work.WorkerParameters
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.utils.FilesHandler.shareFile
import net.veldor.flibustaloader.utils.ZipManager
import java.io.File


class SendLogWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
// создам временный файл
        val outputDir =
            File(Environment.getExternalStorageDirectory().absolutePath)
        if (outputDir.mkdirs() || outputDir.isDirectory) {
            val outputFile = File(outputDir, "flibusta_log.zip")
            // получу список файлов из папки логов
            val logDir =
                File(Environment.getExternalStorageDirectory().absolutePath + "/flibusta_logcat/")
            if (logDir.mkdirs() || logDir.isDirectory) {
                val existentFiles = logDir.listFiles()
                val zipManager = ZipManager()
                zipManager.zip(existentFiles, outputFile)
                if(outputFile.exists() && outputFile.isFile && outputFile.length() > 0){
                    val fileUri: Uri? = try {
                        FileProvider.getUriForFile(
                            App.instance,
                            "net.veldor.flibustaloader.provider",
                            outputFile)
                    } catch (e: IllegalArgumentException) {
                        Log.e("File Selector",
                            "The selected file can't be shared: $outputFile")
                        null
                    }
                    // отправлю запрос на открытие файла
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
                    shareIntent.type = "application/zip"
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    App.instance.startActivity(
                        Intent.createChooser(
                            shareIntent,
                            "Отправить лог"
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                else{
                    Toast.makeText(App.instance, "Не удалось сохранить лог-файл", Toast.LENGTH_SHORT).show()
                }
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
