package net.veldor.flibustaloader.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import androidx.documentfile.provider.DocumentFile;
import android.util.Log;
import android.widget.Toast;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;

import java.io.File;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class BookSharer {
    public static void shareBook(String name, String type) {
        Log.d("surprise", "BookSharer shareBook " + type);
        File file;
        // ========================================================================================
        Context context = App.getInstance();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            DocumentFile downloadsDir = App.getInstance().getNewDownloadDir();
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
                    if(!file.exists()){
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
                        Intent starter = Intent.createChooser(shareIntent, context.getString(R.string.share_with_message));
                        starter.addFlags(FLAG_ACTIVITY_NEW_TASK);
                        if (TransportUtils.intentCanBeHandled(starter)) {
                            context.startActivity(starter);
                        }
                        else{
                            Toast.makeText(App.getInstance(), "Упс, не нашлось приложения, которое могло бы это сделать.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.file_not_found_message), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.file_not_found_message), Toast.LENGTH_LONG).show();
                }
            }
            else {
                // получу путь к файлу
                file = new File(App.getInstance().getDownloadFolder(), name);
                if (file.exists()) {
                    // грязный хак- без него не работает доступ к Kindle, та не умеет в новый метод с контентом
                    //todo По возможности- разобраться и заменить на валидное решение
                    StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                    StrictMode.setVmPolicy(builder.build());
                    // отправлю запрос на открытие файла
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                    shareIntent.setType(MimeTypes.getFullMime(type));
                    Intent starter = Intent.createChooser(shareIntent, context.getString(R.string.share_with_message));
                    starter.addFlags(FLAG_ACTIVITY_NEW_TASK);
                    if (TransportUtils.intentCanBeHandled(starter)) {
                        context.startActivity(starter);
                    }
                    else{
                        Toast.makeText(context, "Упс, не нашлось приложения, которое могло бы это сделать.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.file_not_found_message), Toast.LENGTH_LONG).show();
                }
            }
        } else {
            // получу путь к файлу
            file = new File(App.getInstance().getDownloadFolder(), name);
            if (file.exists()) {
                // грязный хак- без него не работает доступ к Kindle, та не умеет в новый метод с контентом
                //todo По возможности- разобраться и заменить на валидное решение
                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());
                // отправлю запрос на открытие файла
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                shareIntent.setType(MimeTypes.getFullMime(type));
                Intent starter = Intent.createChooser(shareIntent, context.getString(R.string.share_with_message));
                starter.addFlags(FLAG_ACTIVITY_NEW_TASK);
                if (TransportUtils.intentCanBeHandled(starter)) {
                    context.startActivity(starter);
                }
                else{
                    Toast.makeText(context, "Упс, не нашлось приложения, которое могло бы это сделать.", Toast.LENGTH_LONG).show();
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
        }
        else{
            Toast.makeText(context, "Упс, не нашлось приложения, которое могло бы это сделать.", Toast.LENGTH_LONG).show();
        }
    }
}
