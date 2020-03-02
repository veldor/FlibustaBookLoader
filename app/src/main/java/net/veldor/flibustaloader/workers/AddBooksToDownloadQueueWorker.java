package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.util.Log;
import android.util.SparseBooleanArray;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.selections.FoundedItem;

import java.util.ArrayList;

public class AddBooksToDownloadQueueWorker extends Worker {


    public AddBooksToDownloadQueueWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // получу предпочтительный формат
        String preferredFormat = App.getInstance().getFavoriteMime();
        // получу список книг
        SparseBooleanArray selectedBooksList = App.getInstance().mDownloadSelectedBooks;
        // отсортирую книги для загрузки
        ArrayList<FoundedItem> mBooks = App.getInstance().mParsedResult.getValue();
        FoundedBook book;
        ArrayList<FoundedBook> prepareForDownload = new ArrayList<>();
        if (mBooks == null || mBooks.size() == 0 || !(mBooks.get(0) instanceof FoundedBook)) {
            return Result.success();
        }
        if(selectedBooksList != null){
            for (int counter = 0; counter < mBooks.size(); counter++) {
                // если книга выбрана для скачивания- добавлю её в список для скачивания
                if (selectedBooksList.get(counter)) {
                    book = (FoundedBook) mBooks.get(counter);
                    book.preferredFormat = preferredFormat;
                    prepareForDownload.add(book);
                }
            }
        }
        else{
            for (int counter = 0; counter < mBooks.size(); counter++) {
                // если книга выбрана для скачивания- добавлю её в список для скачивания
                    book = (FoundedBook) mBooks.get(counter);
                    book.preferredFormat = preferredFormat;
                    prepareForDownload.add(book);
            }
        }
        // проверю очередь скачивания
        ArrayList<FoundedBook> downloadSchedule = App.getInstance().mDownloadSchedule.getValue();
        if (downloadSchedule != null){
            downloadSchedule.addAll(prepareForDownload);
            Log.d("surprise", "AddBooksToDownloadQueueWorker doWork download schedule length is " + downloadSchedule.size());
        }
        return Result.success();
    }
}
