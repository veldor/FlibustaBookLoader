package net.veldor.flibustaloader.selections

class HistoryItem(
    val nextPageLink: String?,
    val clickedItem: Long,
    val loadedValues: Int,
    val filteredValues: Int,
    val results: ArrayList<FoundedEntity>
)