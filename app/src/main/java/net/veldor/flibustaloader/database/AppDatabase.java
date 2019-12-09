package net.veldor.flibustaloader.database;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;

import net.veldor.flibustaloader.database.dao.ReadedBooksDao;
import net.veldor.flibustaloader.database.entity.ReadedBooks;

@Database(entities = {ReadedBooks.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ReadedBooksDao readedBooksDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(final SupportSQLiteDatabase database) {
            database.execSQL("drop table ReadedBooks;");
            database.execSQL("create table ReadedBooks (id integer primary key autoincrement NOT NULL, bookId TEXT NOT NULL);");
        }
    };
}
