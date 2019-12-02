package net.veldor.flibustaloader.view_models;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.MyWebView;
import net.veldor.flibustaloader.selections.FoundedBook;
import net.veldor.flibustaloader.updater.Updater;
import net.veldor.flibustaloader.utils.BookSharer;
import net.veldor.flibustaloader.utils.MyFileReader;
import net.veldor.flibustaloader.utils.XMLHandler;

import java.util.ArrayList;
import java.util.Random;

public class MainViewModel extends AndroidViewModel {

    public MainViewModel(@NonNull Application application) {
        super(application);
    }

    public int getViewMode(){
        return App.getInstance().getViewMode();
    }
    public void switchViewMode(int type){
        App.getInstance().switchViewMode(type);
    }

    // загрузка ядра TOR
    public LiveData<AndroidOnionProxyManager> getTor(){
        return App.getInstance().mTorManager;
    }

    public LiveData<Boolean> startCheckUpdate() {
        return Updater.checkUpdate();
    }

    public void initializeUpdate() {
        Updater.update();
    }

    public void switchNightMode() {
        App.getInstance().switchNightMode();
    }

    public boolean getNightModeEnabled() {
        return App.getInstance().getNightMode();
    }

    public ArrayList<String> getSearchAutocomplete() {
        String content = MyFileReader.getSearchAutocomplete();
        return XMLHandler.getSearchAutocomplete(content);
    }

    public String getRandomBookUrl() {
        Random random = new Random();
        return App.BASE_BOOK_URL + random.nextInt(App.MAX_BOOK_NUMBER);
    }

    public void shareLink(MyWebView mWebView) {
        BookSharer.shareLink(mWebView.getUrl());
    }

    public void switchODPSMode() {
        App.getInstance().switchODPSMode();
    }
}
