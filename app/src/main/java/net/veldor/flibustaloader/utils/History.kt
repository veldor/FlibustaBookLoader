package net.veldor.flibustaloader.utils

import android.util.Log
import net.veldor.flibustaloader.selections.HistoryItem
import java.util.*

class History private constructor() {
    private val mHistory = Stack<HistoryItem>()

    fun addToHistory(item: HistoryItem) {
        mHistory.push(item)
    }

    val isEmpty: Boolean
        get() = mHistory.size == 0

    val lastPage: HistoryItem?
        get() = if (mHistory.size > 0) {
            mHistory.pop()
        } else null

    companion object {
        @kotlin.jvm.JvmStatic
        var instance: History? = null
            get() {
                if (field == null) {
                    field = History()
                }
                return field
            }
            private set
    }
}