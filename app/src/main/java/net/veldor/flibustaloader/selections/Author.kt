package net.veldor.flibustaloader.selections

import java.io.Serializable

class Author : FoundedItem, Serializable {
    @JvmField
    var name: String? = null
    @JvmField
    var uri: String? = null
    var content: String? = null
    var link: String? = null
    @JvmField
    var id: String? = null
}