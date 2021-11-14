package net.veldor.flibustaloader.utils

import net.veldor.flibustaloader.App
import android.os.Environment
import androidx.work.OneTimeWorkRequest
import net.veldor.flibustaloader.workers.SendLogWorker
import androidx.work.WorkManager
import net.veldor.flibustaloader.workers.SendLogToMailWorker
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * This singleton class is for debug purposes only. Use it to log your selected classes into file. <br></br> Needed permissions:
 * READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, READ_LOGS" <br></br><br></br>Example usage:<br></br> ` FileLogHelper.getInstance().addLogTag(TAG);`
 *
 *
 * Created by bendaf on 2016-04-28
 */
class LogHandler private constructor() {
    fun initLog() {
        val isLogStarted = false
        if (!isLogStarted && shouldLog) {
            val dF = SimpleDateFormat("yy-MM-dd_HH_mm''ss", Locale.getDefault())
            val fileName = "fb_logcat" + dF.format(Date()) + ".txt"
            val outputFile =
                File(Environment.getExternalStorageDirectory().absolutePath + "/flibusta_logcat/")
            if (outputFile.mkdirs() || outputFile.isDirectory) {
                // удалю старые файлы
                val existentFiles = outputFile.listFiles()
                if (existentFiles != null && existentFiles.isNotEmpty()) {
                    val time = System.currentTimeMillis()
                    for (f in existentFiles) {
                        if (f.isFile && f.lastModified() < time - 3600000) {
                            f.delete()
                        }
                    }
                }
                val logFileAbsolutePath = outputFile.absolutePath + "/" + fileName
                // startLog();
                // clear the previous logcat and then write the new one to the file
                try {
                    Runtime.getRuntime().exec("logcat -c")
                    Runtime.getRuntime().exec("logcat -f $logFileAbsolutePath")
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun sendLogs() {
        // запущу рабочего, который подготовит лог и отправит его
        val work = OneTimeWorkRequest.Builder(SendLogWorker::class.java).build()
        WorkManager.getInstance(App.instance).enqueue(work)
    }
    fun sendLogsToMail() {
        // запущу рабочего, который подготовит лог и отправит его
        val work = OneTimeWorkRequest.Builder(SendLogToMailWorker::class.java).build()
        WorkManager.getInstance(App.instance).enqueue(work)
    }

    companion object {
        private const val shouldLog = true //TODO: set to false in final version of the app
        private var mInstance: LogHandler? = null
        @kotlin.jvm.JvmStatic
        fun getInstance(): LogHandler? {
            if (mInstance == null) {
                mInstance = LogHandler()
            }
            return mInstance
        }
    }
}