package net.veldor.flibustaloader.services

import android.app.DownloadManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.MutableLiveData
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.BuildConfig
import net.veldor.flibustaloader.updater.Updater
import java.io.File

class UpdateWaitService : Service() {
    private var mDownloadId: Long? = null
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // получу идентификатор загрузки
        val identification: MutableLiveData<Long> = Updater.updateDownloadIdentification
        mDownloadId = identification.value
        // Регистрирую сервис для приёма статуса загрузки обновления
        val downloadObserver = DownloadReceiver()
        this.registerReceiver(
            downloadObserver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private inner class DownloadReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val finishedDownloadId: Long =
                intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (finishedDownloadId == mDownloadId) {
                val query: DownloadManager.Query = DownloadManager.Query()
                query.setFilterById(finishedDownloadId)
                val manager: DownloadManager = application.getSystemService(
                    DOWNLOAD_SERVICE
                ) as DownloadManager
                val cursor: Cursor = manager.query(query)
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = cursor.getInt(columnIndex)
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        //open the downloaded file
                        val install = Intent(Intent.ACTION_VIEW)
                        install.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        var downloadUri: Uri? = App.instance.updateDownloadUri
                        if (Build.VERSION.SDK_INT >= 24) {
                            downloadUri = FileProvider.getUriForFile(
                                context, BuildConfig.APPLICATION_ID + ".provider",
                                App.instance.downloadedApkFile!!
                            )
                        }
                        install.setDataAndType(
                            downloadUri,
                            manager.getMimeTypeForDownloadedFile(mDownloadId!!)
                        )
                        Log.d("surprise", "DownloadReceiver onReceive: trying install update")
                        context.startActivity(install)
                    } else {
                        clearFile()
                    }
                } else {
                    clearFile()
                }
                App.instance.downloadedApkFile = null
                App.instance.updateDownloadUri = null
                context.unregisterReceiver(this)
                stopSelf()
            }
        }

        private fun clearFile() {
            // удалю файл, если он создался
            val file: File? = App.instance.downloadedApkFile
            if (file != null) {
                if (file.exists()) {
                    val deleteResult = file.delete()
                    if (!deleteResult) {
                        Log.d(
                            "surprise",
                            "DownloadReceiver onReceive: не удалось удалить загруженный файл"
                        )
                    }
                }
            }
            Toast.makeText(applicationContext, "Flibusta loader update failed", Toast.LENGTH_LONG)
                .show()
        }
    }
}