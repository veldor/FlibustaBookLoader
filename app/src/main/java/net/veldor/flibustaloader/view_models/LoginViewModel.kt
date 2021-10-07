package net.veldor.flibustaloader.view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.veldor.flibustaloader.http.UniversalWebClient

class LoginViewModel : ViewModel() {
    fun logMeIn(login: String, password: String){
        viewModelScope.launch(Dispatchers.IO) {
            val result = UniversalWebClient().loginRequest(login, password)
            if(result){
                _liveLoginResult.postValue(LOGIN_SUCCESS)
            }
            else{
                _liveLoginResult.postValue(LOGIN_FAILED)
            }
        }
    }

    private val _liveLoginResult = MutableLiveData<Int>()
    val liveLoginResult: LiveData<Int> = _liveLoginResult
    companion object{
        const val LOGIN_SUCCESS = 1
        const val LOGIN_FAILED = 2
    }
}