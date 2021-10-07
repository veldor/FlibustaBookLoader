package net.veldor.flibustaloader.handlers

import android.graphics.BitmapFactory
import net.veldor.flibustaloader.http.UniversalWebClient
import net.veldor.flibustaloader.selections.FoundedEntity
import kotlin.concurrent.thread

class PicHandler {
    fun loadPic(foundedEntity: FoundedEntity) {
        thread {
            val response = UniversalWebClient().picRequest(foundedEntity.coverUrl!!)
            if (response != null) {
                foundedEntity.cover = BitmapFactory.decodeStream(response.entity.content)
            }
        }
    }
}