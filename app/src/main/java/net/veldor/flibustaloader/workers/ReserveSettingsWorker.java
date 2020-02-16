package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.database.AppDatabase;
import net.veldor.flibustaloader.database.entity.DownloadedBooks;
import net.veldor.flibustaloader.database.entity.ReadedBooks;
import net.veldor.flibustaloader.notificatons.Notificator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ReserveSettingsWorker extends Worker {


    private static final int BUFFER = 1024;
    private static final String PREF_BACKUP_NAME = "data1";
    private static final String DOWNLOADED_BOOKS_BACKUP_NAME = "data2";
    private static final String READED_BOOKS_BACKUP_NAME = "data3";

    public ReserveSettingsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            File backupDir = new File(Environment.getExternalStorageDirectory(), App.BACKUP_DIR_NAME);
            if (!backupDir.exists()) {
                boolean result = backupDir.mkdirs();
                if (result) {
                    Log.d("surprise", "ReserveWorker doWork: dir created");
                }
            }
            File zip = new File(backupDir, App.BACKUP_FILE_NAME);
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
                if(!result){
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
                if(!result){
                    Log.d("surprise", "ReserveSettingsWorker doWork не удалось удалить временный файл");
                }
            }
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Notificator(App.getInstance()).sendBackupSuccessNotification();
        return Result.success();
    }


    private void writeToZip(ZipOutputStream stream, byte[] dataBuffer, File oldFileName, String newFileName) {
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
