package net.veldor.flibustaloader.workers;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.MyWebClient;
import net.veldor.flibustaloader.ecxeptions.TorNotLoadedException;
import net.veldor.flibustaloader.selections.FoundedItem;
import net.veldor.flibustaloader.utils.SortHandler;
import net.veldor.flibustaloader.http.TorWebClient;
import net.veldor.flibustaloader.utils.XMLParser;

import java.util.ArrayList;

import static net.veldor.flibustaloader.ui.OPDSActivity.SEARCH_AUTHORS;
import static net.veldor.flibustaloader.ui.OPDSActivity.SEARCH_BOOKS;
import static net.veldor.flibustaloader.ui.OPDSActivity.SEARCH_GENRE;
import static net.veldor.flibustaloader.ui.OPDSActivity.SEARCH_NEW_AUTHORS;
import static net.veldor.flibustaloader.ui.OPDSActivity.SEARCH_SEQUENCE;

public class GetAllPagesWorker extends Worker {

    public static String sNextPage;

    public GetAllPagesWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        int pagesCounter = 1;
        App.getInstance().mLoadAllStatus.postValue("В процессе");
        Log.d("surprise", "GetAllPagesWorker doWork start work");
        Data data = getInputData();
        App.getInstance().mLoadAllStatus.postValue("Загружаю страницу " + pagesCounter);
        String text = data.getString(MyWebClient.LOADED_URL);
        // создам новый экземпляр веб-клиента
        TorWebClient webClient;
        try {
            webClient = new TorWebClient();
        } catch (TorNotLoadedException e) {
            e.printStackTrace();
            return Result.failure();
        }
        App.getInstance().mLoadAllStatus.postValue("Загрузка страницы начата");
        String answer = webClient.request(text);
        App.getInstance().mLoadAllStatus.postValue("Загрузка страницы завершена");
        // сразу же обработаю результат
        if(answer != null && !answer.isEmpty()){
            ArrayList<FoundedItem> result = new ArrayList<>();
            XMLParser.handleSearchResults(result, answer);
            while (sNextPage != null && !isStopped()){
                ++pagesCounter;
                App.getInstance().mLoadAllStatus.postValue("Загружаю страницу " + pagesCounter);
                try {
                    webClient = new TorWebClient();
                } catch (TorNotLoadedException e) {
                    e.printStackTrace();
                }
                Log.d("surprise", "GetAllPagesWorker doWork next page is " + App.BASE_URL + sNextPage);
                answer = webClient.request(App.BASE_URL + sNextPage);
                XMLParser.handleSearchResults(result, answer);
            }
            Log.d("surprise", "GetAllPagesWorker doWork result length is " + result.size());
            if(!isStopped()){
                // отсортирую результат
                switch (App.sSearchType) {
                    case SEARCH_BOOKS:
                        // пересортирую то, что уже есть
                        SortHandler.sortBooks(result);
                        break;
                    case SEARCH_AUTHORS:
                    case SEARCH_NEW_AUTHORS:
                        SortHandler.sortAuthors(result);
                        break;
                    case SEARCH_GENRE:
                        SortHandler.sortGenres(result);
                        break;
                    case SEARCH_SEQUENCE:
                        SortHandler.sortSequences(result);
                        break;
                }
                App.getInstance().mParsedResult.postValue(result);
            }
        }
        Log.d("surprise", "GetAllPagesWorker doWork work done");
        return Result.success();
    }
}
