package net.veldor.flibustaloader.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import net.veldor.flibustaloader.database.entity.Bookmark

@Dao
interface BookmarksDao {
    @Query("SELECT * FROM Bookmark WHERE id = :id")
    fun getBookmarkById(id: String?): Bookmark?

    @Query("SELECT * FROM Bookmark WHERE id = :id")
    fun getBookmarkById(id: Int): Bookmark?

    @get:Query("SELECT * FROM Bookmark ORDER BY id DESC")
    val allBookmarks: MutableList<Bookmark>

    @Insert
    fun insert(book: Bookmark?)

    @Delete
    fun delete(book: Bookmark?)

    @Query("SELECT * FROM Bookmark WHERE name = :name AND link = :link")
    fun getDuplicate(name: String?, link: String?): List<Bookmark?>? /*    @Update
    void update(ReadedBooks book);
*/
}