package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.MyWebClient;
import net.veldor.flibustaloader.selections.FoundedItem;
import net.veldor.flibustaloader.utils.TorWebClient;
import net.veldor.flibustaloader.utils.XMLParser;

import java.util.ArrayList;

public class GetAllPagesWorker extends Worker {

    public static String sNextPage;

    public GetAllPagesWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("surprise", "GetAllPagesWorker doWork start work");
        Data data = getInputData();
        String text = data.getString(MyWebClient.LOADED_URL);
        // создам новый экземпляр веб-клиента
        TorWebClient webClient = new TorWebClient();
        String answer = webClient.request(text);

        // сразу же обработаю результат
        if(answer != null && !answer.isEmpty()){
            ArrayList<FoundedItem> result = new ArrayList<>();
            XMLParser.handleSearchResults(result, answer);
            while (sNextPage != null){
                webClient = new TorWebClient();
                Log.d("surprise", "GetAllPagesWorker doWork next page is " + App.BASE_URL + sNextPage);
                answer = webClient.request(App.BASE_URL + sNextPage);
                XMLParser.handleSearchResults(result, answer);
            }
            Log.d("surprise", "GetAllPagesWorker doWork result length is " + result.size());
            App.getInstance().mParsedResult.postValue(result);
        }
        Log.d("surprise", "GetAllPagesWorker doWork work done");
        return Result.success();
    }
}
