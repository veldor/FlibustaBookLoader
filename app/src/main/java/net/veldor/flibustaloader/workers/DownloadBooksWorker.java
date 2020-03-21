package net.veldor.flibustaloader.workers;

import android.app.Notification;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.work.ForegroundInfo;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.database.AppDatabase;
import net.veldor.flibustaloader.database.dao.BooksDownloadScheduleDao;
import net.veldor.flibustaloader.database.dao.DownloadedBooksDao;
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule;
import net.veldor.flibustaloader.database.entity.DownloadedBooks;
import net.veldor.flibustaloader.ecxeptions.BookNotFoundException;
import net.veldor.flibustaloader.ecxeptions.FlibustaUnreachableException;
import net.veldor.flibustaloader.ecxeptions.TorNotLoadedException;
import net.veldor.flibustaloader.http.ExternalVpnVewClient;
import net.veldor.flibustaloader.http.TorWebClient;
import net.veldor.flibustaloader.notificatons.Notificator;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static net.veldor.flibustaloader.notificatons.Notificator.DOWNLOAD_PROGRESS_NOTIFICATION;
import static net.veldor.flibustaloader.view_models.MainViewModel.MULTIPLY_DOWNLOAD;

public class DownloadBooksWorker extends Worker {
    private final Notificator mNotificator;
    private int mBooksCount;

    public DownloadBooksWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mNotificator = App.getInstance().getNotificator();
    }

    public static void dropDownloadsQueue() {
        // удалю из базы данных всю очередь скачивания
        AppDatabase db = App.getInstance().mDatabase;
        BooksDownloadScheduleDao dao = db.booksDownloadScheduleDao();
        List<BooksDownloadSchedule> schedule = dao.getAllBooks();
        if (schedule != null && schedule.size() > 0) {
            for (BooksDownloadSchedule b : schedule) {
                dao.delete(b);
            }
        }
    }

    public static void removeFromQueue(BooksDownloadSchedule scheduleItem) {
        AppDatabase db = App.getInstance().mDatabase;
        BooksDownloadScheduleDao dao = db.booksDownloadScheduleDao();
        dao.delete(scheduleItem);
    }

    public static boolean noActiveDownloadProcess() {
        // проверю наличие активных процессов скачивания
        ListenableFuture<List<WorkInfo>> info = WorkManager.getInstance(App.getInstance()).getWorkInfosForUniqueWork(MULTIPLY_DOWNLOAD);
        try {
            List<WorkInfo> results = info.get();
            if (results == null || results.size() == 0) {
                return true;
            }
            for (WorkInfo v : results) {
                WorkInfo.State status = v.getState();
                if (status.equals(WorkInfo.State.ENQUEUED) || status.equals(WorkInfo.State.RUNNING)) {
                    return false;
                }
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static LiveData<List<WorkInfo>> getDownloadProgress() {
        return WorkManager.getInstance(App.getInstance()).getWorkInfosForUniqueWorkLiveData(MULTIPLY_DOWNLOAD);
    }

    public static void skipFirstBook() {
        AppDatabase db = App.getInstance().mDatabase;
        BooksDownloadScheduleDao dao = db.booksDownloadScheduleDao();
        dao.delete(dao.getFirstQueuedBook());
    }

    @NonNull
    @Override
    public Result doWork() {
        // проверю, есть ли в очереди скачивания книги
        AppDatabase db = App.getInstance().mDatabase;
        BooksDownloadScheduleDao dao = db.booksDownloadScheduleDao();
        DownloadedBooksDao downloadBooksDao = db.downloadedBooksDao();
        Boolean reDownload = App.getInstance().isReDownload();
        // получу количество книг на начало скачивания
        mBooksCount = dao.getQueueSize();
        if (mBooksCount > 0) {
            // помечу рабочего важным
            ForegroundInfo info = createForegroundInfo();
            setForegroundAsync(info);
            // создам уведомление о скачивании
            mNotificator.mDownloadScheduleBuilder.setProgress(mBooksCount, 0, true);
            mNotificator.mNotificationManager.notify(DOWNLOAD_PROGRESS_NOTIFICATION, mNotificator.mDownloadScheduleBuilder.build());
            updateDownloadStatusNotification(0);
            BooksDownloadSchedule queuedElement;
            // начну скачивание
            // периодически удостовериваюсь, что работа не отменена
            if (isStopped()) {
                // немедленно прекращаю работу
                mNotificator.cancelBookLoadNotification();
                return Result.success();
            }
            int downloadCounter = 1;
            // пока есть книги в очереди скачивания и работа не остановлена
            while ((queuedElement = dao.getFirstQueuedBook()) != null && !isStopped()) {
                // освежу уведомление
                updateDownloadStatusNotification(downloadCounter);
                // проверю, не загружалась ли уже книга, если загружалась и запрещена повторная загрузка- пропущу её
                if (!reDownload && downloadBooksDao.getBookById(queuedElement.bookId) != null) {
                    dao.delete(queuedElement);
                    continue;
                }
                // загружу книгу
                boolean downloadResult;
                try {
                    updateDownloadStatusNotification(downloadCounter);
                    downloadResult = downloadBook(queuedElement);
                    if (!downloadResult) {
                        // ошибка загрузки книг, выведу сообщение об ошибке
                        mNotificator.showBooksLoadErrorNotification(queuedElement.name);
                        Log.d("surprise", "DownloadBooksWorker doWork: error download!!!");
                        return Result.failure();
                    }
                    if (!isStopped()) {
                        // отмечу книгу как скачанную
                        DownloadedBooks downloadedBook = new DownloadedBooks();
                        downloadedBook.bookId = queuedElement.bookId;
                        downloadBooksDao.insert(downloadedBook);
                        // удалю книгу из очереди скачивания
                        // покажу уведомление о успешной загрузке
                        mNotificator.sendLoadedBookNotification(queuedElement.name, queuedElement.format);
                        dao.delete(queuedElement);
                    }
                } catch (BookNotFoundException e) {
                    // книга недоступна для скачивания в данном формате, удалю её из очереди, выведу уведомление и продолжу загрузку
                    mNotificator.sendBookNotFoundInCurrentFormatNotification(queuedElement);
                    dao.delete(queuedElement);
                }
                ++downloadCounter;
            }
            // цикл закончился, проверю, все ли книги загружены
            mBooksCount = dao.getQueueSize();
            if (mBooksCount == 0 && !isStopped()) {
                // ура, всё загружено, выведу сообщение об успешной загрузке
                mNotificator.showBooksLoadedNotification();
            }
        }
        mNotificator.cancelBookLoadNotification();
        return Result.success();
    }

    private void updateDownloadStatusNotification(int i) {
       /* // уберу сообщение об ошибке загрузки TOR, если оно есть
        mNotificator.cancelTorErrorMessage();
        mNotificator.mDownloadScheduleBuilder.setContentText(String.format(Locale.ENGLISH, App.getInstance().getString(R.string.download_progress_message), i, mBooksCount));
        mNotificator.mDownloadScheduleBuilder.setProgress(mBooksCount,i, false);
        mNotificator.mNotificationManager.notify(DOWNLOAD_PROGRESS_NOTIFICATION, mNotificator.mDownloadScheduleBuilder.build());*/
    }

    private boolean downloadBook(BooksDownloadSchedule book) throws BookNotFoundException {
        if (App.getInstance().isExternalVpn()) {
            Log.d("surprise", "DownloadBooksWorker downloadBook try download trough external vpn");
            return ExternalVpnVewClient.downloadBook(book);
        } else {
            // настрою клиент
            try {
                TorWebClient client = new TorWebClient();
                return client.downloadBook(book);
            } catch (TorNotLoadedException e) {
                // оповещу об ошибке загрузки TOR-а
                mNotificator.showTorNotLoadedNotification();
                return false;
            } catch (FlibustaUnreachableException e) {
                // флибуста лежит
                Log.d("surprise", "DownloadBooksWorker downloadBook: cant find flibusta...");
                return false;
            }
        }
    }


    @NonNull
    private ForegroundInfo createForegroundInfo() {
        // Build a notification
        Notification notification = mNotificator.createMassBookLoadNotification(mBooksCount);
        return new ForegroundInfo(DOWNLOAD_PROGRESS_NOTIFICATION, notification);
    }
}
