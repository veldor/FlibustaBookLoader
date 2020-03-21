package net.veldor.flibustaloader.workers;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.http.ExternalVpnVewClient;
import net.veldor.flibustaloader.ui.OPDSActivity;
import net.veldor.flibustaloader.ecxeptions.TorNotLoadedException;
import net.veldor.flibustaloader.notificatons.Notificator;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.selections.FoundedItem;
import net.veldor.flibustaloader.selections.SubscriptionItem;
import net.veldor.flibustaloader.http.TorWebClient;
import net.veldor.flibustaloader.utils.URLHandler;
import net.veldor.flibustaloader.utils.XMLParser;

import java.io.IOException;
import java.util.ArrayList;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.util.EntityUtils;

public class CheckSubscriptionsWorker extends Worker {

    public static String sNextPage;

    public CheckSubscriptionsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        ArrayList<SubscriptionItem> subscribes = App.getInstance().getBooksSubscribe().getSubscribes();
        ArrayList<SubscriptionItem> authorSubscribes = App.getInstance().getAuthorsSubscribe().getSubscribes();
        ArrayList<SubscriptionItem> sequenceSubscribes = App.getInstance().getSequencesSubscribe().getSubscribes();
        if(subscribes.size() > 0 || authorSubscribes.size() > 0 ||sequenceSubscribes.size() > 0){

            String lastCheckedId = App.getInstance().getLastCheckedBookId();
            Log.d("surprise", "CheckSubscriptionsWorker doWork last checked " + lastCheckedId);
            String answer = null;
            ArrayList<FoundedItem> result = new ArrayList<>();
            // получу список подписок
            App.sSearchType = OPDSActivity.SEARCH_BOOKS;

            if(App.getInstance().isExternalVpn()){
                HttpResponse response = ExternalVpnVewClient.rawRequest((App.BASE_URL + "/opds/new/0/new"));
                if(response != null){
                    try {
                        answer = EntityUtils.toString(response.getEntity());
                    } catch (IOException e) {
                        Log.d("surprise", "CheckSubscriptionsWorker doWork error when receive subscription");
                        e.printStackTrace();
                    }
                }
            }
            else{

                // проверю, запустился ли TOR
                AndroidOnionProxyManager tor = App.getInstance().mLoadedTor.getValue();
                while (tor == null){
                    try {
                        Thread.sleep(3000);
                        Log.d("surprise", "CheckSubscriptionsWorker doWork wait tor");
                    } catch (InterruptedException e) {
                        Log.d("surprise", "CheckSubscriptionsWorker doWork thread interrupted");
                        e.printStackTrace();
                    }
                    tor = App.getInstance().mLoadedTor.getValue();
                }
                // теперь подожду, пока TOR дозагрузится
                while (!tor.isBootstrapped()){
                    try {
                        Thread.sleep(3000);
                        Log.d("surprise", "CheckSubscriptionsWorker doWork wait tor boostrap");
                    } catch (InterruptedException e) {
                        Log.d("surprise", "CheckSubscriptionsWorker doWork thread interrupted");
                        e.printStackTrace();
                    }
                }
                // получу список книг
                // создам новый экземпляр веб-клиента
                TorWebClient webClient = null;
                try {
                    webClient = new TorWebClient();

                } catch (TorNotLoadedException e) {
                    e.printStackTrace();
                }
                if(webClient == null){
                    // верну ошибку
                    return Result.failure();
                }
                answer = webClient.request(App.BASE_URL + "/opds/new/0/new");
            }
            // сразу же обработаю результат
            if(answer != null && !answer.isEmpty()){
                XMLParser.handleSearchResults(result, answer);

                // проверю последнюю загруженную книгу
                String lastId = ((FoundedBook) result.get(result.size() - 1)).id;
                while (sNextPage != null && lastId.compareTo(lastCheckedId) > 0){
                    if(App.getInstance().isExternalVpn()){
                        HttpResponse response = ExternalVpnVewClient.rawRequest((URLHandler.getBaseUrl()) + sNextPage);
                        if(response != null){
                            try {
                                answer = EntityUtils.toString(response.getEntity());
                            } catch (IOException e) {
                                Log.d("surprise", "CheckSubscriptionsWorker doWork error when receive subscription");
                                e.printStackTrace();
                            }
                        }
                    }
                    else{
                        TorWebClient webClient;
                        try {
                            webClient = new TorWebClient();
                            answer = webClient.request(App.BASE_URL + sNextPage);
                        } catch (TorNotLoadedException e) {
                            e.printStackTrace();
                            Log.d("surprise", "CheckSubscriptionsWorker doWork: не удалось запустить TOR");
                        }
                    }
                    XMLParser.handleSearchResults(result, answer);
                    lastId = ((FoundedBook) result.get(result.size() - 1)).id;
                }
            }
            // теперь каждую сущность нужно проверить на соответствие поисковому шаблону
            if(result.size() > 0){
                FoundedBook realBook;
                for (FoundedItem book : result){
                    realBook = (FoundedBook) book;
                    // сравню название книги со всеми подписками
                    for (SubscriptionItem needle : subscribes){
                        if(realBook.name.toLowerCase().contains(needle.name.toLowerCase())){
                            // найдено совпадение
                            // отправлю нотификацию

                            new Notificator(App.getInstance()).sendFoundSubscribesNotification();
                            return Result.success();
                        }
                    }
                    // сравню автора книги со всеми подписками
                    for (SubscriptionItem needle : authorSubscribes){
                        if(realBook.author.toLowerCase().contains(needle.name.toLowerCase())){
                            // найдено совпадение
                            // отправлю нотификацию
                            new Notificator(App.getInstance()).sendFoundSubscribesNotification();
                            return Result.success();
                        }
                    }
                    // сравню серии книги со всеми подписками

                    for (SubscriptionItem needle : sequenceSubscribes){
                        if(realBook.sequenceComplex != null && realBook.sequenceComplex.toLowerCase().contains(needle.name.toLowerCase())){
                            // найдено совпадение
                            // отправлю нотификацию
                            new Notificator(App.getInstance()).sendFoundSubscribesNotification();
                            return Result.success();
                        }
                    }
                }
            }
            else{
                Log.d("surprise", "CheckSubscriptionsWorker doWork книги не найдены");
            }

        }
        else{
            Log.d("surprise", "CheckSubscriptionsWorker doWork подписки не найдены!");
        }
        return Result.success();
    }
}
