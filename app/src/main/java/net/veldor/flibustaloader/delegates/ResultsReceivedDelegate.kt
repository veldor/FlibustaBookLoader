package net.veldor.flibustaloader.delegates

import android.view.View
import net.veldor.flibustaloader.selections.FoundedEntity
import net.veldor.flibustaloader.selections.SearchResult

interface ResultsReceivedDelegate {
    fun resultsReceived(results: SearchResult)
}