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
import net.veldor.flibustaloader.http.ExternalVpnVewClient;
import net.veldor.flibustaloader.selections.FoundedItem;
import net.veldor.flibustaloader.utils.SortHandler;
import net.veldor.flibustaloader.http.TorWebClient;
import net.veldor.flibustaloader.utils.URLHandler;
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
        Log.d("surprise", "GetAllPagesWorker doWork: start multiply page load");
        int pagesCounter = 1;
        App.getInstance().mLoadAllStatus.postValue("В процессе");
        Log.d("surprise", "GetAllPagesWorker doWork start work");
        Data data = getInputData();
        App.getInstance().mLoadAllStatus.postValue("Загружаю страницу " + pagesCounter);
        String text = data.getString(MyWebClient.LOADED_URL);
        String answer;

        // получу страницу
        try {
            answer = getPage(text);
        } catch (TorNotLoadedException e) {
            return Result.failure();
        }

        // сразу же обработаю результат
        if(answer != null && !answer.isEmpty()){
            ArrayList<FoundedItem> result = new ArrayList<>();
            XMLParser.handleSearchResults(result, answer);
            while (sNextPage != null && !isStopped()){
                Log.d("surprise", "GetAllPagesWorker doWork: load next page");
                ++pagesCounter;
                App.getInstance().mLoadAllStatus.postValue("Загружаю страницу " + pagesCounter);
                try {
                    answer = getPage(URLHandler.getBaseUrl() + sNextPage);
                } catch (TorNotLoadedException e) {
                    return Result.failure();
                }
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
                if(!isStopped()){
                    App.getInstance().mParsedResult.postValue(result);
                }
            }
        }
        return Result.success();
    }

    private String getPage(String text) throws TorNotLoadedException {

        // если используется внешний  VPN- просто создам сооединение
        String answer;
        if(App.getInstance().isExternalVpn()){
            answer = ExternalVpnVewClient.request(text);
            if(!isStopped()){
                App.getInstance().mLoadAllStatus.postValue("Загрузка страницы завершена");
                return answer;
            }
        }
        else{
                // создам новый экземпляр веб-клиента
                TorWebClient webClient = new TorWebClient();
                answer = webClient.request(text);
            if(!isStopped()){
                App.getInstance().mLoadAllStatus.postValue("Загрузка страницы завершена");
                return answer;
            }
        }
        return null;
    }
}
