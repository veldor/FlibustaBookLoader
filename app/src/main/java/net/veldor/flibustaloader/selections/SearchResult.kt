package net.veldor.flibustaloader.selections

class SearchResult {
    var appended: Boolean = false
    var size: Int = 0
    var filtered: Int = 0
    var type: String? = null
    var nextPageLink: String? = null
    lateinit var results: ArrayList<FoundedEntity>
    lateinit var filteredList: ArrayList<FoundedEntity>
    var clickedElementIndex: Long = -1
    var isBackSearch = false

}