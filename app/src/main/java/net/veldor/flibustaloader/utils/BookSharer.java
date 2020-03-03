package net.veldor.flibustaloader.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import android.widget.Toast;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.BuildConfig;
import net.veldor.flibustaloader.R;

import java.io.File;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;

public class BookSharer {
    public static void shareBook(String name, String type) {
        Context context = App.getInstance();
        DocumentFile downloadsDir = App.getInstance().getNewDownloadDir();
        if (downloadsDir != null) {
            DocumentFile file = downloadsDir.findFile(name);
            if (file != null) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setDataAndType(file.getUri(), MimeTypes.getFullMime(type));
                intent.addFlags(FLAG_GRANT_READ_URI_PERMISSION|FLAG_ACTIVITY_NEW_TASK);
                App.getInstance().startActivity(intent);
            } else {
                Toast.makeText(context, context.getString(R.string.file_not_found_message), Toast.LENGTH_LONG).show();
            }
        } else {
            // получу путь к файлу
            File file = new File(App.getInstance().getDownloadFolder(), name);
            if (file.exists()) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                Uri data = FileProvider.getUriForFile(App.getInstance(), BuildConfig.APPLICATION_ID +".provider",file);
                intent.setDataAndType(data,MimeTypes.getFullMime(type));
                intent.addFlags(FLAG_GRANT_READ_URI_PERMISSION|FLAG_ACTIVITY_NEW_TASK);
                App.getInstance().startActivity(intent);
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
