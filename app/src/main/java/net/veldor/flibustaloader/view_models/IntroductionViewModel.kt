package net.veldor.flibustaloader.view_models

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
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
import net.veldor.flibustaloader.http.UniversalWebClient
import net.veldor.flibustaloader.utils.FlibustaChecker
import net.veldor.flibustaloader.utils.FlibustaChecker.Companion.STATE_AVAILABLE
import net.veldor.flibustaloader.utils.FlibustaChecker.Companion.STATE_UNAVAILABLE
import java.lang.Exception
import java.net.InetAddress


class IntroductionViewModel : ViewModel() {
    fun permissionsGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val writeResult =
                App.instance.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val readResult =
                App.instance.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            return writeResult == PackageManager.PERMISSION_GRANTED && readResult == PackageManager.PERMISSION_GRANTED
        }

        val writeResult = PermissionChecker.checkSelfPermission(
            App.instance.applicationContext,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val readResult = PermissionChecker.checkSelfPermission(
            App.instance.applicationContext,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        return writeResult == PackageManager.PERMISSION_GRANTED && readResult == PackageManager.PERMISSION_GRANTED
    }

}