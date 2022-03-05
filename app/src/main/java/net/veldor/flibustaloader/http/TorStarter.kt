package net.veldor.flibustaloader.http

import androidx.lifecycle.MutableLiveData
import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager
import net.veldor.flibustaloader.App
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
                App.instance.torException = e
                e.printStackTrace()
            } catch (e: IOException) {
                App.instance.torException = e
                e.printStackTrace()
            }
        }
        return false
    }

    companion object {
        const val TOR_FILES_LOCATION = "torfiles"
        var torStartTry = 0
        const val TOTAL_TRIES_PER_TOR_STARTUP = 3
        private const val TOR_NOT_LAUNCH = 0
        const val TOR_LAUNCH_IN_PROGRESS = 1
        const val TOR_LAUNCH_SUCCESS = 2
        const val TOR_LAUNCH_FAILED = 3
        // хранилище статуса HTTP запроса
        val liveTorLaunchState = MutableLiveData(TOR_NOT_LAUNCH)
        val TOTAL_SECONDS_PER_TOR_STARTUP = TimeUnit.MINUTES.toSeconds(1)
            .toInt()
    }

    init {
        if (App.instance.mLoadedTor.value == null) {
            tor = AndroidOnionProxyManager(App.instance, TOR_FILES_LOCATION)
            App.instance.mLoadedTor.postValue(tor)
        } else {
            tor = App.instance.mLoadedTor.value
        }
    }
}