package net.veldor.flibustaloader.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.StrictMode;
import android.support.v4.app.ShareCompat;
import android.widget.Toast;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.MyWebViewClient;
import net.veldor.flibustaloader.R;

import java.io.File;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class BookSharer {
    public static void shareBook(String name, String type){
        // грязный хак- без него не работает доступ к Kindle, та не умеет в новый метод с контентом
        //todo По возможности- разобраться и заменить на валидное решение
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        // ========================================================================================
        Context context = App.getInstance();
        // получу путь к файлу
        File file = new File(App.getInstance().getDownloadFolder(), name);
        if(file.exists()){
            // отправлю запрос на открытие файла
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            shareIntent.setType(TypesKeeper.getInstance().getMime(type));
            Intent starter = Intent.createChooser(shareIntent, context.getString(R.string.share_with_message));
            starter.addFlags(FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(starter);
        }
        else{
            Toast.makeText(context, context.getString(R.string.file_not_found_message), Toast.LENGTH_LONG).show();
        }
    }
    public static void shareLink(String link){
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
