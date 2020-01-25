package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.OPDSActivity;
import net.veldor.flibustaloader.database.AppDatabase;
import net.veldor.flibustaloader.database.dao.DownloadedBooksDao;
import net.veldor.flibustaloader.database.dao.ReadedBooksDao;
import net.veldor.flibustaloader.database.entity.DownloadedBooks;
import net.veldor.flibustaloader.database.entity.ReadedBooks;

public class DatabaseWorker extends Worker {
    public static final int INSERT_BOOK = 1;
    public static final String WORK_TYPE = "work type";

    public DatabaseWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data data = getInputData();
        int requestedWork = data.getInt(WORK_TYPE, 0);
        switch (requestedWork) {
            case INSERT_BOOK:
                String id = data.getString(OPDSActivity.BOOK_ID);
                insertBook(id);
        }
        return Result.success();
    }

    private void insertBook(String id) {
        AppDatabase db = App.getInstance().mDatabase;
        ReadedBooks book = new ReadedBooks();
        book.bookId = id;
        ReadedBooksDao bookDao = db.readedBooksDao();
        bookDao.insert(book);
    }

    static void makeBookDownloaded(String id) {
        Log.d("surprise", "makeBookDownloaded: add " + id);
        AppDatabase db = App.getInstance().mDatabase;
        DownloadedBooks book = new DownloadedBooks();
        book.bookId = id;
        DownloadedBooksDao bookDao = db.downloadedBooksDao();
        if (bookDao.getBookById(id) == null)
            bookDao.insert(book);
    }
}
