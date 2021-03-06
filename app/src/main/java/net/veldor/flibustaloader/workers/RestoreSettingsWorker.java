package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.utils.MyFileReader;
import net.veldor.flibustaloader.utils.XMLHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static net.veldor.flibustaloader.utils.MyFileReader.AUTHORS_SUBSCRIBE_FILE;
import static net.veldor.flibustaloader.utils.MyFileReader.BOOKS_SUBSCRIBE_FILE;
import static net.veldor.flibustaloader.utils.MyFileReader.SEQUENCES_SUBSCRIBE_FILE;

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
            if (uriString != null && !uriString.isEmpty()) {
                Uri uri = Uri.parse(uriString);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    InputStream fileData = App.getInstance().getContentResolver().openInputStream(uri);
                    ZipInputStream zin = new ZipInputStream(fileData);
                    ZipEntry ze;
                    File targetFile;
                    while ((ze = zin.getNextEntry()) != null) {
                        Log.d("surprise", "doWork: found file " + ze.getName());
                        switch (ze.getName()) {
                            case ReserveSettingsWorker.PREF_BACKUP_NAME:
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                    targetFile = new File(App.getInstance().getDataDir() + "/shared_prefs/net.veldor.flibustaloader_preferences.xml");
                                } else {
                                    targetFile = new File(Environment.getDataDirectory() + "/shared_prefs/net.veldor.flibustaloader_preferences.xml");
                                }
                                extractFromZip(zin, targetFile);
                                break;
                            case ReserveSettingsWorker.AUTOFILL_BACKUP_NAME:
                                targetFile = new File(App.getInstance().getFilesDir(), MyFileReader.SEARCH_AUTOCOMPLETE_FILE);
                                extractFromZip(zin, targetFile);
                                break;
                            case ReserveSettingsWorker.BOOKS_SUBSCRIBE_BACKUP_NAME:
                                targetFile = new File(App.getInstance().getFilesDir(), BOOKS_SUBSCRIBE_FILE);
                                extractFromZip(zin, targetFile);
                                break;
                            case ReserveSettingsWorker.AUTHORS_SUBSCRIBE_BACKUP_NAME:
                                targetFile = new File(App.getInstance().getFilesDir(), AUTHORS_SUBSCRIBE_FILE);
                                extractFromZip(zin, targetFile);
                                break;
                            case ReserveSettingsWorker.SEQUENCES_SUBSCRIBE_BACKUP_NAME:
                                targetFile = new File(App.getInstance().getFilesDir(), SEQUENCES_SUBSCRIBE_FILE);
                                extractFromZip(zin, targetFile);
                                break;
                            case ReserveSettingsWorker.DOWNLOADED_BOOKS_BACKUP_NAME:
                            case ReserveSettingsWorker.READED_BOOKS_BACKUP_NAME:
                            case ReserveSettingsWorker.BOOKMARKS_BACKUP_NAME:
                            case ReserveSettingsWorker.DOWNLOAD_SCHEDULE_BACKUP_NAME:
                                // преобразую файл из XML в массив значений
                                XMLHandler.handleBackup(zin);
                                break;
                        }
                    }
                    zin.close();
                    return Result.success();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Result.failure();
    }

    private void extractFromZip(ZipInputStream zis, File fileName) {
        try {
            FileOutputStream fout = new FileOutputStream(fileName);
            for (int c = zis.read(); c != -1; c = zis.read()) {
                fout.write(c);
            }
            zis.closeEntry();
            fout.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
