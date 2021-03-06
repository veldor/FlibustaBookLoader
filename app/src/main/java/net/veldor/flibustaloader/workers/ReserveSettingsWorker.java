package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.database.AppDatabase;
import net.veldor.flibustaloader.database.entity.Bookmark;
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule;
import net.veldor.flibustaloader.database.entity.DownloadedBooks;
import net.veldor.flibustaloader.database.entity.ReadedBooks;
import net.veldor.flibustaloader.utils.MyFileReader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ReserveSettingsWorker extends Worker {


    private static final int BUFFER = 1024;
    static final String PREF_BACKUP_NAME = "data1";
    static final String DOWNLOADED_BOOKS_BACKUP_NAME = "data2";
    static final String READED_BOOKS_BACKUP_NAME = "data3";
    static final String AUTOFILL_BACKUP_NAME = "data4";
    static final String BOOKS_SUBSCRIBE_BACKUP_NAME = "data5";
    static final String AUTHORS_SUBSCRIBE_BACKUP_NAME = "data6";
    static final String SEQUENCES_SUBSCRIBE_BACKUP_NAME = "data7";
    static final String BOOKMARKS_BACKUP_NAME = "data8";
    static final String DOWNLOAD_SCHEDULE_BACKUP_NAME = "data9";
    public static DocumentFile sSaveDir;
    public static DocumentFile sBackupFile;
    public static File sCompatSaveDir;
    public static File sCompatBackupFile;

    public ReserveSettingsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // пересохраню zip в documentfile
            if (sSaveDir != null && sSaveDir.isDirectory()) {
                // сохраню файл в выбранную директорию
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH-mm-ss", Locale.ENGLISH);
                String filename = "Резервная копия Flibusta downloader от " + sdf.format(new Date());
                sBackupFile = sSaveDir.createFile("application/zip", filename);
            } else {
                return Result.failure();
            }
            File zip = null;
            try {
                File backupDir = new File(Environment.getExternalStorageDirectory(), App.BACKUP_DIR_NAME);
                if (!backupDir.exists()) {
                    boolean result = backupDir.mkdirs();
                    if (result) {
                        Log.d("surprise", "ReserveWorker doWork: dir created");
                    }
                }
                zip = new File(backupDir, App.BACKUP_FILE_NAME);
                FileOutputStream dest;
                dest = new FileOutputStream(zip);
                ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
                byte[] dataBuffer = new byte[BUFFER];
                File sharedPrefsFile;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    sharedPrefsFile = new File(App.getInstance().getDataDir() + "/shared_prefs/net.veldor.flibustaloader_preferences.xml");
                } else {
                    sharedPrefsFile = new File(Environment.getDataDirectory() + "/shared_prefs/net.veldor.flibustaloader_preferences.xml");
                }
                writeToZip(out, dataBuffer, sharedPrefsFile, PREF_BACKUP_NAME);

                // сохраню автозаполнение поиска
                File autocompleteFile = new File(App.getInstance().getFilesDir(), MyFileReader.SEARCH_AUTOCOMPLETE_FILE);
                if (autocompleteFile.isFile()) {
                    writeToZip(out, dataBuffer, autocompleteFile, AUTOFILL_BACKUP_NAME);
                }
                // сохраню подписки
                File subscriptionFile = new File(App.getInstance().getFilesDir(), MyFileReader.BOOKS_SUBSCRIBE_FILE);
                if (autocompleteFile.isFile()) {
                    writeToZip(out, dataBuffer, subscriptionFile, BOOKS_SUBSCRIBE_BACKUP_NAME);
                }
                subscriptionFile = new File(App.getInstance().getFilesDir(), MyFileReader.AUTHORS_SUBSCRIBE_FILE);
                if (autocompleteFile.isFile()) {
                    writeToZip(out, dataBuffer, subscriptionFile, AUTHORS_SUBSCRIBE_BACKUP_NAME);
                }
                subscriptionFile = new File(App.getInstance().getFilesDir(), MyFileReader.SEQUENCES_SUBSCRIBE_FILE);
                if (autocompleteFile.isFile()) {
                    writeToZip(out, dataBuffer, subscriptionFile, SEQUENCES_SUBSCRIBE_BACKUP_NAME);
                }

                // первым делом- получу из базы данных списки прочитанных и скачанных книг
                AppDatabase db = App.getInstance().mDatabase;
                List<DownloadedBooks> books = db.downloadedBooksDao().getAllBooks();
                if (books != null && books.size() > 0) {
                    // создам XML
                    StringBuilder xmlBuilder = new StringBuilder();
                    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><downloaded_books>");
                    for (DownloadedBooks book : books) {
                        xmlBuilder.append("<book id=\"");
                        xmlBuilder.append(book.bookId);
                        xmlBuilder.append("\"/>");
                    }
                    xmlBuilder.append("</downloaded_books>");
                    String text = xmlBuilder.toString();
                    Log.d("surprise", "ReserveSettingsWorker doWork " + text);
                    File f1 = new File(backupDir, "downloaded_books");
                    FileWriter writer = new FileWriter(f1);
                    writer.append(text);
                    writer.flush();
                    writer.close();
                    writeToZip(out, dataBuffer, f1, DOWNLOADED_BOOKS_BACKUP_NAME);
                    boolean result = f1.delete();
                    if (!result) {
                        Log.d("surprise", "ReserveSettingsWorker doWork не удалось удалить временный файл");
                    }
                }
                List<ReadedBooks> rBooks = db.readedBooksDao().getAllBooks();
                if (rBooks != null && rBooks.size() > 0) {
                    // создам XML
                    StringBuilder xmlBuilder = new StringBuilder();
                    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><readed_books>");
                    for (ReadedBooks book : rBooks) {
                        xmlBuilder.append("<book id=\"");
                        xmlBuilder.append(book.bookId);
                        xmlBuilder.append("\"/>");
                    }
                    xmlBuilder.append("</readed_books>");
                    String text = xmlBuilder.toString();
                    File f1 = new File(backupDir, "readed_books");
                    FileWriter writer = new FileWriter(f1);
                    writer.append(text);
                    writer.flush();
                    writer.close();
                    writeToZip(out, dataBuffer, f1, READED_BOOKS_BACKUP_NAME);
                    boolean result = f1.delete();
                    if (!result) {
                        Log.d("surprise", "ReserveSettingsWorker doWork не удалось удалить временный файл");
                    }
                }
                // закладки
                List<Bookmark> rBookmarks = db.bookmarksDao().getAllBookmarks();
                if (rBookmarks != null && rBookmarks.size() > 0) {
                    // создам XML
                    StringBuilder xmlBuilder = new StringBuilder();
                    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><bookmarks>");
                    for (Bookmark bookmark : rBookmarks) {
                        xmlBuilder.append("<bookmark name=\"");
                        xmlBuilder.append(bookmark.name);
                        xmlBuilder.append("\" link=\"");
                        xmlBuilder.append(bookmark.link);
                        xmlBuilder.append("\"/>");
                    }
                    xmlBuilder.append("</bookmarks>");
                    String text = xmlBuilder.toString();
                    File f1 = new File(backupDir, "bookmarks");
                    FileWriter writer = new FileWriter(f1);
                    writer.append(text);
                    writer.flush();
                    writer.close();
                    writeToZip(out, dataBuffer, f1, BOOKMARKS_BACKUP_NAME);
                    boolean result = f1.delete();
                    if (!result) {
                        Log.d("surprise", "ReserveSettingsWorker doWork не удалось удалить временный файл");
                    }
                }
                // список загрузки
                List<BooksDownloadSchedule> rDownloadSchedule = db.booksDownloadScheduleDao().getAllBooks();
                if (rDownloadSchedule != null && rDownloadSchedule.size() > 0) {
                    // создам XML
                    StringBuilder xmlBuilder = new StringBuilder();
                    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><schedule>");
                    for (BooksDownloadSchedule schedule : rDownloadSchedule) {
                        xmlBuilder.append("<item bookId=\"");
                        xmlBuilder.append(schedule.bookId);
                        xmlBuilder.append("\" link=\"");
                        xmlBuilder.append(schedule.link);
                        xmlBuilder.append("\" name=\"");
                        xmlBuilder.append(schedule.name);
                        xmlBuilder.append("\" size=\"");
                        xmlBuilder.append(schedule.size);
                        xmlBuilder.append("\" author=\"");
                        xmlBuilder.append(schedule.author);
                        xmlBuilder.append("\" format=\"");
                        xmlBuilder.append(schedule.format);
                        xmlBuilder.append("\" authorDirName=\"");
                        xmlBuilder.append(schedule.authorDirName);
                        xmlBuilder.append("\" sequenceDirName=\"");
                        xmlBuilder.append(schedule.sequenceDirName);
                        xmlBuilder.append("\" reservedSequenceName=\"");
                        xmlBuilder.append(schedule.reservedSequenceName);
                        xmlBuilder.append("\"/>");
                    }
                    xmlBuilder.append("</schedule>");
                    String text = xmlBuilder.toString();
                    File f1 = new File(backupDir, "schedule");
                    FileWriter writer = new FileWriter(f1);
                    writer.append(text);
                    writer.flush();
                    writer.close();
                    writeToZip(out, dataBuffer, f1, DOWNLOAD_SCHEDULE_BACKUP_NAME);
                    boolean result = f1.delete();
                    if (!result) {
                        Log.d("surprise", "ReserveSettingsWorker doWork не удалось удалить временный файл");
                    }
                }
                out.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                OutputStream fileStream = App.getInstance().getContentResolver().openOutputStream(sBackupFile.getUri());
                if (fileStream != null) {
                    FileInputStream inputStream = new FileInputStream(zip);
                    final byte[] b = new byte[8192];
                    for (int r; (r = inputStream.read(b)) != -1; ) {
                        fileStream.write(b, 0, r);
                    }
                    fileStream.close();
                    inputStream.close();
                    boolean deleteResult = zip.delete();
                    Log.d("surprise", "ReserveSettingsWorker doWork 171: transfer file delete status is " + deleteResult);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                if (sCompatSaveDir != null && sCompatSaveDir.exists() && sCompatSaveDir.isDirectory()) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH-mm-ss", Locale.ENGLISH);
                    String filename = "Резервная копия Flibusta downloader от " + sdf.format(new Date());
                    sCompatBackupFile = new File(sCompatSaveDir, filename);

                    FileOutputStream dest;
                    dest = new FileOutputStream(sCompatBackupFile);
                    ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
                    byte[] dataBuffer = new byte[BUFFER];
                    File sharedPrefsFile;
                    sharedPrefsFile = new File(Environment.getDataDirectory() + "/shared_prefs/net.veldor.flibustaloader_preferences.xml");
                    writeToZip(out, dataBuffer, sharedPrefsFile, PREF_BACKUP_NAME);

                    // сохраню автозаполнение поиска
                    File autocompleteFile = new File(App.getInstance().getFilesDir(), MyFileReader.SEARCH_AUTOCOMPLETE_FILE);
                    if (autocompleteFile.isFile()) {
                        writeToZip(out, dataBuffer, autocompleteFile, AUTOFILL_BACKUP_NAME);
                    }
                    // сохраню подписки
                    File subscriptionFile = new File(App.getInstance().getFilesDir(), MyFileReader.BOOKS_SUBSCRIBE_FILE);
                    if (autocompleteFile.isFile()) {
                        writeToZip(out, dataBuffer, subscriptionFile, BOOKS_SUBSCRIBE_BACKUP_NAME);
                    }
                    subscriptionFile = new File(App.getInstance().getFilesDir(), MyFileReader.AUTHORS_SUBSCRIBE_FILE);
                    if (autocompleteFile.isFile()) {
                        writeToZip(out, dataBuffer, subscriptionFile, AUTHORS_SUBSCRIBE_BACKUP_NAME);
                    }
                    subscriptionFile = new File(App.getInstance().getFilesDir(), MyFileReader.SEQUENCES_SUBSCRIBE_FILE);
                    if (autocompleteFile.isFile()) {
                        writeToZip(out, dataBuffer, subscriptionFile, SEQUENCES_SUBSCRIBE_BACKUP_NAME);
                    }

                    // первым делом- получу из базы данных списки прочитанных и скачанных книг
                    AppDatabase db = App.getInstance().mDatabase;
                    List<DownloadedBooks> books = db.downloadedBooksDao().getAllBooks();
                    if (books != null && books.size() > 0) {
                        // создам XML
                        StringBuilder xmlBuilder = new StringBuilder();
                        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><downloaded_books>");
                        for (DownloadedBooks book : books) {
                            xmlBuilder.append("<book id=\"");
                            xmlBuilder.append(book.bookId);
                            xmlBuilder.append("\"/>");
                        }
                        xmlBuilder.append("</downloaded_books>");
                        String text = xmlBuilder.toString();
                        Log.d("surprise", "ReserveSettingsWorker doWork " + text);
                        File f1 = new File(sCompatSaveDir, "downloaded_books");
                        FileWriter writer = new FileWriter(f1);
                        writer.append(text);
                        writer.flush();
                        writer.close();
                        writeToZip(out, dataBuffer, f1, DOWNLOADED_BOOKS_BACKUP_NAME);
                        boolean result = f1.delete();
                        if (!result) {
                            Log.d("surprise", "ReserveSettingsWorker doWork не удалось удалить временный файл");
                        }
                    }
                    List<ReadedBooks> rBooks = db.readedBooksDao().getAllBooks();
                    if (rBooks != null && rBooks.size() > 0) {
                        // создам XML
                        StringBuilder xmlBuilder = new StringBuilder();
                        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><readed_books>");
                        for (ReadedBooks book : rBooks) {
                            xmlBuilder.append("<book id=\"");
                            xmlBuilder.append(book.bookId);
                            xmlBuilder.append("\"/>");
                        }
                        xmlBuilder.append("</readed_books>");
                        String text = xmlBuilder.toString();
                        File f1 = new File(sCompatSaveDir, "readed_books");
                        FileWriter writer = new FileWriter(f1);
                        writer.append(text);
                        writer.flush();
                        writer.close();
                        writeToZip(out, dataBuffer, f1, READED_BOOKS_BACKUP_NAME);
                        boolean result = f1.delete();
                        if (!result) {
                            Log.d("surprise", "ReserveSettingsWorker doWork не удалось удалить временный файл");
                        }
                    }
                    out.close();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Result.success();
    }


    private void writeToZip(ZipOutputStream stream, byte[] dataBuffer, File oldFileName, String
            newFileName) {
        if (oldFileName.exists()) {
            FileInputStream fis;
            try {
                fis = new FileInputStream(oldFileName);
                BufferedInputStream origin = new BufferedInputStream(fis, BUFFER);
                ZipEntry entry = new ZipEntry(newFileName);
                stream.putNextEntry(entry);
                int count;

                while ((count = origin.read(dataBuffer, 0, BUFFER)) != -1) {
                    stream.write(dataBuffer, 0, count);
                }
                origin.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
