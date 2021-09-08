package net.veldor.flibustaloader.database.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity
class BooksDownloadSchedule {
    @PrimaryKey(autoGenerate = true)
    var id = 0
    var bookId = ""
    @JvmField
    var link = ""
    @JvmField
    var name = ""
    var size = ""
    var author = ""
    @JvmField
    var format = ""
    @JvmField
    var authorDirName = ""
    @JvmField
    var sequenceDirName = ""
    @JvmField
    var reservedSequenceName = ""

    @JvmField
    @Ignore
    var loaded = false
}