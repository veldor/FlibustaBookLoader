package net.veldor.flibustaloader.workers;

import android.app.Notification;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.ecxeptions.ConnectionLostException;
import net.veldor.flibustaloader.ecxeptions.TorNotLoadedException;
import net.veldor.flibustaloader.http.ExternalVpnVewClient;
import net.veldor.flibustaloader.http.TorWebClient;
import net.veldor.flibustaloader.notificatons.Notificator;
import net.veldor.flibustaloader.parsers.SubscriptionsParser;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.selections.SubscriptionItem;
import net.veldor.flibustaloader.ui.BaseActivity;
import net.veldor.flibustaloader.utils.SubscribesHandler;
import net.veldor.flibustaloader.view_models.SubscriptionsViewModel;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.util.ArrayList;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.util.EntityUtils;

import static net.veldor.flibustaloader.utils.MyFileReader.SUBSCRIPTIONS_FILE;

public class CheckSubscriptionsWorker extends Worker {

    public static final String CHECK_SUBSCRIBES = "check subscribes";
    public static final String FULL_CHECK = "full check";
    public static final String PERIODIC_CHECK_TAG = "periodic check subscriptions";
    private Notificator mNotifier;
    private String mNextPageLink;
    private ArrayList<SubscriptionItem> mSubscribes;
    private final ArrayList<FoundedBook> result = new ArrayList<>();
    private String mLastCheckedBookId;
    private String mCheckFor;
    private boolean mStopCheck;

    public CheckSubscriptionsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // список подписок
        mSubscribes = SubscribesHandler.getAllSubscribes();
        if (mSubscribes.size() > 0) {
            // проверю, полная ли проверка или частичная
            Data data = getInputData();
            boolean fullCheck = data.getBoolean(FULL_CHECK, false);
            if (!fullCheck) {
                // получу последнюю проверенную книгу
                mCheckFor = App.getInstance().getLastCheckedBookId();
            }

            // буду загружать страницы с новинками

            TorWebClient webClient;
            mNextPageLink = "/opds/new/0/new";
            mNotifier = Notificator.getInstance();
            // помечу рабочего важным
            ForegroundInfo info = createForegroundInfo();
            setForegroundAsync(info);

            String answer = null;
            do {
                if (!isStopped()) {
                    // пока буду загружать все страницы, которые есть
                    if (App.getInstance().isExternalVpn()) {
                        HttpResponse response = ExternalVpnVewClient.rawRequest((App.BASE_URL + mNextPageLink));
                        if (response != null) {
                            try {
                                answer = EntityUtils.toString(response.getEntity());
                            } catch (IOException e) {
                                Log.d("surprise", "CheckSubscriptionsWorker doWork error when receive subscription");
                                e.printStackTrace();
                            }
                        }
                    } else {
                        // создам новый экземпляр веб-клиента
                        try {
                            webClient = new TorWebClient();
                        } catch (ConnectionLostException e) {
                            e.printStackTrace();
                            return Result.success();
                        }
                        if (!isStopped()) {
                            answer = webClient.request(App.BASE_URL + mNextPageLink);
                        }
                    }
                    mNextPageLink = null;
                    if (answer != null) {
                        try {
                            handleAnswer(answer);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            while (mNextPageLink != null && !mStopCheck);
        }

        if (result.size() > 0) {
            // найдены книги
            Notificator.getInstance().sendFoundSubscribesNotification();
            serializeResult();
        } else {
            Notificator.getInstance().sendNotFoundSubscribesNotification();
        }
        if (mLastCheckedBookId != null) {
            // сохраню последнюю проверенную книгу
            App.getInstance().setLastCheckedBook(mLastCheckedBookId);
        }
        // оповещу об окончании процесса загрузки подписок
        SubscriptionsViewModel.sSubscriptionsChecked.postValue(true);
        return Result.success();
    }

    private void serializeResult() {
        try {
            Log.d("surprise", "CheckSubscriptionsWorker serializeResult: serialize " + result.size() + " books");
            File autocompleteFile = new File(App.getInstance().getFilesDir(), SUBSCRIPTIONS_FILE);
            FileOutputStream fos = new FileOutputStream(autocompleteFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(result);
            oos.close();
            fos.close();
            // оповещу об изменении списка
            BaseActivity.sLiveFoundedSubscriptionsCount.postValue(true);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void handleAnswer(String answer) throws Exception {

        // найду id последней книги на странице
        String lastId = answer.substring(answer.lastIndexOf("tag:book:"), answer.indexOf("<", answer.lastIndexOf("tag:book:")));
        if (mCheckFor != null && mCheckFor.compareTo(lastId) > 0) {
            mStopCheck = true;
        }

        mNextPageLink = null;
        boolean foundKeyWords = false;
        // проверю, есть ли в тексте упоминания слов, входящих в подписки. Если их нет- проверяю только ссылку на следующую страницу
        for (SubscriptionItem si :
                mSubscribes) {
            if (answer.toLowerCase().contains(si.name.toLowerCase())) {
                foundKeyWords = true;
                Log.d("surprise", "CheckSubscriptionsWorker handleAnswer 163: found subscription results");
                break;
            }
        }

        // если найдены слова, входящие в подписки- проверю страницу
        if (foundKeyWords) {
            SubscriptionsParser.handleSearchResults(result, answer, mSubscribes);
        }
        String value;
        String value1;
        boolean foundFirstBook = false;
        boolean nextPageLinkFound = false;
        // прогоню строку через парсер
        XmlPullParserFactory factory = XmlPullParserFactory
                .newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xrp = factory.newPullParser();
        xrp.setInput(new StringReader(answer));
        xrp.next();
        int eventType = xrp.getEventType();
        while (eventType != XmlResourceParser.END_DOCUMENT) {
            if (eventType == XmlResourceParser.START_TAG) {
                value = xrp.getName();
                if (value != null) {
                    if (!nextPageLinkFound && value.equals("link")) {
                        // проверю, возможно, это ссылка на следующую страницу
                        value1 = xrp.getAttributeValue(null, "rel");
                        if (value1 != null && value1.equals("next")) {
                            // найдена ссылка на следующую страницу
                            mNextPageLink = xrp.getAttributeValue(null, "href");
                            nextPageLinkFound = true;
                        }
                    }
                    if (mLastCheckedBookId == null && value.equals("entry")) {
                        // найдена книга, если это первая проверенная- запишу её id
                        foundFirstBook = true;
                    } else if (foundFirstBook && mLastCheckedBookId == null && value.equals("id")) {
                        mLastCheckedBookId = xrp.nextText();
                        return;
                    }
                }
            }
            eventType = xrp.next();
        }
    }

    @NonNull
    private ForegroundInfo createForegroundInfo() {
        Notification notification = mNotifier.getCheckSubscribesNotification();
        return new ForegroundInfo(Notificator.CHECK_SUBSCRIBES_WORKER_NOTIFICATION, notification);
    }
}
