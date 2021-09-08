package net.veldor.flibustaloader.utils

import android.util.Log
import java.io.*
import java.lang.Exception
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipManager {
    fun zip(existentFiles: Array<File>, outputFile: File?) {
        try {
            var origin: BufferedInputStream
            val dest = FileOutputStream(outputFile)
            val out = ZipOutputStream(
                BufferedOutputStream(
                    dest
                )
            )
            val data = ByteArray(BUFFER)
            for (file in existentFiles) {
                Log.d("surprise", "Adding: $file")
                val fi = FileInputStream(file)
                origin = BufferedInputStream(fi, BUFFER)
                val entry = ZipEntry(file.name)
                out.putNextEntry(entry)
                var count: Int
                while (origin.read(data, 0, BUFFER).also { count = it } != -1) {
                    out.write(data, 0, count)
                }
                origin.close()
            }
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val BUFFER = 1024
    }
}