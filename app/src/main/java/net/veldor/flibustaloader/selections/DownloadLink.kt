package net.veldor.flibustaloader.selections

import java.io.Serializable

class DownloadLink : Serializable {
    var url: String? = null
    var id: String? = null
    @JvmField
    var mime: String? = null
    var name: String? = null
    var author: String? = null
    var size: String? = null
    var authorDirName: String? = null
    var sequenceDirName: String? = null
    var reservedSequenceName: String? = null
}