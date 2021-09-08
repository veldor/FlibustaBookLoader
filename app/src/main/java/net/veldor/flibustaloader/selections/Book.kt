package net.veldor.flibustaloader.selections

import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.Serializable

class Book : FoundedItem, Serializable {
    @JvmField
    var name: String? = null
    @JvmField
    var author: String? = null
    @JvmField
    var size: String? = null
    @JvmField
    var extension: String? = null
    @JvmField
    var file: DocumentFile? = null
    @JvmField
    var fileCompat: File? = null
}