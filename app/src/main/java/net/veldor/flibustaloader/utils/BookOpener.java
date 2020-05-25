package net.veldor.flibustaloader.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;

import java.io.File;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;

public class BookOpener {
    public static void openBook(String name, String type) {
        Context context = App.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DocumentFile downloadsDir = App.getInstance().getDownloadDir();
            if (downloadsDir != null) {
                DocumentFile file = downloadsDir.findFile(name);
                if (file != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(file.getUri(), MimeTypes.getFullMime(type));
                    intent.addFlags(FLAG_GRANT_READ_URI_PERMISSION|FLAG_ACTIVITY_NEW_TASK);
                    if(intentCanBeHandled(intent)){
                        App.getInstance().startActivity(intent);
                    }
                    else{
                        Toast.makeText(App.getInstance(), "Не найдено приложение, открывающее данный файл",Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.file_not_found_message), Toast.LENGTH_LONG).show();
                }
            }
        }
        else{
            File dd = MyPreferences.getInstance().getDownloadDir();
            File file = new File(dd, name);
            if(file.isFile()){
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(file), MimeTypes.getFullMime(type));
                intent.addFlags(FLAG_GRANT_READ_URI_PERMISSION|FLAG_ACTIVITY_NEW_TASK);
                if(intentCanBeHandled(intent)){
                    App.getInstance().startActivity(intent);
                }
                else{
                    Toast.makeText(App.getInstance(), "Не найдено приложение, открывающее данный файл",Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, context.getString(R.string.file_not_found_message), Toast.LENGTH_LONG).show();
            }
        }
    }
    static boolean intentCanBeHandled(Intent intent){
        PackageManager packageManager = App.getInstance().getPackageManager();
        return intent.resolveActivity(packageManager) != null;
    }
}