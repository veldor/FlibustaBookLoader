package net.veldor.flibustaloader.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.StrictMode;
import android.widget.Toast;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.MyWebViewClient;
import net.veldor.flibustaloader.R;

import java.io.File;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class BookOpener {
    public static void openBook(String name, String type) {
        // грязный хак- без него не работает доступ к Kindle, та не умеет в новый метод с контентом
        //todo По возможности- разобраться и заменить на валидное решение
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        // ========================================================================================
        Context context = App.getInstance();
        // получу путь к файлу
        File file = new File(MyWebViewClient.DOWNLOAD_FOLDER_LOCATION, name);
        if(file.exists()){
            // отправлю запрос на открытие файла
            Intent openIntent = new Intent(Intent.ACTION_VIEW);
            openIntent.setDataAndType(Uri.fromFile(file), TypesKeeper.getInstance().getMime(type));
            Intent starter = Intent.createChooser(openIntent, context.getString(R.string.open_with_message));
            starter.addFlags(FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(starter);
        }
        else{
            Toast.makeText(context, context.getString(R.string.file_not_found_message), Toast.LENGTH_LONG).show();
        }
    }
}