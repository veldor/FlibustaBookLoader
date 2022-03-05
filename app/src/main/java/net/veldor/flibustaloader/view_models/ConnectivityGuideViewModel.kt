package net.veldor.flibustaloader.view_models

import android.content.Context
import android.net.ConnectivityManager
import android.text.Editable
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.http.TorStarter
import net.veldor.flibustaloader.http.UniversalWebClient
import net.veldor.flibustaloader.http.WebResponse
import net.veldor.flibustaloader.utils.FilesHandler
import net.veldor.flibustaloader.utils.FlibustaChecker
import net.veldor.flibustaloader.utils.FlibustaChecker.Companion.STATE_AVAILABLE
import net.veldor.flibustaloader.utils.FlibustaChecker.Companion.STATE_UNAVAILABLE
import net.veldor.flibustaloader.utils.Grammar
import net.veldor.flibustaloader.utils.PreferencesHandler
import java.net.InetAddress


class ConnectivityGuideViewModel : ViewModel() {
    private lateinit var testConnectionJob: Job
    private lateinit var testLibraryJob: Job
    private lateinit var testAccessJob: Job
    private var initTorJob: Job? = null

    private val _testConnectionState = MutableLiveData<String>()
    val testConnectionState: LiveData<String> = _testConnectionState

    private val _libraryConnectionState = MutableLiveData<String>()
    val libraryConnectionState: LiveData<String> = _libraryConnectionState

    private val _testTorInit = MutableLiveData<String?>()
    val testTorInit: LiveData<String?> = _testTorInit

    fun testInternetConnection() {
        _testConnectionState.postValue(STATE_WAIT)
        testConnectionJob = viewModelScope.launch(Dispatchers.IO) {
            val cm =
                App.instance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            if (cm == null || !cm.activeNetworkInfo.isConnected) {
                _testConnectionState.postValue(STATE_FAILED)
                return@launch
            }
            val ipAddr: InetAddress = InetAddress.getByName("google.com")
            if (ipAddr.equals("")) {
                _testConnectionState.postValue(STATE_FAILED)
                return@launch
            }
            _testConnectionState.postValue(STATE_PASSED)
        }
    }

    fun testLibraryConnection(url: String) {
        testLibraryJob = viewModelScope.launch(Dispatchers.IO) {
            _libraryConnectionState.postValue(STATE_WAIT)
            when (FlibustaChecker().isAlive(url)) {
                STATE_AVAILABLE -> {
                    _libraryConnectionState.postValue(STATE_PASSED)
                }
                STATE_UNAVAILABLE -> {
                    _libraryConnectionState.postValue(STATE_FAILED)
                }
                else -> {
                    _libraryConnectionState.postValue(STATE_CHECK_ERROR)
                }
            }
        }
    }

    fun finallyTestConnection(text: Editable?) {
        testAccessJob = viewModelScope.launch(Dispatchers.IO) {
            _libraryConnectionState.postValue(STATE_WAIT)
            try {
                val response: WebResponse = if(text.toString().isNotEmpty()){
                    val customMirrorUrl = text.toString()
                    Log.d("surprise", "ConnectivityGuideViewModel.kt 89 finallyTestConnection mirror is $customMirrorUrl")
                    if(Grammar.isValidUrl(customMirrorUrl)){
                        // test with this mirror
                        UniversalWebClient().rawRequest(customMirrorUrl,"/opds")
                    } else{
                        UniversalWebClient().rawRequest("/opds")
                    }
                } else{
                    UniversalWebClient().rawRequest("/opds")
                }
                val answer = UniversalWebClient().responseToString(response.inputStream)
                if (answer.isNullOrEmpty() || !answer.startsWith("<?xml version=\"1.0\" encoding=\"utf-8\"?>")) {
                    _libraryConnectionState.postValue(STATE_FAILED)
                }
                else{
                    _libraryConnectionState.postValue(STATE_PASSED)
                    if(!text.isNullOrEmpty()){
                        Log.d(
                            "surprise",
                            "ConnectivityGuideViewModel.kt 106 finallyTestConnection saving custom mirror $text"
                        )
                        // save it as custom mirror
                        PreferencesHandler.instance.customMirror = text.toString()
                        PreferencesHandler.instance.isCustomMirror = true
                    }
                }
            }
            catch (e: Exception){
                _libraryConnectionState.postValue(STATE_CHECK_ERROR)
            }
        }
    }

    fun initTor() {
        initTorJob = viewModelScope.launch(Dispatchers.IO) {
            _testTorInit.postValue(STATE_WAIT)
            try {
                if(App.instance.mLoadedTor.value != null){
                    val tor = App.instance.mLoadedTor.value
                    tor!!.stop()
                }
                Log.d("surprise", "StartTorWorker.kt 12 doWork running start tor worker")
                // получу список мостов для TOR из Firestore
                val starter = TorStarter()
                var torStartTry = 0
                // попробую стартовать TOR 3 раза
                while (torStartTry < 4) {
                    torStartTry++
                    _testTorInit.postValue("Попытка запуска #$torStartTry")
                    // есть три попытки, если все три неудачны- верну ошибку
                    if (starter.startTor()) {
                        _testTorInit.postValue(STATE_PASSED)
                        return@launch
                    }
                }
                _testTorInit.postValue(STATE_FAILED)
                return@launch
            } catch (e: Exception) {
                e.printStackTrace()
                _testTorInit.postValue(e.message)
                Thread.sleep(1000)
                _testTorInit.postValue(STATE_CHECK_ERROR)
            }
        }
    }

    fun cancelTorLaunch() {
        initTorJob?.cancel()
        _testTorInit.value = null
    }

    fun loadTorBridges() {
        viewModelScope.launch (Dispatchers.IO){
            val db = Firebase.firestore
            db.collection("bridges")
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        // save document data
                        FilesHandler.saveBridges(document.data)
                        PreferencesHandler.instance.setCustomBridges(document.data)
                        Toast.makeText(App.instance, App.instance.getString(R.string.new_bridges_loaded_message), Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(App.instance, App.instance.getString(R.string.error_load_bridges_message), Toast.LENGTH_LONG).show()
                }
        }
    }

    fun saveBridges(text: Editable?) {
        if(text != null && text.isNotEmpty()){
            App.instance.isCustomBridgesSet = true
            PreferencesHandler.instance.setCustomBridges(text.toString())
            PreferencesHandler.instance.setUseCustomBridges(true)
            FilesHandler.saveBridges(text.toString())
        }
    }

    companion object {
        const val STATE_WAIT = "wait"
        const val STATE_PASSED = "passed"
        const val STATE_FAILED = "failed"
        const val STATE_CHECK_ERROR = "check error"
    }
}