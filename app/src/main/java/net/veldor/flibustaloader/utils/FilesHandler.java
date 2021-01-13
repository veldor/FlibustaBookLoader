package net.veldor.flibustaloader.utils;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpResponse;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static net.veldor.flibustaloader.utils.BookOpener.intentCanBeHandled;

public class FilesHandler {
    public static void shareFile(File zip) {
        //todo По возможности- разобраться и заменить на валидное решение
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        // отправлю запрос на открытие файла
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(zip));
        shareIntent.setType("application/zip");
        shareIntent.addFlags(FLAG_GRANT_READ_URI_PERMISSION | FLAG_ACTIVITY_NEW_TASK);
        if (intentCanBeHandled(shareIntent)) {
            App.getInstance().startActivity(shareIntent);
        } else {
            Toast.makeText(App.getInstance(), "Не найдено приложение, открывающее данный файл", Toast.LENGTH_SHORT).show();
        }
    }

    public static void shareFile (DocumentFile zip){
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, zip.getUri());
        shareIntent.setType("application/zip");
        shareIntent.addFlags(FLAG_GRANT_READ_URI_PERMISSION | FLAG_ACTIVITY_NEW_TASK);
        if (intentCanBeHandled(shareIntent)) {
            App.getInstance().startActivity(shareIntent);
        } else {
            Toast.makeText(App.getInstance(), "Не найдено приложение, открывающее данный файл", Toast.LENGTH_SHORT).show();
        }
    }

    public static String getChangeText() {
        try {
            InputStream textFileStream = App.getInstance().getAssets().open("changes.txt");
            BufferedReader r = new BufferedReader(new InputStreamReader(textFileStream));
            StringBuilder total = new StringBuilder();
            for (String line; (line = r.readLine()) != null; ) {
                total.append('\u25CF').append(' ').append(line).append("\n\n");
            }
            return total.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "no changes";
    }

    public static DocumentFile getDownloadFile(BooksDownloadSchedule book, HttpResponse response) {
        // придётся проверять, соответствует ли разрешение запрашиваемого файла разрешению полученного
        Header receivedContentType = response.getLastHeader("Content-Type");
        String trueFormat = receivedContentType.getValue();
        String trueFormatExtension = MimeTypes.getTrueFormatExtension(trueFormat);
        if(trueFormatExtension != null){
            // сравню полученный формат с настоящим. Если полученный отличается от настоящего- поменяю исходный
            String extension = Grammar.getExtension(book.name);
            if(!extension.equals(trueFormatExtension)){
                book.format = trueFormat;
                book.name = Grammar.changeExtension(book.name, trueFormatExtension);
            }
        }
            // получу имя файла
            DocumentFile downloadsDir = App.getInstance().getDownloadDir();
            if(MyPreferences.getInstance().isCreateSequencesDir() && book.reservedSequenceName != null){
                if(MyPreferences.getInstance().isCreateAdditionalDir()){
                    DocumentFile seriesDir = downloadsDir.findFile("Серии");
                    if(seriesDir == null || !seriesDir.exists()){
                        downloadsDir = downloadsDir.createDirectory("Серии");
                    }
                    else{
                        downloadsDir = seriesDir;
                    }
                }
                if (downloadsDir != null && downloadsDir.findFile(book.reservedSequenceName) == null) {
                    downloadsDir = downloadsDir.createDirectory(book.reservedSequenceName);
                    Log.d("surprise", "FilesHandler getDownloadFile create dir " + book.reservedSequenceName);
                } else if (downloadsDir != null){
                    downloadsDir = downloadsDir.findFile(book.reservedSequenceName);
                    Log.d("surprise", "FilesHandler getDownloadFile create dir " + book.reservedSequenceName);
                }
                if (downloadsDir == null) {
                    Log.d("surprise", "FilesHandler getDownloadFile can't create dir " + book.reservedSequenceName);
                    downloadsDir = App.getInstance().getDownloadDir();
                }
            }
            else{
                // проверю, нужно ли создавать папку под автора
                if (MyPreferences.getInstance().isCreateAuthorsDir()) {
                    if(MyPreferences.getInstance().isCreateAdditionalDir()){
                        DocumentFile authorsDir = downloadsDir.findFile("Авторы");
                        if(authorsDir == null || !authorsDir.exists()){
                            downloadsDir = downloadsDir.createDirectory("Авторы");
                        }
                        else{
                            downloadsDir = authorsDir;
                        }
                    }
                    // создам папку
                    if (downloadsDir != null && downloadsDir.findFile(book.authorDirName) == null) {
                        downloadsDir = downloadsDir.createDirectory(book.authorDirName);
                        Log.d("surprise", "FilesHandler getDownloadFile create dir " + book.authorDirName);
                    } else if(downloadsDir != null){
                        downloadsDir = downloadsDir.findFile(book.authorDirName);
                        Log.d("surprise", "FilesHandler getDownloadFile use dir " + book.authorDirName);
                    }
                    if (downloadsDir == null) {
                        Log.d("surprise", "FilesHandler getDownloadFile can't use dir " + book.authorDirName);
                        downloadsDir = App.getInstance().getDownloadDir();
                    }
                }
                if(MyPreferences.getInstance().isCreateSequencesDir() && book.sequenceDirName != null && !book.sequenceDirName.isEmpty()){
                    if (downloadsDir.findFile(book.sequenceDirName) == null) {
                        downloadsDir = downloadsDir.createDirectory(book.sequenceDirName);
                        Log.d("surprise", "FilesHandler getDownloadFile create dir " + book.sequenceDirName);
                    } else {
                        downloadsDir = downloadsDir.findFile(book.sequenceDirName);
                        Log.d("surprise", "FilesHandler getDownloadFile use dir " + book.sequenceDirName);
                    }
                    if (downloadsDir == null) {
                        downloadsDir = App.getInstance().getDownloadDir();
                        Log.d("surprise", "FilesHandler getDownloadFile can't use dir " + book.sequenceDirName);
                    }
                }
            }
        Log.d("surprise", "FilesHandler getDownloadFile load to dir " + downloadsDir.getUri());
            return downloadsDir.createFile(book.format, book.name);
    }

    public static File getCompatDownloadFile(BooksDownloadSchedule book) {
        File file = MyPreferences.getInstance().getDownloadDir();
        // проверю, нужно ли создавать папку под автора
        if(MyPreferences.getInstance().isCreateSequencesDir() && book.reservedSequenceName != null){
            file = new File(file, book.reservedSequenceName);
            Log.d("surprise", "FilesHandler getDownloadFile create dir " + book.reservedSequenceName);
        }
        else{
            if (MyPreferences.getInstance().isCreateAuthorsDir()) {
                file = new File(file, book.authorDirName);
                Log.d("surprise", "FilesHandler getDownloadFile use dir " + book.authorDirName);
            }
            if(MyPreferences.getInstance().isCreateSequencesDir() && book.sequenceDirName != null){
                Log.d("surprise", "FilesHandler getDownloadFile use dir " + book.sequenceDirName);
                file = new File(file, book.sequenceDirName);
            }
        }
        Log.d("surprise", "FilesHandler getDownloadFile load to dir " + file.getAbsolutePath());
        return new File(file, book.name);
    }

    public static File getBaseDownloadFile(BooksDownloadSchedule book) {
        File file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        return new File(file, book.name);
    }
}
