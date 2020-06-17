package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.util.SparseBooleanArray;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.database.AppDatabase;
import net.veldor.flibustaloader.database.dao.BooksDownloadScheduleDao;
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule;
import net.veldor.flibustaloader.selections.DownloadLink;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.ui.BaseActivity;
import net.veldor.flibustaloader.ui.OPDSActivity;
import net.veldor.flibustaloader.utils.Grammar;
import net.veldor.flibustaloader.utils.MimeTypes;

import java.util.ArrayList;

public class AddBooksToDownloadQueueWorker extends Worker {


    public AddBooksToDownloadQueueWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void addLink(DownloadLink downloadLink) {
        addDownloadLink(App.getInstance().mDatabase.booksDownloadScheduleDao(), downloadLink);
        // уведомлю, что размер списка закачек изменился
        BaseActivity.sLiveDownloadScheduleCount.postValue(true);
    }

    @NonNull
    @Override
    public Result doWork() {
        // проверю, нужно ли подгружать только незагруженные
        boolean redownload = App.getInstance().mDownloadUnloaded;

        // получу предпочтительный формат
        String preferredFormat = App.getInstance().getFavoriteMime();
        if (preferredFormat == null) {
            preferredFormat = App.getInstance().mSelectedFormat;
        }
        // если выбраны книги для загрузки- получу их ID
        SparseBooleanArray selectedBooksList = App.getInstance().mDownloadSelectedBooks;
        // получу весь список книг
        ArrayList<FoundedBook> mBooks = OPDSActivity.sBooksForDownload;
        // проверю, что книги есть и готовы к загрузке
        if (mBooks == null || mBooks.size() == 0) {
            return Result.success();
        }

        FoundedBook book;
        ArrayList<FoundedBook> prepareForDownload = new ArrayList<>();
        // если для загрузки выбран список книг
        if (selectedBooksList != null) {
            for (int counter = 0; counter < mBooks.size(); counter++) {
                // если книга выбрана для скачивания- добавлю её в список для скачивания
                if (selectedBooksList.get(counter)) {
                    book = mBooks.get(counter);
                    book.preferredFormat = preferredFormat;
                    prepareForDownload.add(book);
                }
            }
        }
        // иначе- добавлю все книги в список для загрузки
        else {
            for (int counter = 0; counter < mBooks.size(); counter++) {
                book = mBooks.get(counter);
                // тут проверю, если запрещено загружать ранее загруженные книги повторно- пропущу ранее загруженные
                if (redownload && book.downloaded) {
                    continue;
                }
                book.preferredFormat = preferredFormat;
                prepareForDownload.add(book);
            }
        }
        // теперь добавлю книги в базу данных для загрузки
        AppDatabase database = App.getInstance().mDatabase;
        BooksDownloadScheduleDao dao = database.booksDownloadScheduleDao();
        // проверю правило загружать строго выбранный формат
        Boolean loadOnlySelectedFormat = App.getInstance().isSaveOnlySelected();
        for (FoundedBook book1 : prepareForDownload) {
            // проверю, есть ли предпочтительная ссылка на скачивание книги
            ArrayList<DownloadLink> links = book1.downloadLinks;
            if (links.size() > 0) {
                DownloadLink link = null;
                DownloadLink fb2Link = null;
                if (links.size() == 1 && links.get(0).mime.contains(preferredFormat)) {
                    link = links.get(0);
                } else {
                    int counter = 0;
                    while (counter < links.size()) {
                        if (links.get(counter).mime.contains(preferredFormat)) {
                            link = links.get(counter);
                            break;
                        }
                        if (links.get(counter).mime.equals(MimeTypes.getFullMime("fb2"))) {
                            fb2Link = links.get(counter);
                        }
                        counter++;
                    }
                }
                // если не найдена предпочтительная ссылка на книгу и разрешено загружать книги вне выбранного формата
                if (link == null && !loadOnlySelectedFormat) {
                    // попробую скачать книгу в fb2, как в самом распространённом формате
                    if (fb2Link != null) {
                        link = fb2Link;
                    } else {
                        // просто возьму первую ссылку
                        link = links.get(0);
                    }
                }
                // если ссылка всё-же не найдена- перехожу к следующей книге
                if (link == null) {
                    continue;
                }
                addDownloadLink(dao, link);
                // уведомлю, что размер списка закачек изменился
                BaseActivity.sLiveDownloadScheduleCount.postValue(true);
            }
        }
        return Result.success();
    }

    private static void addDownloadLink(BooksDownloadScheduleDao dao, DownloadLink link) {
        // добавлю книги в очередь скачивания
        BooksDownloadSchedule newScheduleElement = new BooksDownloadSchedule();
        newScheduleElement.bookId = link.id;
        newScheduleElement.author = link.author;
        newScheduleElement.link = link.url;
        newScheduleElement.format = link.mime;
        newScheduleElement.size = link.size;
        newScheduleElement.authorDirName = link.authorDirName;
        newScheduleElement.sequenceDirName = link.sequenceDirName;
        newScheduleElement.reservedSequenceName = link.reservedSequenceName;
        // определю имя ссылки для скачивания =======================================
        String author_last_name;
        if (link.author != null && !link.author.isEmpty()) {
            newScheduleElement.author = link.author;
            int delimiter = link.author.indexOf(" ");
            if (delimiter >= 0) {
                author_last_name = link.author.substring(0, delimiter);
            } else {
                author_last_name = link.author;
            }
        } else {
            author_last_name = "Автор неизвестен";
            newScheduleElement.author = "Автор неизвестен";
        }
        String book_name = link.name.replaceAll(" ", "_").replaceAll("[^\\d\\w-_]", "");
        String book_mime = MimeTypes.getDownloadMime(link.mime);
        // если сумма символов меньше 255- создаю полное имя
        if (author_last_name.length() + book_name.length() + book_mime.length() + 2 < 255 / 2 - 6) {
            newScheduleElement.name = author_last_name + "_" + book_name + "_" + Grammar.getRandom() + "." + book_mime;
        } else {
            // сохраняю книгу по имени автора и тому, что влезет от имени книги
            newScheduleElement.name = author_last_name + "_" + book_name.substring(0, 127 - (author_last_name.length() + book_mime.length() + 2 + 6)) + "_" + Grammar.getRandom() + "." + book_mime;
        }
        //===========================================================================
        dao.insert(newScheduleElement);
    }
}
