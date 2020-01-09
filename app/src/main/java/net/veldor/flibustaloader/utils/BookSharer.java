package net.veldor.flibustaloader.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.support.v4.provider.DocumentFile;
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
                DocumentFile dfile = downloadsDir.findFile(name);
                if (dfile != null) {
                    final String docId;
                    docId = DocumentsContract.getDocumentId(dfile.getUri());
                    final String[] split = docId.split(":");
                    final String storage = split[0];
                    String path = "///storage/" + storage + "/" + split[1];
                    file = new File(path);
                    // отправлю запрос на открытие файла
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                    shareIntent.setType(MimeTypes.getFullMime(type));
                    Intent starter = Intent.createChooser(shareIntent, context.getString(R.string.share_with_message));
                    starter.addFlags(FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(starter);
                }
                else {
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
                context.startActivity(starter);
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
        context.startActivity(starter);
    }
}
