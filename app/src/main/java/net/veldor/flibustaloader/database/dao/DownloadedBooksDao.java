package net.veldor.flibustaloader.database.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import net.veldor.flibustaloader.database.entity.DownloadedBooks;
import net.veldor.flibustaloader.database.entity.ReadedBooks;

@Dao
public interface DownloadedBooksDao {
    @Query("SELECT * FROM downloadedbooks WHERE bookId = :id")
    DownloadedBooks getBookById(String id);

    @Insert
    void insert(DownloadedBooks book);

    @Update
    void update(DownloadedBooks book);

    @Delete
    void delete(DownloadedBooks book);
}
