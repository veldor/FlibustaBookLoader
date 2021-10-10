package net.veldor.flibustaloader.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule

@Dao
interface BooksDownloadScheduleDao {
    @get:Query("SELECT COUNT(id) FROM BooksDownloadSchedule")
    val queueSize: Int

    @get:Query("SELECT * FROM BooksDownloadSchedule LIMIT 1")
    val firstQueuedBook: BooksDownloadSchedule?

    @Query("SELECT * FROM BooksDownloadSchedule WHERE bookId = :id")
    fun getBookById(id: String?): BooksDownloadSchedule?

    @get:Query("SELECT * FROM BooksDownloadSchedule")
    val allBooks: List<BooksDownloadSchedule>?

    @get:Query("SELECT * FROM BooksDownloadSchedule")
    val allBooksLive: LiveData<List<BooksDownloadSchedule>>?

    @Insert
    fun insert(book: BooksDownloadSchedule?)

    @Delete
    fun delete(book: BooksDownloadSchedule?)
}