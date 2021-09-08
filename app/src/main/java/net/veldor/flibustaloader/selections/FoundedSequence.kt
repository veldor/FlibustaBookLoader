package net.veldor.flibustaloader.selections

import java.io.Serializable

class FoundedSequence : FoundedItem, Serializable {
    @JvmField
    var title: String? = null
    var content: String? = null
    @JvmField
    var link: String? = null
}