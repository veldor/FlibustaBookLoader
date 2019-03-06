package net.veldor.flibustaloader.view_models;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.updater.Updater;

public class MainViewModel extends AndroidViewModel {

    public MainViewModel(@NonNull Application application) {
        super(application);
    }

    public boolean getLightModeEnabled(){
        return App.getInstance().getViewMode();
    }
    public void switchLightMode(){
        App.getInstance().swtichViewMode();
    }

    // загрузка ядра TOR
    public LiveData<AndroidOnionProxyManager> getTor(){
        return App.getInstance().mTorManager;
    }

    public void startCheckUpdate() {
        Updater.checkUpdate();
    }
}
