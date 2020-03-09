package net.veldor.flibustaloader.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule;
import net.veldor.flibustaloader.database.entity.ReadedBooks;

import java.util.List;

@Dao
public interface BooksDownloadScheduleDao {
    @Query("SELECT COUNT(id) FROM BooksDownloadSchedule")
    int getQueueSize();

    @Query("SELECT * FROM BooksDownloadSchedule LIMIT 1")
    BooksDownloadSchedule getFirstQueuedBook();

    @Query("SELECT * FROM BooksDownloadSchedule WHERE bookId = :id")
    BooksDownloadSchedule getBookById(String id);


    @Query("SELECT * FROM BooksDownloadSchedule")
    List<BooksDownloadSchedule> getAllBooks();


    @Query("SELECT * FROM BooksDownloadSchedule")
    LiveData<List<BooksDownloadSchedule>> getAllBooksLive();

    @Insert
    void insert(BooksDownloadSchedule book);

/*    @Update
    void update(ReadedBooks book);
*/
    @Delete
    void delete(BooksDownloadSchedule book);
}
