package net.veldor.flibustaloader.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import net.veldor.flibustaloader.database.entity.ReadedBooks;

import java.util.List;

@Dao
public interface ReadedBooksDao {
    @Query("SELECT * FROM ReadedBooks WHERE bookId = :id")
    ReadedBooks getBookById(String id);


    @Query("SELECT * FROM ReadedBooks")
    List<ReadedBooks> getAllBooks();

    @Insert
    void insert(ReadedBooks book);

/*    @Update
    void update(ReadedBooks book);

    @Delete
    void delete(ReadedBooks book);*/
}
