package net.veldor.flibustaloader.selections

import java.util.ArrayList

class FoundedEntity {
    lateinit var type: String
    lateinit var id: String
    lateinit var link: String
    lateinit var name: String
    var author: String? = null
    lateinit var downloadsCount: String
    var translate: String? = null
    var size: String? = null
    var format: String? = null
    val downloadLinks = ArrayList<DownloadLink>()
    val genres = ArrayList<FoundedEntity>()
    var genreComplex: String? = null
    val sequences = ArrayList<FoundedEntity>()
    var sequencesComplex: String? = null


    var read = false
    var downloaded = false
}