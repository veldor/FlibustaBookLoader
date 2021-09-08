package net.veldor.flibustaloader.utils

import android.content.Context
import android.os.Build
import android.provider.DocumentsContract
import android.os.Environment
import androidx.annotation.RequiresApi
import android.net.Uri

internal object UriConverter {
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    fun getPath(context: Context?, uri: Uri): String? {
        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            println("getPath() uri: $uri")
            println("getPath() uri authority: " + uri.authority)
            println("getPath() uri path: " + uri.path)

            // ExternalStorageProvider
            if ("com.android.externalstorage.documents" == uri.authority) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                println("getPath() docId: " + docId + ", split: " + split.size + ", type: " + type)

                // This is for checking Main Memory
                return if ("primary".equals(type, ignoreCase = true)) {
                    if (split.size > 1) {
                        Environment.getExternalStorageDirectory().toString() + "/" + split[1] + "/"
                    } else {
                        Environment.getExternalStorageDirectory().toString() + "/"
                    }
                    // This is for checking SD Card
                } else {
                    "storage" + "/" + docId.replace(":", "/")
                }
            }
        }
        return null
    }
}