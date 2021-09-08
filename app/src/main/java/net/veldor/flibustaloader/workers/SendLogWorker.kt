package net.veldor.flibustaloader.workers

import net.veldor.flibustaloader.utils.FilesHandler.shareFile
import android.os.Environment
import net.veldor.flibustaloader.utils.ZipManager
import android.content.Context
import androidx.work.*
import java.io.File

class SendLogWorker(context: Context, workerParams: WorkerParameters) :
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
                shareFile(outputFile)
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