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
import java.io.File

class SettingsViewModel(application: Application) : GlobalViewModel(application) {

    private val _liveBackupFile = MutableLiveData<DocumentFile>()
    val liveBackupData: LiveData<DocumentFile> = _liveBackupFile

    private val _liveCompatBackupFile = MutableLiveData<File>()
    val liveCompatBackupData: LiveData<File> = _liveCompatBackupFile

    private val _livePrefsRestored = MutableLiveData<Boolean>()
    val livePrefsRestored: LiveData<Boolean> = _livePrefsRestored

    fun backup(dl: DocumentFile, options: BooleanArray) {
        viewModelScope.launch(Dispatchers.IO) {
            _liveBackupFile.postValue(BackupSettings.backup(dl, options))
        }
    }

    fun restore(file: DocumentFile, options: BooleanArray) {
        viewModelScope.launch(Dispatchers.IO) {
            BackupSettings.restore(file, options)
            // notify about settings restored
            _livePrefsRestored.postValue(true)
        }
    }
    fun restore(file: File, options: BooleanArray) {
        viewModelScope.launch(Dispatchers.IO) {
            BackupSettings.restore(file, options)
            // notify about settings restored
            _livePrefsRestored.postValue(true)
        }
    }

    fun backup(dl: File, options: BooleanArray) {
        viewModelScope.launch(Dispatchers.IO) {
            _liveCompatBackupFile.postValue(BackupSettings.backup(dl, options))
        }
    }

    fun checkReserve(dl: DocumentFile): BooleanArray {
        // прочитаю список файлов в архиве
        val filesInZip = BackupSettings.getFilesList(dl)
        val result = BooleanArray(15)
        result[0] = filesInZip.contains(BackupSettings.PREF_BACKUP_NAME)
        result[1] = filesInZip.contains(BackupSettings.DOWNLOADED_BOOKS_BACKUP_NAME)
        result[2] = filesInZip.contains(BackupSettings.READED_BOOKS_BACKUP_NAME)
        result[3] = filesInZip.contains(BackupSettings.AUTOFILL_BACKUP_NAME)
        result[4] = filesInZip.contains(BackupSettings.BOOKMARKS_BACKUP_NAME)
        result[5] = filesInZip.contains(BackupSettings.BOOKS_SUBSCRIBE_BACKUP_NAME)
        result[6] = filesInZip.contains(BackupSettings.AUTHORS_SUBSCRIBE_BACKUP_NAME)
        result[7] = filesInZip.contains(BackupSettings.GENRE_SUBSCRIBE_BACKUP_NAME)
        result[8] = filesInZip.contains(BackupSettings.SEQUENCES_SUBSCRIBE_BACKUP_NAME)
        result[9] = filesInZip.contains(BackupSettings.BLACKLIST_BOOKS_BACKUP_NAME)
        result[10] = filesInZip.contains(BackupSettings.BLACKLIST_AUTHORS_BACKUP_NAME)
        result[11] = filesInZip.contains(BackupSettings.BLACKLIST_GENRES_BACKUP_NAME)
        result[12] = filesInZip.contains(BackupSettings.BLACKLIST_SEQUENCES_BACKUP_NAME)
        result[13] = filesInZip.contains(BackupSettings.BLACKLIST_FORMAT_BACKUP_NAME)
        result[14] = filesInZip.contains(BackupSettings.DOWNLOAD_SCHEDULE_BACKUP_NAME)
        return result
    }

    fun checkReserve(dl: File): BooleanArray {
        // прочитаю список файлов в архиве
        val filesInZip = BackupSettings.getFilesList(dl)
        val result = BooleanArray(15)
        result[0] = filesInZip.contains(BackupSettings.PREF_BACKUP_NAME)
        result[1] = filesInZip.contains(BackupSettings.DOWNLOADED_BOOKS_BACKUP_NAME)
        result[2] = filesInZip.contains(BackupSettings.READED_BOOKS_BACKUP_NAME)
        result[3] = filesInZip.contains(BackupSettings.AUTOFILL_BACKUP_NAME)
        result[4] = filesInZip.contains(BackupSettings.BOOKMARKS_BACKUP_NAME)
        result[5] = filesInZip.contains(BackupSettings.BOOKS_SUBSCRIBE_BACKUP_NAME)
        result[6] = filesInZip.contains(BackupSettings.AUTHORS_SUBSCRIBE_BACKUP_NAME)
        result[7] = filesInZip.contains(BackupSettings.GENRE_SUBSCRIBE_BACKUP_NAME)
        result[8] = filesInZip.contains(BackupSettings.SEQUENCES_SUBSCRIBE_BACKUP_NAME)
        result[9] = filesInZip.contains(BackupSettings.BLACKLIST_BOOKS_BACKUP_NAME)
        result[10] = filesInZip.contains(BackupSettings.BLACKLIST_AUTHORS_BACKUP_NAME)
        result[11] = filesInZip.contains(BackupSettings.BLACKLIST_GENRES_BACKUP_NAME)
        result[12] = filesInZip.contains(BackupSettings.BLACKLIST_SEQUENCES_BACKUP_NAME)
        result[13] = filesInZip.contains(BackupSettings.BLACKLIST_FORMAT_BACKUP_NAME)
        result[14] = filesInZip.contains(BackupSettings.DOWNLOAD_SCHEDULE_BACKUP_NAME)
        return result
    }
}