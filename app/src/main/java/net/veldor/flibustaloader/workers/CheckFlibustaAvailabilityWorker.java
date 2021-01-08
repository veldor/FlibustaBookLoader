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
import net.veldor.flibustaloader.ecxeptions.TorNotLoadedException;
import net.veldor.flibustaloader.http.ExternalVpnVewClient;
import net.veldor.flibustaloader.http.GlobalWebClient;
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

public class CheckFlibustaAvailabilityWorker extends Worker {


    public static final String ACTION = "check availability";

    public CheckFlibustaAvailabilityWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // check availability
        try {
            String answer = GlobalWebClient.request(App.BASE_URL);
            if(answer.length() == 0){
                return Result.failure();
            }
            answer = GlobalWebClient.request(App.BASE_URL + "/opds/");
            if(answer.length() == 0){
                return Result.failure();
            }
            return Result.success();
        } catch (TorNotLoadedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Result.failure();
    }
}
