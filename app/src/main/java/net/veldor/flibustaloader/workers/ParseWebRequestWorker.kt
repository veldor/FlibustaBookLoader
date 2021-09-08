package net.veldor.flibustaloader.workers

import net.veldor.flibustaloader.utils.XMLParser.searchDownloadLinks
import net.veldor.flibustaloader.App
import android.content.Context
import androidx.work.*

class ParseWebRequestWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        val data = App.instance.mRequestData
        if (data != null) {
            searchDownloadLinks(data)
        }
        return Result.success()
    }
}