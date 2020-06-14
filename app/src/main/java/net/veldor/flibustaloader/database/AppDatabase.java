package net.veldor.flibustaloader.database;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;

import net.veldor.flibustaloader.database.dao.BooksDownloadScheduleDao;
import net.veldor.flibustaloader.database.dao.DownloadedBooksDao;
import net.veldor.flibustaloader.database.dao.ReadedBooksDao;
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule;
import net.veldor.flibustaloader.database.entity.DownloadedBooks;
import net.veldor.flibustaloader.database.entity.ReadedBooks;

@Database(entities = {ReadedBooks.class, DownloadedBooks.class, BooksDownloadSchedule.class}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ReadedBooksDao readedBooksDao();
    public abstract DownloadedBooksDao downloadedBooksDao();
    public abstract BooksDownloadScheduleDao booksDownloadScheduleDao();

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
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("create table BooksDownloadSchedule (id integer primary key autoincrement NOT NULL, bookId TEXT NOT NULL, link TEXT NOT NULL, name TEXT NOT NULL, size TEXT NOT NULL, author TEXT NOT NULL, format TEXT NOT NULL);");
        }
    };
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE BooksDownloadSchedule ADD COLUMN authorDirName TEXT NOT NULL");
            database.execSQL("ALTER TABLE BooksDownloadSchedule ADD COLUMN sequenceDirName TEXT");
        }
    };
}
