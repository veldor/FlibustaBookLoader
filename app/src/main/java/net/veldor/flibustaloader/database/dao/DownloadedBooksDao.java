package net.veldor.flibustaloader.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import net.veldor.flibustaloader.database.entity.DownloadedBooks;

import java.util.List;

@Dao
public interface DownloadedBooksDao {
    @Query("SELECT * FROM downloadedbooks WHERE bookId = :id")
    DownloadedBooks getBookById(String id);

    @Query("SELECT * FROM downloadedbooks")
    List<DownloadedBooks> getAllBooks();

    @Query("SELECT * FROM downloadedbooks")
    LiveData<List<DownloadedBooks>> getAllBooksLive();

    @Insert
    void insert(DownloadedBooks book);
/*
    @Update
    void update(DownloadedBooks book);

    @Delete
    void delete(DownloadedBooks book);*/
}
