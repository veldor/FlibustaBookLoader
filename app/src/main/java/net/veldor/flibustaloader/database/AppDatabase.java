package net.veldor.flibustaloader.database;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;

import net.veldor.flibustaloader.database.dao.DownloadedBooksDao;
import net.veldor.flibustaloader.database.dao.ReadedBooksDao;
import net.veldor.flibustaloader.database.entity.DownloadedBooks;
import net.veldor.flibustaloader.database.entity.ReadedBooks;

@Database(entities = {ReadedBooks.class, DownloadedBooks.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ReadedBooksDao readedBooksDao();
    public abstract DownloadedBooksDao downloadedBooksDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("drop table ReadedBooks;");
            database.execSQL("create table ReadedBooks (id integer primary key autoincrement NOT NULL, bookId TEXT NOT NULL);");
        }
    };
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("create table DownloadedBooks (id integer primary key autoincrement NOT NULL, bookId TEXT NOT NULL);");
        }
    };
}
