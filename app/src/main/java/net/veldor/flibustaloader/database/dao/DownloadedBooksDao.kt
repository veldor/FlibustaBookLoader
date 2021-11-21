package net.veldor.flibustaloader.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import net.veldor.flibustaloader.database.entity.DownloadedBooks

@Dao
interface DownloadedBooksDao {
    @Query("SELECT * FROM downloadedbooks WHERE bookId = :id")
    fun getBookById(id: String?): DownloadedBooks?

    @get:Query("SELECT * FROM downloadedbooks")
    val allBooks: List<DownloadedBooks?>?

    @get:Query("SELECT * FROM downloadedbooks")
    val allBooksLive: LiveData<List<DownloadedBooks?>?>?

    @Insert
    fun insert(book: DownloadedBooks?)
    @Delete
    fun delete(book: DownloadedBooks?)
}