package net.veldor.flibustaloader.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import net.veldor.flibustaloader.database.entity.ReadBooks;

@Dao
public interface ReadBooksDao {
    @Query("SELECT * FROM ReadBooks WHERE bookId = :id")
    ReadBooks getBookById(String id);

    @Insert
    void insert(ReadBooks book);

/*    @Update
    void update(ReadBooks book);

    @Delete
    void delete(ReadBooks book);*/
}
