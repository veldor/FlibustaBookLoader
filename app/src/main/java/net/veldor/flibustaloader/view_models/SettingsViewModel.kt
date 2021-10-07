package net.veldor.flibustaloader.view_models

import android.app.Application
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.handlers.BackupSettings
import net.veldor.flibustaloader.ui.MainActivity

class SettingsViewModel(application: Application) : GlobalViewModel(application) {

    private val _liveBackupFile = MutableLiveData<DocumentFile>()
    val liveBackupData: LiveData<DocumentFile> = _liveBackupFile

    private val _livePrefsRestored = MutableLiveData<Boolean>()
    val livePrefsRestored: LiveData<Boolean> = _livePrefsRestored

    fun backup(dl: DocumentFile) {
        viewModelScope.launch(Dispatchers.IO) {
            _liveBackupFile.postValue(BackupSettings.backup(dl))
        }
    }

    fun restore(file: DocumentFile) {
        viewModelScope.launch(Dispatchers.IO) {
            BackupSettings.restore(file)
            Log.d("surprise", "restore: now reboot app")
            // notify about settings restored
            _livePrefsRestored.postValue(true)
        }
    }
}