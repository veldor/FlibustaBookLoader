package net.veldor.flibustaloader.view_models

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.PermissionChecker
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.http.TorWebClient
import net.veldor.flibustaloader.notificatons.NotificationHandler
import net.veldor.flibustaloader.utils.URLHelper

class StartViewModel : ViewModel() {

    private var currentWork: Job? = null
    private val _connectionTestSuccess = MutableLiveData<Boolean>().apply {}
    val connectionTestSuccess: LiveData<Boolean> = _connectionTestSuccess
    private val _connectionTestFailed = MutableLiveData<Boolean>().apply {}
    val connectionTestFailed: LiveData<Boolean> = _connectionTestFailed
    private var testInProgress = false

    fun checkFlibustaAvailability() {
        currentWork?.cancel()
        currentWork = viewModelScope.launch(Dispatchers.IO) {
            if (!testInProgress) {
                testInProgress = true
                try {
                    val url = URLHelper.getFlibustaUrl();
                    Log.d("surprise", "checkFlibustaAvailability: check $url")
                    val result = TorWebClient().directRequest(url)
                    if (result.isNullOrEmpty()) {
                        Log.d("surprise", "checkFlibustaAvailability: can't request base mirror")
                    } else {
                        Log.d("surprise", "checkFlibustaAvailability: base url available")
                        _connectionTestSuccess.postValue(true)
                        testInProgress = false
                        return@launch
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.d("surprise", "checkFlibustaAvailability: error then request mirror")
                }
                Log.d("surprise", "checkFlibustaAvailability: check ${URLHelper.getFlibustaMirrorUrl()}")
                val result = TorWebClient().directRequest(URLHelper.getFlibustaMirrorUrl())
                testInProgress = if (result.isNullOrEmpty()) {
                    Log.d("surprise", "checkFlibustaAvailability: totally connection wrong")
                    _connectionTestFailed.postValue(true)
                    false
                } else{
                    Log.d("surprise", "checkFlibustaAvailability: mirror url available")
                    // set use alternative mirror
                    App.instance.useMirror = true
                    NotificationHandler.instance.notifyUseAlternativeMirror()
                    _connectionTestSuccess.postValue(true)
                    false
                }
            }
            else{
                Log.d("surprise", "checkFlibustaAvailability: test already start")
            }
        }
    }

    fun cancelCheck(){
        currentWork?.cancel()
    }


    fun permissionGranted(): Boolean {
        val writeResult: Int
        val readResult: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            writeResult = App.instance.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            readResult = App.instance.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            writeResult = PermissionChecker.checkSelfPermission(
                    App.instance.applicationContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            readResult = PermissionChecker.checkSelfPermission(
                    App.instance.applicationContext,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
        return writeResult == PackageManager.PERMISSION_GRANTED && readResult == PackageManager.PERMISSION_GRANTED
    }
}