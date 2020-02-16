package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.database.AppDatabase;
import net.veldor.flibustaloader.database.entity.DownloadedBooks;
import net.veldor.flibustaloader.database.entity.ReadedBooks;
import net.veldor.flibustaloader.notificatons.Notificator;
import net.veldor.flibustaloader.utils.MimeTypes;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class RestoreSettingsWorker extends Worker {


    public static final String URI = "uri";

    public RestoreSettingsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
        // получу дату
        Data data = getInputData();
        String uriString = data.getString(URI);
        if(uriString != null && !uriString.isEmpty()){
            Uri uri = Uri.parse(uriString);
            // далее- получу содержимое файла
            DocumentFile file = DocumentFile.fromTreeUri(App.getInstance(), uri);
            if(file != null && file.exists()){

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
                        context.startActivity(starter);
                    } else {
                        Toast.makeText(context, context.getString(R.string.file_not_found_message), Toast.LENGTH_LONG).show();
                    }

                // прочитаю данные из файла
                InputStream fin = App.getInstance().getContentResolver().openInputStream(file.getUri());
                ZipInputStream zin = new ZipInputStream(fin);
                ZipEntry ze;
                File targetFile;
                while ((ze = zin.getNextEntry()) != null) {
                    Log.d("surprise", "RestoreSettingsWorker doWork " + ze.getName());
                }
                zin.close();
            }
        }
    } catch (FileNotFoundException e) {
        e.printStackTrace();
    } catch (IOException e) {
            e.printStackTrace();
        }
        return Result.success();
    }
}
