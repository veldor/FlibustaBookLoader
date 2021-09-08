package net.veldor.flibustaloader.http

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager
import net.veldor.flibustaloader.App
import android.util.Log
import java.io.IOException
import java.util.concurrent.TimeUnit

class TorStarter {
    var tor: AndroidOnionProxyManager? = null
    fun startTor(): Boolean {
        if (tor != null) {
            // тут- время, которое отводится на попытку запуска
            // количество попыток запуска
            // пытаемся запустить TOR. Если он запустится- вернёт TRUE, Иначе- false
            try {
                return tor!!.startWithRepeat(
                    TOTAL_SECONDS_PER_TOR_STARTUP,
                    TOTAL_TRIES_PER_TOR_STARTUP
                )
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return false
    }

    companion object {
        const val TOTAL_TRIES_PER_TOR_STARTUP = 3
        val TOTAL_SECONDS_PER_TOR_STARTUP = TimeUnit.MINUTES.toSeconds(1)
            .toInt()
    }

    init {
        if (App.instance.mLoadedTor.value == null) {
            tor = AndroidOnionProxyManager(App.instance, App.TOR_FILES_LOCATION)
            Log.d("surprise", "StartTorWorker doWork 39: tor manager added")
            App.instance.mLoadedTor.postValue(tor)
        } else {
            tor = App.instance.mLoadedTor.value
        }
    }
}