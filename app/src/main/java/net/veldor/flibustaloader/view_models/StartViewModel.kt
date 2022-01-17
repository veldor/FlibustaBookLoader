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
import kotlinx.coroutines.launch
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.ecxeptions.ConnectionLostException
import net.veldor.flibustaloader.http.UniversalWebClient
import net.veldor.flibustaloader.utils.FlibustaChecker


class StartViewModel : ViewModel() {


    private var currentWork: Job? = null
    private val _connectionTestSuccess = MutableLiveData<Boolean>().apply {}
    val connectionTestSuccess: LiveData<Boolean> = _connectionTestSuccess
    private val _connectionTestFailed = MutableLiveData<Boolean>().apply {}
    val connectionTestFailed: LiveData<Boolean> = _connectionTestFailed
    private val _torStartFailed = MutableLiveData<Boolean>().apply {}
    val torStartFailed: LiveData<Boolean> = _connectionTestFailed
    private val _flibustaServerCheckState = MutableLiveData<Int>().apply {}
    val flibustaServerCheckState: LiveData<Int> = _flibustaServerCheckState
    private var testInProgress = false

    fun checkFlibustaAvailability() {
        currentWork?.cancel()
        currentWork = viewModelScope.launch(Dispatchers.IO) {
            Thread.sleep(500)
            try {
                if (!testInProgress) {
                    testInProgress = true
                    try {
                        val response = UniversalWebClient().rawRequest("/opds")
                        Log.d(
                            "surprise",
                            "StartViewModel.kt 42 checkFlibustaAvailability first step completed"
                        )
                        val answer = UniversalWebClient().responseToString(response.inputStream)
                        if (answer.isNullOrEmpty() || !answer.startsWith("<?xml version=\"1.0\" encoding=\"utf-8\"?>")) {
                            Log.d(
                                "surprise",
                                "StartViewModel.kt 44 checkFlibustaAvailability success failed"
                            )
                            _connectionTestFailed.postValue(true)
                        } else {
                            Log.d(
                                "surprise",
                                "StartViewModel.kt 47 checkFlibustaAvailability connect success"
                            )
                            _connectionTestSuccess.postValue(true)
                        }
                        testInProgress = false
                        return@launch
                    } catch (e: Exception) {
                        Log.d(
                            "surprise",
                            "StartViewModel.kt 61 checkFlibustaAvailability error here"
                        )
                        e.printStackTrace()
                        _torStartFailed.postValue(true)
                        Log.d("surprise", "checkFlibustaAvailability: error then request mirror")
                    }
                } else {
                    Log.d("surprise", "checkFlibustaAvailability: test already start")
                }
            } catch (err: ConnectionLostException) {
                // не удалось запустить TOR
                _torStartFailed.postValue(true)
            }
        }
    }

    fun cancelCheck() {
        currentWork?.cancel()
    }


    fun permissionGranted(): Boolean {
        val writeResult: Int
        val readResult: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            writeResult =
                App.instance.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
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

    fun ping() {
        viewModelScope.launch(Dispatchers.IO) {
            if (flibustaServerCheckState.value != FlibustaChecker.STATE_WAITING) {
                _flibustaServerCheckState.postValue(FlibustaChecker.STATE_RUNNING)
                try {
                    val result = FlibustaChecker().isAlive()
                    Log.d("surprise", "StartViewModel.kt 98 ping result is $result")
                    _flibustaServerCheckState.postValue(result)
                } catch (e: Exception) {
                    e.printStackTrace()
                    _flibustaServerCheckState.postValue(FlibustaChecker.STATE_PASSED)
                }
            }
        }
    }
}