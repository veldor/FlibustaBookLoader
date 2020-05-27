package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.http.GlobalWebClient;
import net.veldor.flibustaloader.parsers.SearchResponseParser;
import net.veldor.flibustaloader.selections.Author;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.selections.FoundedSequence;
import net.veldor.flibustaloader.selections.Genre;
import net.veldor.flibustaloader.ui.OPDSActivity;
import net.veldor.flibustaloader.utils.URLHelper;

import java.util.ArrayList;

public class SearchWorker extends Worker {
    public static final String REQUEST = "request";
    public static final String WORK_TAG = "search worker";

    public SearchWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            OPDSActivity.sLiveAuthorsFound.postValue(null);
            
            // сброшу указатель на следующую страницу
            OPDSActivity.sNextPage = null;
            // получу данные поиска
            Data data = getInputData();
            String request = data.getString(REQUEST);
            if (request != null) {
                App.getInstance().mLoadAllStatus.postValue("Загружаю страницу");
                Log.d("surprise", "SearchWorker doWork 40: load " + request);
                // сделаю первый запрос по-любому
                String answer = GlobalWebClient.request(request);
                if (answer != null && !isStopped()) {
                    ArrayList result;
                    // получу DOM
                    SearchResponseParser parser = new SearchResponseParser(answer);
                    int resultType = parser.getType();
                    result = parser.parseResponse();
                    if (!isStopped()) {
                        if(result == null || result.size() == 0){
                            // Ничего не найдено, уведомлю об этом
                            OPDSActivity.sNothingFound.postValue(true);
                        }
                        switch (resultType) {
                            case OPDSActivity.SEARCH_GENRE:
                                //noinspection unchecked
                                OPDSActivity.sLiveGenresFound.postValue((ArrayList<Genre>) result);
                                break;
                            case OPDSActivity.SEARCH_SEQUENCE:
                                //noinspection unchecked
                                OPDSActivity.sLiveSequencesFound.postValue((ArrayList<FoundedSequence>) result);
                                break;
                            case OPDSActivity.SEARCH_AUTHORS:
                                // сброшу предыдущие значения
                                Log.d("surprise", "SearchWorker doWork 65: post new authors");
                                //noinspection unchecked
                                OPDSActivity.sLiveAuthorsFound.postValue((ArrayList<Author>) result);
                                do {
                                    App.getInstance().mLoadAllStatus.postValue("Загружаю страницу");
                                    // запрошу следующую страницу, если она есть
                                    if (answer != null) {
                                        request = getNextPageLink(answer);
                                    } else {
                                        request = null;
                                    }
                                    if (request != null) {
                                        answer = GlobalWebClient.request(request);
                                        if (answer != null && !isStopped()) {
                                            parser = new SearchResponseParser(answer);
                                            result = parser.parseResponse();
                                            if (result.size() > 0 && !isStopped()) {
                                                //noinspection unchecked
                                                OPDSActivity.sLiveAuthorsFound.postValue((ArrayList<Author>) result);
                                            }
                                        }
                                    }
                                }
                                while (request != null && !isStopped());
                                break;
                            case OPDSActivity.SEARCH_BOOKS:
                                // сброшу предыдущие значения
                                OPDSActivity.sLiveBooksFound.postValue(null);
                                //noinspection unchecked
                                OPDSActivity.sLiveBooksFound.postValue((ArrayList<FoundedBook>) result);
                                // если выбрана загрузка сразу всего- гружу все результаты
                                if (App.getInstance().isDownloadAll()) {
                                    do {
                                        App.getInstance().mLoadAllStatus.postValue("Загружаю страницу");
                                        // запрошу следующую страницу, если она есть
                                        if (answer != null) {
                                            request = getNextPageLink(answer);
                                        } else {
                                            request = null;
                                        }
                                        if (request != null) {
                                            answer = GlobalWebClient.request(request);
                                            if (answer != null && !isStopped()) {
                                                parser = new SearchResponseParser(answer);
                                                result = parser.parseResponse();
                                                if (result.size() > 0 && !isStopped()) {
                                                    //noinspection unchecked
                                                    OPDSActivity.sLiveBooksFound.postValue((ArrayList<FoundedBook>) result);
                                                }
                                            }
                                        }
                                    }
                                    while (request != null && !isStopped());
                                } else {
                                    OPDSActivity.sNextPage = getNextPageLink(answer);
                                }
                                break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("surprise", "SearchWorker doWork 77: i failed( " + e.getMessage() + ")");
            return Result.failure();
        }
        return Result.success();
    }

    private String getNextPageLink(String answer) {
        String s = "rel=\"next\"";
        if (answer.contains(s)) {
            int finishValue = answer.indexOf(s) - 2;
            s = answer.substring(0, finishValue);
            return URLHelper.getBaseOPDSUrl() + s.substring(s.lastIndexOf("\"") + 1).replace("&amp;", "&");
        }
        return null;
    }
}
