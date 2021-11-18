package net.veldor.flibustaloader.handlers

import android.util.Log
import net.veldor.flibustaloader.http.UniversalWebClient
import net.veldor.flibustaloader.selections.FoundedEntity
import net.veldor.flibustaloader.utils.Grammar
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.concurrent.thread

class PicHandler {

    companion object{
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
        if(!loadInProgress){
            loadInProgress = true
            loadPics()
        }
    }

    private fun loadPics() {
        thread {
            var foundedEntity: FoundedEntity
            while (queue.isNotEmpty()){
                try {
                    foundedEntity = queue.removeAt(0)
                    downloadPic(foundedEntity)
                }
                catch (e: Exception){
                    e.printStackTrace()
                }
            }
            loadInProgress = false
        }
    }

    fun downloadPic(
        foundedEntity: FoundedEntity
    ) {
        val response = UniversalWebClient().picRequest(foundedEntity.coverUrl!!)
        if (response != null) {
            val status = response.statusLine.statusCode
            if (status < 400) {
                val contentTypeHeader = response.getLastHeader("Content-Type")
                when (contentTypeHeader.value) {
                    "image/jpeg" -> {
                        val tempFile = File.createTempFile(Grammar.longRandom.toString(), "jpg")
                        Log.d("surprise", "downloadPic: ${tempFile.absolutePath}")
                        tempFile.deleteOnExit()
                        val out: OutputStream = FileOutputStream(tempFile)
                        var read: Int
                        val buffer = ByteArray(1024)
                        while (response.entity.content.read(buffer).also { read = it } > 0) {
                            out.write(buffer, 0, read)
                        }
                        out.close()
                        response.entity.content.close()
                        foundedEntity.cover = tempFile
                    }
                    "image/png" -> {
                        val tempFile = File.createTempFile(Grammar.longRandom.toString(), "png")
                        tempFile.deleteOnExit()
                        val out: OutputStream = FileOutputStream(tempFile)
                        var read: Int
                        val buffer = ByteArray(1024)
                        while (response.entity.content.read(buffer).also { read = it } > 0) {
                            out.write(buffer, 0, read)
                        }
                        out.close()
                        response.entity.content.close()
                        foundedEntity.cover = tempFile
                    }
                    else -> {
                        Log.d(
                            "surprise",
                            "loadPic: pic ${foundedEntity.coverUrl} is ${contentTypeHeader.value}"
                        )
                    }
                }
            }
        }
    }

}