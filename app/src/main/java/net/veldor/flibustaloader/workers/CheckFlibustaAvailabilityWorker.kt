package net.veldor.flibustaloader.workers

import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.http.GlobalWebClient
import android.content.Context
import net.veldor.flibustaloader.notificatons.NotificationHandler
import android.util.Log
import androidx.work.*
import net.veldor.flibustaloader.utils.PreferencesHandler
import java.io.IOException

class CheckFlibustaAvailabilityWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        val outputDataBuilder = Data.Builder()

        // проверю доступность интернета в принципе
        var url: String
        // теперь проверю подключение к основному серверу
        if (PreferencesHandler.instance.isCustomMirror) {
            url = PreferencesHandler.instance.customMirror
            if (inspect(url)) {
                outputDataBuilder.putBoolean(AVAILABILITY_STATE, true)
            }
        } else if (PreferencesHandler.instance.isExternalVpn) {
            url = "http://flibusta.is/"
            if (inspect(url)) {
                outputDataBuilder.putBoolean(AVAILABILITY_STATE, true)
            }
        } else {
            url = "http://flibustahezeous3.onion/"
            if (inspect(url)) {
                outputDataBuilder.putBoolean(AVAILABILITY_STATE, true)
            } else {
                // запущу периодическую проверку доступности зеркала
                App.instance.startCheckWorker()
                // попробую использовать резервное подключение
                url = "https://flibusta.appspot.com/"
                if (inspect(url)) {
                    App.instance.useMirror = true
                    NotificationHandler.instance.notifyUseMirror()
                    outputDataBuilder.putBoolean(AVAILABILITY_STATE, true)
                }
            }
        }
        return Result.success(outputDataBuilder.build())
    }

    private fun inspect(url: String): Boolean {
        try {
            val answer = GlobalWebClient.requestNoMirror(url)
            if (answer != null && answer.isNotEmpty()) {
                Log.d("surprise", "CheckFlibustaAvailabilityWorker inspect 49: check success")
                return true
            }
        } catch (e: IOException) {
            Log.d("surprise", "CheckFlibustaAvailabilityWorker inspect 57: check failed")
            //e.printStackTrace();
        }
        return false
    }

    companion object {
        const val ACTION = "check availability"
        const val AVAILABILITY_STATE = "availability state"
    }
}