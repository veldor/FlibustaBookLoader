package net.veldor.flibustaloader;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.work.WorkInfo;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import net.veldor.flibustaloader.view_models.MainViewModel;

import static androidx.work.WorkInfo.State.SUCCEEDED;
import static net.veldor.flibustaloader.MainActivity.START_TOR;

public class StartTorActivity extends AppCompatActivity {
    private static final String TOR_LAUNCHED_MESSAGE = "LAUNCHED";
    private LiveData<AndroidOnionProxyManager> mTorClient;
    private TextView mTorLoadingStatusText;
    private ProgressBar mTorLoadingProgressIndicator;
    private CountDownTimer mCdt;
    private int mProgressCounter;
    private AndroidOnionProxyManager mTor;
    private TorConnectErrorReceiver mTtorConnectErrorReceiver;
    private AlertDialog mTorRestartDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ещё одно отслеживание TOR
        LiveData<WorkInfo> workStatus = App.getInstance().mWork;
        workStatus.observe(this, new Observer<WorkInfo>() {
            @Override
            public void onChanged(@Nullable WorkInfo workInfo) {
                if (workInfo != null) {
                    if(workInfo.getState() == SUCCEEDED){
                        Log.d("surprise", "StartTorActivity onChanged work done");
                        torLoaded();
                    }
                }
            }
        });

        // зарегистрирую отслеживание загружающегося TOR
        LiveData<AndroidOnionProxyManager> loadedTor = App.getInstance().mLoadedTor;
        loadedTor.observe(this, new Observer<AndroidOnionProxyManager>() {
            @Override
            public void onChanged(@Nullable AndroidOnionProxyManager tor) {
                if (tor != null) {
                    startTrackingTor(tor);
                }
            }
        });
        // стартую загрузку TOR, жду, пока загрузится
        setContentView(R.layout.activity_start_tor);

        // найду строку статуса загрузки
        mTorLoadingStatusText = findViewById(R.id.progressTorLoadStatus);
        mTorLoadingStatusText.setText("Начинаю инициализацию");
        // найду индикатор прогресса
        mTorLoadingProgressIndicator = findViewById(R.id.torLoadProgressIndicator);

        mTorLoadingProgressIndicator.setProgress(0);

        MainViewModel myViewModel = ViewModelProviders.of(this).get(MainViewModel.class);

        if (mTorClient == null) {
            // если клиент не загружен- загружаю
            mTorClient = myViewModel.getTor();
        }
        // ещё одна проверка, может вернуться null
        // подожду, пока TOR загрузится
        mTorClient.observe(this, new Observer<AndroidOnionProxyManager>() {
            @Override
            public void onChanged(@Nullable AndroidOnionProxyManager androidOnionProxyManager) {
                if (androidOnionProxyManager != null) {
                    mTorLoadingStatusText.setText(R.string.tor_is_loaded);
                    mTorLoadingProgressIndicator.setProgress(100);
                }
            }
        });

        startTimer();

        // зарегистрирую получатель ошибки подключения к TOR
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyWebViewClient.TOR_CONNECT_ERROR_ACTION);
        mTtorConnectErrorReceiver = new TorConnectErrorReceiver();
        registerReceiver(mTtorConnectErrorReceiver, filter);
    }

    private void startTimer() {
        int oneMin = 3000000; // 1 minute in milli seconds
        mProgressCounter = 0;
        mCdt = new CountDownTimer(oneMin, 1000) {
            public void onTick(long millisUntilFinished) {
                mProgressCounter++;
                mTorLoadingProgressIndicator.setProgress(mProgressCounter / 3);
                if(mTor != null){
                    String last = mTor.getLastLog();
                    if (last != null) {
                        if (!last.isEmpty()) {
                            mTorLoadingStatusText.setText(last);
                            if (last.indexOf(TOR_LAUNCHED_MESSAGE) > 0) {
                                mTorLoadingStatusText.setText(R.string.tor_is_loaded);
                                mTorLoadingProgressIndicator.setProgress(100);
                                if (mCdt != null) {
                                    mCdt.cancel();
                                }
                            }
                        } else {
                            mTorLoadingStatusText.setText(R.string.tor_loading);
                        }
                        mTorLoadingStatusText.setText(last);
                    } else {
                        mTorLoadingStatusText.setText(R.string.tor_start_loading);

                    }
                }
                else{
                    mTorLoadingStatusText.setText(R.string.wait_tor_loading_message);
                }
            }
            public void onFinish() {
                // tor не загрузился, покажу сообщение с предложением подождать или перезапустить процесс
                torLoadTooLongDialog();
            }
        };
        mCdt.start();
    }

    private void torLoadTooLongDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Tor load too long")
                .setMessage("Подождём ещё или перезапустим?")
                .setPositiveButton("Перезапуск", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        restartCounter();
                        App.getInstance().restartTor();
                    }
                })
                .setNegativeButton("Подождать ещё", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        restartCounter();
                    }
                }).show();
    }

    private void restartCounter() {
        startTimer();
    }

    private void startTrackingTor(final AndroidOnionProxyManager tor) {
        mTor = tor;
    }

    private void torLoaded() {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

    // не разрешу вернуться в предыдущую активность при нажании кнопки назад
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public class TorConnectErrorReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // покажу диалоговое окно с оповещением, что TOR остановлен и кнопкой повторного запуска
            showTorRestartDialog();
        }
    }

    private void showTorRestartDialog() {
        if (mTorRestartDialog == null) {
            // создам диалоговое окно
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.tor_is_stopped)
                    .setMessage(R.string.tor_restart_dialog_message)
                    .setPositiveButton(R.string.restart_tor_message, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            App.getInstance().restartTor();
                            dialog.dismiss();
                            restartCounter();
                        }
                    })
                    .setCancelable(false);
            mTorRestartDialog = dialogBuilder.create();
        }
        mTorRestartDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mTtorConnectErrorReceiver != null) {
            unregisterReceiver(mTtorConnectErrorReceiver);
        }
        if(mTorRestartDialog != null){
            mTorRestartDialog.dismiss();
        }
    }
}