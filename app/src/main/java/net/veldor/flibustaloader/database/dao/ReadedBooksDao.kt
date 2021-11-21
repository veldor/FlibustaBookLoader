package net.veldor.flibustaloader.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import net.veldor.flibustaloader.database.entity.ReadedBooks

@Dao
interface ReadedBooksDao {
    @Query("SELECT * FROM ReadedBooks WHERE bookId = :id")
    fun getBookById(id: String?): ReadedBooks?

    @get:Query("SELECT * FROM ReadedBooks")
    val allBooks: List<ReadedBooks?>?

    @Insert
    fun insert(book: ReadedBooks?)
    @Delete
    fun delete(book: ReadedBooks?)
}