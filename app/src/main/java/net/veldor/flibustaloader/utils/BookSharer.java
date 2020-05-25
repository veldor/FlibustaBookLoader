package net.veldor.flibustaloader.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;

import java.io.File;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
import static net.veldor.flibustaloader.utils.BookOpener.intentCanBeHandled;

public class BookSharer {

    public static void shareBook(String name, String type) {
        Log.d("surprise", "BookSharer shareBook " + type);
        File file;
        // ========================================================================================
        Context context = App.getInstance();
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DocumentFile downloadsDir = App.getInstance().getDownloadDir();
            if (downloadsDir != null) {
                DocumentFile downloadFile = downloadsDir.findFile(name);
                if (downloadFile != null) {
                    final String docId;
                    docId = DocumentsContract.getDocumentId(downloadFile.getUri());
                    Log.d("surprise", "BookSharer shareBook " + docId);
                    final String[] split = docId.split(":");
                    final String storage = split[0];
                    String path = "///storage/" + storage + "/" + split[1];
                    file = new File(path);
                    Log.d("surprise", "BookSharer shareBook " + file);
                    // костыли, проверю существование файла с условием, что он находится на основной флешке
                    if (!file.exists()) {
                        file = new File(Environment.getExternalStorageDirectory() + "/" + split[1]);
                    }
                    if (file.exists()) {
                        //todo По возможности- разобраться и заменить на валидное решение
                        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                        StrictMode.setVmPolicy(builder.build());
                        // отправлю запрос на открытие файла
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                        shareIntent.setType(MimeTypes.getFullMime(type));
                        shareIntent.addFlags(FLAG_GRANT_READ_URI_PERMISSION | FLAG_ACTIVITY_NEW_TASK);
                        if (intentCanBeHandled(shareIntent)) {
                            App.getInstance().startActivity(shareIntent);
                        } else {
                            Toast.makeText(App.getInstance(), "Не найдено приложение, открывающее данный файл", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.file_not_found_message), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.file_not_found_message), Toast.LENGTH_LONG).show();
                }
            }
        } else {
            File dd = MyPreferences.getInstance().getDownloadDir();
            File bookFile = new File(dd, name);
            if (bookFile.isFile()) {
                //todo По возможности- разобраться и заменить на валидное решение
                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());
                // отправлю запрос на открытие файла
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(bookFile));
                shareIntent.setType(MimeTypes.getFullMime(type));
                shareIntent.addFlags(FLAG_GRANT_READ_URI_PERMISSION | FLAG_ACTIVITY_NEW_TASK);
                if (intentCanBeHandled(shareIntent)) {
                    App.getInstance().startActivity(shareIntent);
                } else {
                    Toast.makeText(App.getInstance(), "Не найдено приложение, открывающее данный файл", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, context.getString(R.string.file_not_found_message), Toast.LENGTH_LONG).show();
            }
        }
    }

    public static void shareLink(String link) {
        Context context = App.getInstance();
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, R.string.share_url_message);
        i.putExtra(Intent.EXTRA_TEXT, link);
        Intent starter = Intent.createChooser(i, context.getString(R.string.share_url_title));
        starter.addFlags(FLAG_ACTIVITY_NEW_TASK);
        if (TransportUtils.intentCanBeHandled(starter)) {
            context.startActivity(starter);
        } else {
            Toast.makeText(context, "Упс, не нашлось приложения, которое могло бы это сделать.", Toast.LENGTH_LONG).show();
        }
    }
}
