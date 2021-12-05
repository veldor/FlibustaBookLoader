package net.veldor.flibustaloader.handlers

import android.graphics.Bitmap
import android.util.Log
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.http.UniversalWebClient
import net.veldor.flibustaloader.selections.FoundedEntity
import net.veldor.flibustaloader.utils.Grammar
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.concurrent.thread


class PicHandler {

    companion object {
        fun dropPreviousLoading() {
            queue = arrayListOf()
        }

        var queue = ArrayList<FoundedEntity>()
        var loadInProgress = false
    }

    fun loadPic(foundedEntity: FoundedEntity) {
        // put image to load queue
        queue.add(foundedEntity)
        initLoad()
    }

    private fun initLoad() {
        if (!loadInProgress) {
            loadInProgress = true
            loadPics()
        }
    }

    private fun loadPics() {
        thread {
            var foundedEntity: FoundedEntity
            while (queue.isNotEmpty()) {
                try {
                    foundedEntity = queue.removeAt(0)
                    downloadPic(foundedEntity)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            loadInProgress = false
        }
    }

    fun downloadPic(
        foundedEntity: FoundedEntity
    ) {
        if (foundedEntity.cover != null) {
            return
        }
        GlobalScope.launch(Dispatchers.IO) {
            val response = UniversalWebClient().picRequest(foundedEntity.coverUrl!!)
            if (response != null) {
                val status = response.statusLine.statusCode
                if (status < 400) {
                    var tempFile: File? = null
                    val contentTypeHeader = response.getLastHeader("Content-Type")
                    when (contentTypeHeader.value) {
                        "image/jpeg" -> {
                            tempFile = File.createTempFile(Grammar.longRandom.toString(), "jpg")
                            tempFile.deleteOnExit()
                            val out: OutputStream = FileOutputStream(tempFile)
                            var read: Int
                            val buffer = ByteArray(1024)
                            while (response.entity.content.read(buffer).also { read = it } > 0) {
                                out.write(buffer, 0, read)
                            }
                            out.close()
                            response.entity.content.close()
                        }
                        "image/png" -> {
                            tempFile = File.createTempFile(Grammar.longRandom.toString(), "png")
                            tempFile.deleteOnExit()
                            val out: OutputStream = FileOutputStream(tempFile)
                            var read: Int
                            val buffer = ByteArray(1024)
                            while (response.entity.content.read(buffer).also { read = it } > 0) {
                                out.write(buffer, 0, read)
                            }
                            out.close()
                            response.entity.content.close()
                        }
                        else -> {
                            Log.d(
                                "surprise",
                                "loadPic: pic ${foundedEntity.coverUrl} is ${contentTypeHeader.value}"
                            )
                        }
                    }
                    if (tempFile != null && tempFile.isFile && tempFile.length() > 0) {
                        val compressedImageFile = Compressor.compress(App.instance, tempFile) {
                            resolution(100, 143)
                            quality(80)
                            format(Bitmap.CompressFormat.JPEG)
                        }
                        foundedEntity.cover = compressedImageFile
                        tempFile.delete()
                    }
                }
            }
        }
    }
}