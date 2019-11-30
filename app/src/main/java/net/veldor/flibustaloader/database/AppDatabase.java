package net.veldor.flibustaloader.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import net.veldor.flibustaloader.database.dao.ReadedBooksDao;
import net.veldor.flibustaloader.database.entity.ReadedBooks;

@Database(entities = {ReadedBooks.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ReadedBooksDao readedBooksDao();
}
