package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.MyWebClient;
import net.veldor.flibustaloader.utils.TorWebClient;

public class GetPageWorker extends Worker {
    private boolean mIsStopped;

    public GetPageWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Data data = getInputData();
        String text = data.getString(MyWebClient.LOADED_URL);
        // создам новый экземпляр веб-клиента
        TorWebClient webClient = new TorWebClient();
        String answer = webClient.request(text);
        if(!mIsStopped){
            App.getInstance().mSearchResult.postValue(answer);
        }
        return Result.success();
    }

    @Override
    public void onStopped() {
        super.onStopped();
        Log.d("surprise", "GetAllPagesWorker onStopped i stopped");
        mIsStopped = true;
        // остановлю процесс
    }
}
