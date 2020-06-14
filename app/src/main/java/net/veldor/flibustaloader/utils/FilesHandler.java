package net.veldor.flibustaloader.utils;

import android.content.Intent;
import android.net.Uri;
import android.os.StrictMode;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.database.entity.BooksDownloadSchedule;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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

    public static DocumentFile getDownloadFile(BooksDownloadSchedule book) {
            // получу имя файла
            DocumentFile downloadsDir = App.getInstance().getDownloadDir();
            // проверю, нужно ли создавать папку под автора
            if (MyPreferences.getInstance().isCreateAuthorsDir()) {
                // создам папку
                if (downloadsDir.findFile(book.authorDirName) == null) {
                    downloadsDir = downloadsDir.createDirectory(book.authorDirName);
                } else {
                    downloadsDir = downloadsDir.findFile(book.authorDirName);
                }
                if (downloadsDir == null) {
                    downloadsDir = App.getInstance().getDownloadDir();
                }
            }
            if(MyPreferences.getInstance().isCreateSequencesDir() && book.sequenceDirName != null && !book.sequenceDirName.isEmpty()){
                if (downloadsDir.findFile(book.sequenceDirName) == null) {
                    downloadsDir = downloadsDir.createDirectory(book.sequenceDirName);
                } else {
                    downloadsDir = downloadsDir.findFile(book.sequenceDirName);
                }
                if (downloadsDir == null) {
                    downloadsDir = App.getInstance().getDownloadDir();
                }
            }
            return downloadsDir.createFile(book.format, book.name);
    }

    public static File getCompatDownloadFile(BooksDownloadSchedule book) {
        File file = MyPreferences.getInstance().getDownloadDir();
        // проверю, нужно ли создавать папку под автора
        if (MyPreferences.getInstance().isCreateAuthorsDir()) {
            file = new File(file, book.authorDirName);
        }
        if(MyPreferences.getInstance().isCreateSequencesDir() && book.sequenceDirName != null){
            file = new File(file, book.sequenceDirName);
        }
        return new File(file, book.name);
    }
}
