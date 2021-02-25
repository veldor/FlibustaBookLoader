package net.veldor.flibustaloader.workers;

import android.content.Context;
import android.content.Intent;
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
import net.veldor.flibustaloader.utils.Grammar;
import net.veldor.flibustaloader.utils.URLHelper;

import java.util.ArrayList;

import static net.veldor.flibustaloader.MyWebViewClient.TOR_CONNECT_ERROR_ACTION;
import static net.veldor.flibustaloader.http.TorWebClient.ERROR_DETAILS;

public class SearchWorker extends Worker {
    public static final String REQUEST = "request";
    public static final String WORK_TAG = "search worker";
    public static String sSequenceName;

    public SearchWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String reservedSequenceName = null;
        // если есть имя серии- сохраню книги в папку с данным имененм
        if (sSequenceName != null) {
            reservedSequenceName = Grammar.clearDirName(sSequenceName);
            sSequenceName = null;
            if (reservedSequenceName.startsWith("Все книги серии ")) {
                reservedSequenceName = reservedSequenceName.substring(16);
            }
        }
        try {
            // оповещу о начале нового поиска
            OPDSActivity.sNewSearch.postValue(true);
            // сброшу указатель на следующую страницу
            OPDSActivity.sNextPage = null;
            int pageCounter = 1;
            // получу данные поиска
            Data data = getInputData();
            String request = data.getString(REQUEST);
            if (request != null) {
                // сделаю первый запрос по-любому
                String answer = GlobalWebClient.request(request);
                if (answer == null || answer.length() == 0) {
                    OPDSActivity.isLoadError.postValue(true);
                }
                if (!isStopped()) {
                    ArrayList result;
                    // получу DOM
                    SearchResponseParser parser = new SearchResponseParser(answer);
                    int resultType = parser.getType();
                    result = parser.parseResponse(reservedSequenceName);
                    if (!isStopped()) {
                        if (result == null || result.size() == 0) {
                            // Ничего не найдено, уведомлю об этом
                            OPDSActivity.sNothingFound.postValue(true);
                        }
                        switch (resultType) {
                            case OPDSActivity.SEARCH_GENRE:
                                //noinspection unchecked
                                OPDSActivity.sLiveGenresFound.postValue((ArrayList<Genre>) result);
                                OPDSActivity.sSearchType = OPDSActivity.SEARCH_TYPE_GENRE;
                                do {
                                    ++pageCounter;
                                    // запрошу следующую страницу, если она есть
                                    if (answer != null) {
                                        App.getInstance().mLoadAllStatus.postValue("Загружаю страницу " + pageCounter);
                                        request = getNextPageLink(answer);
                                    } else {
                                        request = null;
                                    }
                                    if (request != null) {
                                        answer = GlobalWebClient.request(request);
                                        if (answer != null && !isStopped()) {
                                            parser = new SearchResponseParser(answer);
                                            result = parser.parseResponse(reservedSequenceName);
                                            if (result != null && result.size() > 0 && !isStopped()) {
                                                //noinspection unchecked
                                                OPDSActivity.sLiveGenresFound.postValue((ArrayList<Genre>) result);
                                            }
                                        }
                                    }
                                }
                                while (request != null && !isStopped());
                                break;
                            case OPDSActivity.SEARCH_SEQUENCE:
                                OPDSActivity.sSearchType = OPDSActivity.SEARCH_TYPE_SEQUENCES;
                                //noinspection unchecked
                                OPDSActivity.sLiveSequencesFound.postValue((ArrayList<FoundedSequence>) result);
                                do {
                                    ++pageCounter;
                                    // запрошу следующую страницу, если она есть
                                    if (answer != null) {
                                        App.getInstance().mLoadAllStatus.postValue("Загружаю страницу " + pageCounter);
                                        request = getNextPageLink(answer);
                                    } else {
                                        request = null;
                                    }
                                    if (request != null) {
                                        answer = GlobalWebClient.request(request);
                                        if (answer != null && !isStopped()) {
                                            parser = new SearchResponseParser(answer);
                                            result = parser.parseResponse(reservedSequenceName);
                                            if (result != null && result.size() > 0 && !isStopped()) {
                                                //noinspection unchecked
                                                OPDSActivity.sLiveSequencesFound.postValue((ArrayList<FoundedSequence>) result);
                                            }
                                        }
                                    }
                                }
                                while (request != null && !isStopped());
                                break;
                            case OPDSActivity.SEARCH_AUTHORS:
                            case OPDSActivity.SEARCH_NEW_AUTHORS:
                                OPDSActivity.sSearchType = OPDSActivity.SEARCH_TYPE_AUTHORS;
                                Log.d("surprise", "SearchWorker doWork 110: load authors");
                                // сброшу предыдущие значения
                                Log.d("surprise", "SearchWorker doWork 65: post new authors");
                                //noinspection unchecked
                                OPDSActivity.sLiveAuthorsFound.postValue((ArrayList<Author>) result);
                                do {
                                    ++pageCounter;
                                    Log.d("surprise", "SearchWorker doWork 110: load authors");
                                    // запрошу следующую страницу, если она есть
                                    if (answer != null) {
                                        App.getInstance().mLoadAllStatus.postValue("Загружаю страницу " + pageCounter);
                                        request = getNextPageLink(answer);
                                    } else {
                                        request = null;
                                    }
                                    if (request != null) {
                                        answer = GlobalWebClient.request(request);
                                        if (answer != null && !isStopped()) {
                                            parser = new SearchResponseParser(answer);
                                            result = parser.parseResponse(reservedSequenceName);
                                            if (result != null && result.size() > 0 && !isStopped()) {
                                                //noinspection unchecked
                                                OPDSActivity.sLiveAuthorsFound.postValue((ArrayList<Author>) result);
                                            }
                                        }
                                    }
                                }
                                while (request != null && !isStopped());
                                break;
                            case OPDSActivity.SEARCH_BOOKS:
                                OPDSActivity.sSearchType = OPDSActivity.SEARCH_TYPE_BOOKS;
                                // сброшу предыдущие значения
                                OPDSActivity.sLiveBooksFound.postValue(null);
                                //noinspection unchecked
                                OPDSActivity.sLiveBooksFound.postValue((ArrayList<FoundedBook>) result);
                                // если выбрана загрузка сразу всего- гружу все результаты
                                if (App.getInstance().isDownloadAll()) {
                                    do {
                                        ++pageCounter;
                                        // запрошу следующую страницу, если она есть
                                        if (answer != null) {
                                            request = getNextPageLink(answer);
                                            App.getInstance().mLoadAllStatus.postValue("Загружаю страницу " + pageCounter);
                                        } else {
                                            request = null;
                                        }
                                        if (request != null) {
                                            answer = GlobalWebClient.request(request);
                                            if (answer != null && !isStopped()) {
                                                parser = new SearchResponseParser(answer);
                                                result = parser.parseResponse(reservedSequenceName);
                                                if (result != null && result.size() > 0 && !isStopped()) {
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
            Intent finishLoadingIntent = new Intent(TOR_CONNECT_ERROR_ACTION);
            finishLoadingIntent.putExtra(ERROR_DETAILS, " Не удалось обработать полученные данные. Проблемы с соединением или сервером Флибусты");
            App.getInstance().sendBroadcast(finishLoadingIntent);
            return Result.failure();
        }
        OPDSActivity.sNewSearch.postValue(false);
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
