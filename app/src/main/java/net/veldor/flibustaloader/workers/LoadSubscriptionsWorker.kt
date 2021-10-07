package net.veldor.flibustaloader.workers
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class LoadSubscriptionsWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        return Result.success()
    }

    companion object {
        var sNextPage: String? = null
    }
}