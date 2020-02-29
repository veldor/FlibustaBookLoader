package net.veldor.flibustaloader;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.work.WorkInfo;

import com.msopentech.thali.android.toronionproxy.AndroidOnionProxyManager;

import java.util.Locale;

import static androidx.work.WorkInfo.State.ENQUEUED;
import static androidx.work.WorkInfo.State.FAILED;
import static androidx.work.WorkInfo.State.RUNNING;
import static androidx.work.WorkInfo.State.SUCCEEDED;

public class StartTorActivity extends AppCompatActivity {

    private static boolean active = false;
    private TextView mTorLoadingStatusText;
    private ProgressBar mTorLoadingProgressIndicator;
    private CountDownTimer mCdt;
    private int mProgressCounter;
    private AndroidOnionProxyManager mTor;
    private TorConnectErrorReceiver mTorConnectErrorReceiver;
    private AlertDialog mTorRestartDialog;
    private long mConfirmExit;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_start_tor);


        // переназову окно
        ActionBar actionbar = getActionBar();
        if(actionbar != null){
            actionbar.setTitle("Подготовка");
        }

        // настройки интерфейса ====================================================================
        TextView versionView = findViewById(R.id.app_version);
        String version;
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
            versionView.setText(String.format(Locale.ENGLISH, getString(R.string.application_version_message), version));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        Button startBtn = findViewById(R.id.testStartApp);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // покажу диалог, предупреждающий о том, что это не запустит приложение
                showForceStartDialog();
            }
        });


        // найду строку статуса загрузки
        mTorLoadingStatusText = findViewById(R.id.progressTorLoadStatus);
        mTorLoadingStatusText.setText(StartTorActivity.this.getString(R.string.begin_tor_init_msg));

        // =========================================================================================

        observeTorStart();

        // работа с индикатором прогресса ==========================================================
        // найду индикатор прогресса
        mTorLoadingProgressIndicator = findViewById(R.id.torLoadProgressIndicator);
        mTorLoadingProgressIndicator.setProgress(0);

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

        // зарегистрирую получатель ошибки подключения к TOR
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyWebViewClient.TOR_CONNECT_ERROR_ACTION);
        mTorConnectErrorReceiver = new TorConnectErrorReceiver();
        registerReceiver(mTorConnectErrorReceiver, filter);
    }

    private void observeTorStart() {
        stopCounter();
        LiveData<WorkInfo> startTorWorkStatus = App.getInstance().TorStartWork;

        startTorWorkStatus.observe(this, new Observer<WorkInfo>() {
            @Override
            public void onChanged(@Nullable WorkInfo workInfo) {
                // стартую загрузку TOR, жду, пока загрузится
                if (workInfo != null) {
                    Log.d("surprise", "StartTorActivity onChanged tor load status is " + workInfo.getState());
                    if (workInfo.getState() == SUCCEEDED) {
                        torLoaded();
                    } else if (workInfo.getState() == FAILED) {
                        // покажу диалоговое окно с предупрежением, что TOR не запускается на этой версии Android
                        showTorNotWorkDialog();
                    } else if (workInfo.getState() == RUNNING) {
                        startTimer();
                        if (mTorLoadingStatusText != null) {
                            mTorLoadingStatusText.setText(getString(R.string.launch_begin_message));
                        }
                    } else if (workInfo.getState() == ENQUEUED) {
                        if (mTorLoadingStatusText != null) {
                            mTorLoadingStatusText.setText(getString(R.string.tor_load_waiting_internet_message));
                        }
                    }
                }
            }
        });
    }

    private void stopCounter() {
        mProgressCounter = 0;
        if (mCdt != null) {
            mCdt.cancel();
        }
    }

    private void showForceStartDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setMessage("Принудительный запуск приложения. Внимание, это не значит, что приложение будет работать! Данная функция сделана исключительно для тех ситуаций, когда приложение не смогло само определить, что клиент TOR успешно запустился (проблема некоторорых китайских аппаратов). Так что используйте только если точно знаете, что делаете");
        dialogBuilder.setPositiveButton("Да, я знаю, что делаю", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                torLoaded();
            }
        });
        dialogBuilder.setNegativeButton("Нет, подождать ещё", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        dialogBuilder.show();
    }

    private void showTorNotWorkDialog() {
        if (active) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(StartTorActivity.this.getString(R.string.tor_cant_load_message))
                    .setMessage(StartTorActivity.this.getString(R.string.tor_not_start_body))
                    .setPositiveButton(getString(R.string.try_again_message), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            App.getInstance().startTor();
                            observeTorStart();

                        }
                    })
                    .setNegativeButton(getString(R.string.try_later_message), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            StartTorActivity.this.finishAffinity();
                        }
                    })
                    .show();
        }
    }

    private void startTimer() {
        int oneMin = 180000; // 3 minute in milli seconds
        mProgressCounter = 0;
        mCdt = new CountDownTimer(oneMin, 1000) {
            public void onTick(long millisUntilFinished) {
                mProgressCounter++;
                mTorLoadingProgressIndicator.setProgress(mProgressCounter);
                if (mTor != null) {
                    String last = mTor.getLastLog();
                    if (last != null) {
                        if (!last.isEmpty()) {
                            mTorLoadingStatusText.setText(last);
                        } else {
                            mTorLoadingStatusText.setText(String.format(Locale.ENGLISH, getString(R.string.tor_continue_loading), mProgressCounter));
                        }
                    } else {
                        mTorLoadingStatusText.setText(String.format(Locale.ENGLISH, getString(R.string.tor_continue_loading), mProgressCounter));

                    }
                } else {
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
        if (active) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle("Tor load too long")
                    .setMessage("Подождём ещё или перезапустим?")
                    .setPositiveButton("Перезапуск", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            App.getInstance().startTor();
                            observeTorStart();
                        }
                    })
                    .setNegativeButton("Подождать ещё", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            restartCounter();
                        }
                    }).show();
        }
    }

    private void restartCounter() {
        startTimer();
    }

    private void startTrackingTor(final AndroidOnionProxyManager tor) {
        mTor = tor;
    }

    private void torLoaded() {
        if (mCdt != null) {
            mCdt.cancel();
        }
        if (mTorLoadingStatusText != null) {
            mTorLoadingStatusText.setText(R.string.tor_is_loaded);
        }
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

    public class TorConnectErrorReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // покажу диалоговое окно с оповещением, что TOR остановлен и кнопкой повторного запуска
            showTorRestartDialog();
        }
    }

    private void showTorRestartDialog() {
        Log.d("surprise", "StartTorActivity showTorRestartDialog: tor require restart");
        if (mTorRestartDialog == null) {
            // создам диалоговое окно
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(R.string.tor_is_stopped)
                    .setMessage(R.string.tor_restart_dialog_message)
                    .setPositiveButton(R.string.restart_tor_message, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            App.getInstance().startTor();
                            observeTorStart();
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
        if (mTorConnectErrorReceiver != null) {
            unregisterReceiver(mTorConnectErrorReceiver);
        }
        if (mTorRestartDialog != null) {
            mTorRestartDialog.dismiss();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mConfirmExit != 0) {
                if (mConfirmExit > System.currentTimeMillis() - 3000) {
                    // выйду из приложения
                    Log.d("surprise", "OPDSActivity onKeyDown exit");
                    this.finishAffinity();
                    return true;
                } else {
                    Toast.makeText(this, "Нажмите ещё раз для выхода", Toast.LENGTH_SHORT).show();
                    mConfirmExit = System.currentTimeMillis();
                    return true;
                }
            } else {
                Toast.makeText(this, "Нажмите ещё раз для выхода", Toast.LENGTH_SHORT).show();
                mConfirmExit = System.currentTimeMillis();
                return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStart() {
        super.onStart();
        active = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        active = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        active = false;
    }
}
