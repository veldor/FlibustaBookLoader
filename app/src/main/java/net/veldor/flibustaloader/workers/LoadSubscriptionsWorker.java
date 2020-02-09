package net.veldor.flibustaloader.workers;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.OPDSActivity;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.selections.FoundedItem;
import net.veldor.flibustaloader.selections.SubscriptionItem;
import net.veldor.flibustaloader.utils.TorWebClient;
import net.veldor.flibustaloader.utils.XMLParser;

import java.util.ArrayList;

public class LoadSubscriptionsWorker extends Worker {

    public static String sNextPage;

    public LoadSubscriptionsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        ArrayList<SubscriptionItem> subscribes = App.getInstance().getBooksSubscribe().getSubscribes();
        ArrayList<SubscriptionItem> authorSubscribes = App.getInstance().getAuthorsSubscribe().getSubscribes();
        if (subscribes.size() > 0 || authorSubscribes.size() > 0) {
            // получу последний проверенный ID
            String lastCheckedId = App.getInstance().getLastCheckedBookId();
            String firstCheckedId = null;
            Log.d("surprise", "LoadSubscriptionsWorker doWork last previous checked id is " + lastCheckedId);
            ArrayList<FoundedBook> booksResult = new ArrayList<>();
            // проверю, запустился ли TOR
            AndroidOnionProxyManager tor = App.getInstance().mLoadedTor.getValue();
            while (tor == null) {
                try {
                    Thread.sleep(1000);
                    Log.d("surprise", "CheckSubscriptionsWorker doWork wait tor");
                } catch (InterruptedException e) {
                    Log.d("surprise", "CheckSubscriptionsWorker doWork thread interrupted");
                    e.printStackTrace();
                }
                tor = App.getInstance().mLoadedTor.getValue();
            }
            // теперь подожду, пока TOR дозагрузится
            while (!tor.isBootstrapped()) {
                try {
                    Thread.sleep(1000);
                    Log.d("surprise", "CheckSubscriptionsWorker doWork wait tor boostrap");
                } catch (InterruptedException e) {
                    Log.d("surprise", "CheckSubscriptionsWorker doWork thread interrupted");
                    e.printStackTrace();
                }
            }
            // получу список подписок
            App.sSearchType = OPDSActivity.SEARCH_BOOKS;
            // получу список книг
            // создам новый экземпляр веб-клиента
            TorWebClient webClient = new TorWebClient();
            ArrayList<FoundedItem> result = new ArrayList<>();
            String answer = webClient.request(App.BASE_URL + "/opds/new/0/new");
            // сразу же обработаю результат
            if (answer != null && !answer.isEmpty()) {
                XMLParser.handleSearchResults(result, answer);
                // найду id последней добавленной книги
                if (result.size() > 0) {
                    FoundedItem book = result.get(0);
                    firstCheckedId = ((FoundedBook) book).id;
                    Log.d("surprise", "LoadSubscriptionsWorker doWork last id is " + firstCheckedId);
                }
                while (sNextPage != null) {
                    webClient = new TorWebClient();
                    answer = webClient.request(App.BASE_URL + sNextPage);
                    XMLParser.handleSearchResults(result, answer);
                }
            }
            // теперь каждую сущность нужно проверить на соответствие поисковому шаблону
            if (result.size() > 0) {
                FoundedBook realBook;
                Log.d("surprise", "CheckSubscriptionsWorker doWork найдены книги, проверяю");
                for (FoundedItem book : result) {
                    realBook = (FoundedBook) book;
                    // сравню название книги со всеми подписками
                    for (SubscriptionItem needle : subscribes) {
                        if (realBook.name.toLowerCase().contains(needle.name.toLowerCase())) {
                            // найдено совпадение
                            // добавлю книгу в список найденных
                            booksResult.add(realBook);
                        }
                    }
                    // сравню название книги со всеми подписками
                    for (SubscriptionItem needle : authorSubscribes) {
                        // если есть автор
                        if (realBook.author != null && !realBook.author.isEmpty()) {
                            if (realBook.author.toLowerCase().contains(needle.name.toLowerCase())) {
                                // найдено совпадение
                                // добавлю книгу в список найденных
                                booksResult.add(realBook);
                            }
                        }
                    }
                }
            } else {
                Log.d("surprise", "CheckSubscriptionsWorker doWork книги не найдены");
            }
            if (booksResult.size() > 0) {
                // отправлю результат
                App.getInstance().mSubscribeResults.postValue(booksResult);
                Log.d("surprise", "LoadSubscriptionsWorker doWork список книг отправлен");
            }
            if(firstCheckedId != null){
                App.getInstance().setLastCheckedBook(firstCheckedId);
            }
        } else {
            Log.d("surprise", "CheckSubscriptionsWorker doWork подписки не найдены!");
        }

        // сохраню первый отсканированный id как последний проверенный
        return Result.success();
    }
}
