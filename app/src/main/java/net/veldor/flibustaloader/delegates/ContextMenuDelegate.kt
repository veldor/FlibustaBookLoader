package net.veldor.flibustaloader.delegates

import android.view.View
import net.veldor.flibustaloader.selections.Book
import net.veldor.flibustaloader.selections.FoundedEntity

interface ContextMenuDelegate {
    fun contextMenuCreated(delegate: Book)
}