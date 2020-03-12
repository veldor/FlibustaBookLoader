package net.veldor.flibustaloader.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.utils.XMLParser;

import java.io.InputStream;

public class ParseWebRequestWorker extends Worker {
    public ParseWebRequestWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        InputStream data = App.getInstance().mRequestData;
        if(data != null){
            XMLParser.searchDownloadLinks(data);
        }
        return Result.success();
    }
}
