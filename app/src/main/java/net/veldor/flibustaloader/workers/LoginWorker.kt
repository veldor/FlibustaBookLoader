package net.veldor.flibustaloader.workers

import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.http.TorWebClient
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.*
import java.io.UnsupportedEncodingException
import java.lang.Exception

class LoginWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        val data = inputData
        val login = data.getString(USER_LOGIN)
        val password = data.getString(USER_PASSWORD)
        Log.d("surprise", "LoginWorker.java 35 doWork: login is $login")
        Log.d("surprise", "LoginWorker.java 35 doWork: password is $password")
        Log.d("surprise", "LoginWorker doWork prepare work")
        try {
            // создам запрос на аутентификацию
            val url = "http://flibustahezeous3.onion/node?destination=node"
            App.instance.requestStatus.postValue(
                App.instance.getString(R.string.message_prepare_request)
            )
            val webClient = TorWebClient()
            val request = Uri.parse(url)
            App.instance.requestStatus.postValue(
                App.instance.getString(R.string.send_request_message)
            )
            val result = webClient.login(request, login!!, password!!)
            if (result) {
                return Result.success()
            }
        } catch (e: UnsupportedEncodingException) {
            Log.d("surprise", "LoginWorker doWork не могу преобразовать запрос")
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Result.failure()
    }

    companion object {
        const val USER_LOGIN = "user login"
        const val USER_PASSWORD = "user password"
        const val LOGIN_ACTION = "login"
    }
}