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
import net.veldor.flibustaloader.ecxeptions.ConnectionLostException;
import net.veldor.flibustaloader.ecxeptions.TorNotLoadedException;
import net.veldor.flibustaloader.http.ExternalVpnVewClient;
import net.veldor.flibustaloader.http.TorWebClient;
import net.veldor.flibustaloader.notificatons.Notificator;
import net.veldor.flibustaloader.ui.BaseActivity;
import net.veldor.flibustaloader.utils.FilesHandler;
import net.veldor.flibustaloader.utils.MyPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static net.veldor.flibustaloader.notificatons.Notificator.DOWNLOAD_PROGRESS_NOTIFICATION;
import static net.veldor.flibustaloader.view_models.MainViewModel.MULTIPLY_DOWNLOAD;

public class DownloadBooksWorker extends Worker {
    private final Notificator mNotificator;
    public static boolean downloadInProgress = false;
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
        // уведомлю, что размер списка закачек изменился
        BaseActivity.sLiveDownloadScheduleCount.postValue(true);
    }

    public static void removeFromQueue(BooksDownloadSchedule scheduleItem) {
        AppDatabase db = App.getInstance().mDatabase;
        BooksDownloadScheduleDao dao = db.booksDownloadScheduleDao();
        dao.delete(scheduleItem);
        // уведомлю, что размер списка закачек изменился
        BaseActivity.sLiveDownloadScheduleCount.postValue(true);
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
        // уведомлю, что размер списка закачек изменился
        BaseActivity.sLiveDownloadScheduleCount.postValue(true);
    }

    @NonNull
    @Override
    public Result doWork() {
        downloadInProgress = true;
        long downloadStartTime = System.currentTimeMillis();
        if (App.getInstance().useMirror) {
            // оповещу о невозможности скачивания книг с альтернативного зеркала
            mNotificator.notifyDownloadFromMirror();
        }
        ArrayList<BooksDownloadSchedule> downloadErrors = new ArrayList<>();
        // проверю, есть ли в очереди скачивания книги
        AppDatabase db = App.getInstance().mDatabase;
        BooksDownloadScheduleDao dao = db.booksDownloadScheduleDao();
        DownloadedBooksDao downloadBooksDao = db.downloadedBooksDao();
        Boolean reDownload = App.getInstance().isReDownload();
        try {
            // получу количество книг на начало скачивания
            int mBooksCount = dao.getQueueSize();
            int bookDownloadsWithErrors = 0;
            if (mBooksCount > 0) {
                // помечу рабочего важным
                ForegroundInfo info = createForegroundInfo();
                setForegroundAsync(info);
                // создам уведомление о скачивании
                mNotificator.mDownloadScheduleBuilder.setProgress(mBooksCount, 0, true);
                mNotificator.mNotificationManager.notify(DOWNLOAD_PROGRESS_NOTIFICATION, mNotificator.mDownloadScheduleBuilder.build());
                BooksDownloadSchedule queuedElement;
                // начну скачивание
                // периодически удостовериваюсь, что работа не отменена
                if (isStopped()) {
                    // немедленно прекращаю работу
                    mNotificator.cancelBookLoadNotification();
                    downloadInProgress = false;
                    return Result.success();
                }
                int downloadCounter = 1;
                // пока есть книги в очереди скачивания и работа не остановлена
                while ((queuedElement = dao.getFirstQueuedBook()) != null && !isStopped()) {
                    queuedElement.name = queuedElement.name.replaceAll("\\p{C}", "");
                    mNotificator.updateDownloadProgress(dao.getQueueSize() + downloadCounter - 1, downloadCounter, downloadStartTime);
                    // проверю, не загружалась ли уже книга, если загружалась и запрещена повторная загрузка- пропущу её
                    if (!reDownload && downloadBooksDao.getBookById(queuedElement.bookId) != null) {
                        dao.delete(queuedElement);
                        // уведомлю, что размер списка закачек изменился
                        BaseActivity.sLiveDownloadScheduleCount.postValue(true);
                        continue;
                    }
                    // загружу книгу
                    try {
                        downloadBook(queuedElement);
                        if (!isStopped()) {
                            if (queuedElement.loaded) {
                                // отмечу книгу как скачанную
                                DownloadedBooks downloadedBook = new DownloadedBooks();
                                downloadedBook.bookId = queuedElement.bookId;
                                downloadBooksDao.insert(downloadedBook);
                                // удалю книгу из очереди скачивания
                                // покажу уведомление о успешной загрузке
                                mNotificator.sendLoadedBookNotification(queuedElement);
                                dao.delete(queuedElement);
                                // уведомлю, что размер списка закачек изменился
                                BaseActivity.sLiveDownloadScheduleCount.postValue(true);
                                // оповещу о скачанной книге
                                App.getInstance().mLiveDownloadedBookId.postValue(queuedElement.bookId);
                                // если не клянчил донаты- поклянчу :)
                                if (!MyPreferences.getInstance().askedForDonation()) {
                                    mNotificator.begDonation();
                                    MyPreferences.getInstance().setDonationBegged();
                                }
                            } else {
                                Log.d("surprise", "DownloadBooksWorker doWork 178: book not loaded or loaded with zero size");
                                mNotificator.sendBookNotFoundInCurrentFormatNotification(queuedElement);
                                bookDownloadsWithErrors++;
                                downloadErrors.add(queuedElement);
                                dao.delete(queuedElement);
                            }
                        }
                    } catch (BookNotFoundException e) {
                        Log.d("surprise", "DownloadBooksWorker doWork 173: catch book not found error");
                        // ошибка загрузки книг, выведу сообщение об ошибке
                        mNotificator.sendBookNotFoundInCurrentFormatNotification(queuedElement);
                        bookDownloadsWithErrors++;
                        downloadErrors.add(queuedElement);
                        dao.delete(queuedElement);
                        // уведомлю, что размер списка закачек изменился
                        BaseActivity.sLiveDownloadScheduleCount.postValue(true);
                    } catch (TorNotLoadedException e) {
                        Log.d("surprise", "DownloadBooksWorker doWork 172: catch tor load exception, download work stopped");
                        e.printStackTrace();
                        // при ошибке загрузки TOR остановлю работу
                        if (downloadErrors.size() > 0) {
                            for (BooksDownloadSchedule b :
                                    downloadErrors) {
                                dao.insert(b);
                                downloadErrors.remove(b);
                            }
                        }
                        mNotificator.cancelBookLoadNotification();
                        mNotificator.showTorNotLoadedNotification();
                        downloadInProgress = false;
                        return Result.success();
                    }
                    ++downloadCounter;
                }
                // цикл закончился, проверю, все ли книги загружены
                mBooksCount = dao.getQueueSize();
                if (mBooksCount == 0 && !isStopped()) {
                    // ура, всё загружено, выведу сообщение об успешной загрузке
                    mNotificator.showBooksLoadedNotification(bookDownloadsWithErrors);
                    // Добавлю все книги с ошибками обратно в список загрузки
                    for (BooksDownloadSchedule b :
                            downloadErrors) {
                        dao.insert(b);
                        downloadErrors.remove(b);
                    }
                    // уведомлю, что размер списка закачек изменился
                    BaseActivity.sLiveDownloadScheduleCount.postValue(true);
                }
            }
        } finally {
            if (downloadErrors.size() > 0) {
                for (BooksDownloadSchedule b :
                        downloadErrors) {
                    dao.insert(b);
                    downloadErrors.remove(b);
                }
            }
        }
        mNotificator.cancelBookLoadNotification();
        downloadInProgress = false;
        return Result.success();
    }

    private void downloadBook(BooksDownloadSchedule book) throws BookNotFoundException, TorNotLoadedException {
        if (App.getInstance().isExternalVpn()) {
            Log.d("surprise", "DownloadBooksWorker downloadBook try download trough external vpn");
            ExternalVpnVewClient.downloadBook(book);
        } else {
            // настрою клиент
            try {
                TorWebClient client = new TorWebClient();
                client.downloadBook(book);
            } catch (ConnectionLostException e) {
                mNotificator.showTorNotLoadedNotification();
                e.printStackTrace();
                throw new TorNotLoadedException();
            }
        }
    }


    @NonNull
    private ForegroundInfo createForegroundInfo() {
        // Build a notification
        Notification notification = mNotificator.createMassBookLoadNotification();
        return new ForegroundInfo(DOWNLOAD_PROGRESS_NOTIFICATION, notification);
    }
}
