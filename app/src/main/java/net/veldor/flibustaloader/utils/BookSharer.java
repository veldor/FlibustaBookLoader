package net.veldor.flibustaloader.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.util.Log;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.MyWebViewClient;

import java.io.File;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class BookSharer {
    public static void shareBook(String name, String type){

        Log.d("surprise", "receive data name= " + name + " type= " + type);

        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(dir, name);
        Context context = App.getInstance();
        Intent intent = new Intent(Intent.ACTION_SEND);
        String fileType = "text/plain";
        switch (type){
            case MyWebViewClient.MOBI_TYPE:
                fileType = "application/x-mobipocket-ebook";
                break;
            case MyWebViewClient.EPUB_TYPE:
                fileType = "application/epub";
                break;
            case MyWebViewClient.FB2_TYPE:
                fileType = "application/fb2";
                break;
            case MyWebViewClient.PDF_TYPE:
                fileType = "application/pdf";
                break;
            case MyWebViewClient.DJVU_TYPE:
                fileType = "application/djvu";
                break;
        }
        Uri uri = FileProvider.getUriForFile(context, "net.veldor.flibustaloader.fileProvider", file);
        intent.setDataAndType(uri, fileType);
        Intent chooserIntent = Intent.createChooser(intent, "Open file");
        chooserIntent.addFlags(FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(chooserIntent);
    }
}
