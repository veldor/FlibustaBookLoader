package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.selections.FoundedItem;
import net.veldor.flibustaloader.selections.SubscriptionItem;
import net.veldor.flibustaloader.utils.TorWebClient;
import net.veldor.flibustaloader.utils.XMLParser;

import java.util.ArrayList;

public class CheckSubsctiptionsWorker extends Worker {

    public static String sNextPage;

    public CheckSubsctiptionsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // получу список подписок
        ArrayList<SubscriptionItem> books = App.getInstance().getBooksSubscribe().getSubscribes();
        // получу список книг
        // создам новый экземпляр веб-клиента
        TorWebClient webClient = new TorWebClient();
        String answer = webClient.request(App.BASE_URL + "/opds/new/0/new");
        // сразу же обработаю результат
        if(answer != null && !answer.isEmpty()){
            ArrayList<FoundedItem> result = new ArrayList<>();
            XMLParser.handleSearchResults(result, answer);
            while (sNextPage != null){
                Log.d("surprise", "CheckSubsctiptionsWorker doWork load next page");
                webClient = new TorWebClient();
                answer = webClient.request(App.BASE_URL + sNextPage);
                XMLParser.handleSearchResults(result, answer);
            }
            // теперь каждую сущность нужно проверить на соответствие поисковому шаблону
        }
        return Result.success();
    }
}
