package net.veldor.flibustaloader.selections

import android.graphics.Bitmap
import java.io.File
import java.util.ArrayList

class FoundedEntity {
    var filterResult: FilteringResult? = null
    var content: String = ""
    var selected: Boolean = false
    var buttonPressed: Boolean = false
    var description: String = ""
    var language: String = ""
    var coverUrl: String? = null
    var type: String? = null
    var id: String? = null
    var link: String? = null
    var name: String? = null
    var author: String? = null
    var downloadsCount: String? = null
    var translate: String? = null
    var size: String? = null
    var format: String? = null
    val downloadLinks = ArrayList<DownloadLink>()
    val genres = ArrayList<FoundedEntity>()
    var genreComplex: String? = null
    val sequences = ArrayList<FoundedEntity>()
    val authors = ArrayList<FoundedEntity>()
    var sequencesComplex: String = ""
    var cover: File? = null

    var read = false
    var downloaded = false
}