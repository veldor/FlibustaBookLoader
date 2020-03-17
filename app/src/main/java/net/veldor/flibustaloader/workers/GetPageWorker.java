package net.veldor.flibustaloader.workers;

import android.content.Context;
import androidx.annotation.NonNull;

import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.MyWebClient;
import net.veldor.flibustaloader.ecxeptions.TorNotLoadedException;
import net.veldor.flibustaloader.http.ExternalVpnVewClient;
import net.veldor.flibustaloader.http.TorWebClient;

public class GetPageWorker extends Worker {

    public GetPageWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        App.getInstance().mLoadAllStatus.postValue("В процессе");
        Data data = getInputData();
        String text = data.getString(MyWebClient.LOADED_URL);

        // если используется внешний  VPN- просто создам сооединение
        if(App.getInstance().isExternalVpn()){
            String answer = ExternalVpnVewClient.request(text);
            if(!isStopped()){
                App.getInstance().mLoadAllStatus.postValue("Загрузка страницы завершена");
                App.getInstance().mSearchResult.postValue(answer);
            }
        }
        else{
            // создам новый экземпляр веб-клиента
            TorWebClient webClient;
            try {
                webClient = new TorWebClient();
            } catch (TorNotLoadedException e) {
                e.printStackTrace();
                return Result.failure();
            }
            if(!isStopped()){
                App.getInstance().mLoadAllStatus.postValue("Загрузка страницы начата");
                String answer = webClient.request(text);
                if(!isStopped()){
                    App.getInstance().mLoadAllStatus.postValue("Загрузка страницы завершена");
                    App.getInstance().mSearchResult.postValue(answer);
                }
            }
        }
        return Result.success();
    }
}
