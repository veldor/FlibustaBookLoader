package net.veldor.flibustaloader.selections

class SearchResult {
    var appended: Boolean = false
    var size: Int = 0
    var nextPageLink: String? = null
    lateinit var results: ArrayList<FoundedEntity>

}