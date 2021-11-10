package net.veldor.flibustaloader.handlers

import android.graphics.BitmapFactory
import android.util.Log
import net.veldor.flibustaloader.http.UniversalWebClient
import net.veldor.flibustaloader.selections.FoundedEntity
import java.lang.Exception
import kotlin.concurrent.thread

class PicHandler {
    fun loadPic(foundedEntity: FoundedEntity) {
        thread {
            try {
                val response = UniversalWebClient().picRequest(foundedEntity.coverUrl!!)
                if (response != null) {
                    val status = response.statusLine.statusCode
                    if(status < 400) {
                        val contentTypeHeader = response.getLastHeader("Content-Type")
                        if(contentTypeHeader.value == "image/jpeg"){
                            foundedEntity.cover = BitmapFactory.decodeStream(response.entity.content)
                        }
                        else if(contentTypeHeader.value == "image/png"){
                            Log.d("surprise", "loadPic: try load png")
                            foundedEntity.cover = BitmapFactory.decodeStream(response.entity.content)
                        }
                        else{
                            Log.d("surprise", "loadPic: pic ${foundedEntity.coverUrl} is ${contentTypeHeader.value}")
                        }
                    }
                }
            }
            catch (e: Exception){
                e.printStackTrace()
            }
        }
    }
}