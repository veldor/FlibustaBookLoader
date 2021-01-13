package net.veldor.flibustaloader.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import net.veldor.flibustaloader.database.entity.Bookmark;

import java.util.List;

@Dao
public interface BookmarksDao {

    @Query("SELECT * FROM Bookmark WHERE id = :id")
    Bookmark getBookmarkById(String id);

    @Query("SELECT * FROM Bookmark WHERE id = :id")
    Bookmark getBookmarkById(int id);


    @Query("SELECT * FROM Bookmark ORDER BY id DESC")
    List<Bookmark> getAllBookmarks();

    @Insert
    void insert(Bookmark book);

    @Delete
    void delete(Bookmark book);

    @Query("SELECT * FROM Bookmark WHERE name = :name AND link = :link")
    List<Bookmark> getDuplicate(String name, String link);

/*    @Update
    void update(ReadedBooks book);
*/
}
