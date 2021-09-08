package net.veldor.flibustaloader.selections

import java.io.Serializable
import java.util.*

class FoundedBook : FoundedItem, Serializable {
    @JvmField
    var name: String? = null
    var author: String? = null
    var downloadsCount: String? = null
    @JvmField
    var translate: String? = null
    val downloadLinks = ArrayList<DownloadLink>()
    var size: String? = null
    @JvmField
    var format: String? = null
    var genreComplex: String? = null
    val genres = ArrayList<Genre>()
    var sequenceComplex: String? = null
    val sequences = ArrayList<FoundedSequence>()
    val authors = ArrayList<Author>()
    @JvmField
    var bookInfo: String? = null
    var id: String? = null
    var read = false
    var downloaded = false
    @JvmField
    var previewUrl: String? = null
    var preferredFormat: String? = null
    @JvmField
    var bookLink: String? = null
    var bookLanguage: String? = null
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + (author?.hashCode() ?: 0)
        result = 31 * result + (downloadsCount?.hashCode() ?: 0)
        result = 31 * result + (translate?.hashCode() ?: 0)
        result = 31 * result + downloadLinks.hashCode()
        result = 31 * result + (size?.hashCode() ?: 0)
        result = 31 * result + (format?.hashCode() ?: 0)
        result = 31 * result + (genreComplex?.hashCode() ?: 0)
        result = 31 * result + genres.hashCode()
        result = 31 * result + (sequenceComplex?.hashCode() ?: 0)
        result = 31 * result + sequences.hashCode()
        result = 31 * result + authors.hashCode()
        result = 31 * result + (bookInfo?.hashCode() ?: 0)
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + read.hashCode()
        result = 31 * result + downloaded.hashCode()
        result = 31 * result + (previewUrl?.hashCode() ?: 0)
        result = 31 * result + (preferredFormat?.hashCode() ?: 0)
        result = 31 * result + (bookLink?.hashCode() ?: 0)
        result = 31 * result + (bookLanguage?.hashCode() ?: 0)
        return result
    }
}