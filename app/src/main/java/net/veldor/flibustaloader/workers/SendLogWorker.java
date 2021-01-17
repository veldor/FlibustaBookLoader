package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.utils.FilesHandler;
import net.veldor.flibustaloader.utils.ZipManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SendLogWorker extends Worker {

    public SendLogWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
// создам временный файл
        File outputDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/flibusta_log/");
        if (outputDir.mkdirs() || outputDir.isDirectory()) {
            File outputFile = new File(outputDir, "flibusta_log.zip");
            // получу список файлов из папки логов
            File logDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/flibusta_logcat/");
            if (logDir.mkdirs() || logDir.isDirectory()) {
                File[] existentFiles = logDir.listFiles();
                ZipManager zipManager = new ZipManager();
                zipManager.zip(existentFiles, outputFile);
                FilesHandler.shareFile(outputFile);
                if(existentFiles.length > 0){
                    for (File f :
                            existentFiles) {
                        //noinspection ResultOfMethodCallIgnored
                        f.delete();
                    }
                }
            }
        }
        return Result.success();
    }
}
