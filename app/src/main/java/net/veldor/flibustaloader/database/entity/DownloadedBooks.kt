package net.veldor.flibustaloader.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class DownloadedBooks {
    @PrimaryKey(autoGenerate = true)
    var id = 0
    var bookId = ""
}