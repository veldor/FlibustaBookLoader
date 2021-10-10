package net.veldor.flibustaloader.selections

class SearchResult {
    var appended: Boolean = false
    var size: Int = 0
    var filtered: Int = 0
    var type: String? = null
    var nextPageLink: String? = null
    lateinit var results: ArrayList<FoundedEntity>
    var clickedElementIndex = -1
    var isBackSearch = false

}