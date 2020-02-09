package net.veldor.flibustaloader.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import net.veldor.flibustaloader.database.entity.DownloadedBooks;

@Dao
public interface DownloadedBooksDao {
    @Query("SELECT * FROM downloadedbooks WHERE bookId = :id")
    DownloadedBooks getBookById(String id);

    @Insert
    void insert(DownloadedBooks book);
/*
    @Update
    void update(DownloadedBooks book);

    @Delete
    void delete(DownloadedBooks book);*/
}
