package net.veldor.flibustaloader.database.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import net.veldor.flibustaloader.database.entity.ReadedBooks;

@Dao
public interface ReadedBooksDao {
    @Query("SELECT * FROM readedbooks WHERE bookId = :id")
    ReadedBooks getBookById(String id);

    @Insert
    void insert(ReadedBooks book);

    @Update
    void update(ReadedBooks book);

    @Delete
    void delete(ReadedBooks book);
}
