package net.veldor.flibustaloader.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextSwitcher;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.WorkInfo;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.view_models.LoginViewModel;

import static androidx.work.WorkInfo.State.ENQUEUED;
import static androidx.work.WorkInfo.State.FAILED;
import static androidx.work.WorkInfo.State.RUNNING;
import static androidx.work.WorkInfo.State.SUCCEEDED;

public class LoginActivity extends AppCompatActivity {
    private Button mLoginButton;
    private boolean mPasswordReady;
    private boolean mLoginReady;
    private LoginViewModel mMyViewModel;
    private TextSwitcher mTextSwitcher;
    private EditText mLogin;
    private EditText mPassword;
    private View mProgressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mMyViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        handleInput();
    }

    private void handleInput() {
        mLogin = findViewById(R.id.username);
        mPassword = findViewById(R.id.password);
        mLoginButton = findViewById(R.id.login);

        mTextSwitcher = findViewById(R.id.statusWrapper);
        mTextSwitcher.setInAnimation(this, android.R.anim.slide_in_left);
        mTextSwitcher.setOutAnimation(this, android.R.anim.slide_out_right);
        mProgressBar = findViewById(R.id.loading);

        mLogin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if(s != null && s.length() > 0){
                    if(mPasswordReady){
                        mLoginButton.setEnabled(true);
                    }
                    mLoginReady = true;
                }
                else{
                    mLoginReady = false;
                }
            }
        });

        mPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if(s != null && s.length() > 0){
                    if(mLoginReady){
                        mLoginButton.setEnabled(true);
                    }
                    mPasswordReady = true;
                }
                else{
                    mPasswordReady = false;
                }
            }
        });

        mLoginButton.setOnClickListener(v -> {
            mProgressBar.setVisibility(View.VISIBLE);
            lockElements();
            LiveData<WorkInfo> worker = mMyViewModel.logMeIn(mLogin.getText().toString(), mPassword.getText().toString());
            observeRequest(worker);
        });
    }

    private void observeRequest(LiveData<WorkInfo> worker) {
        // также подпишусь на обновления статуса запроса
        final LiveData<String> requestStatus = App.getInstance().RequestStatus;
        worker.observe(this, workInfo -> {
            if (workInfo != null) {
                if (workInfo.getState() == SUCCEEDED) {
                    setStatusText(getString(R.string.done_message));
                    requestStatus.removeObservers(LoginActivity.this);
                    done();
                } else if (workInfo.getState() == FAILED) {
                    setStatusText(getString(R.string.failed_message));
                    unlockElements();
                    // разблокирую всё
                } else if (workInfo.getState() == RUNNING) {
                    setStatusText(getString(R.string.wait_for_initialize));
                } else if (workInfo.getState() == ENQUEUED) {
                    setStatusText(getString(R.string.wait_for_internet_message));
                }
            }
        });
        requestStatus.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                if(s != null && !s.isEmpty()){
                    setStatusText(s);
                }
            }
        });
    }

    private void done() {
        // через секунду закрою активити
        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
            finish();
        }, 1000);
    }

    private void unlockElements() {
        mLogin.setEnabled(true);
        mLoginButton.setEnabled(true);
        mPassword.setEnabled(true);
        mProgressBar.setVisibility(View.GONE);
    }
    private void lockElements() {
        mLogin.setEnabled(false);
        mLoginButton.setEnabled(false);
        mPassword.setEnabled(false);
    }

    private void setStatusText(String status) {
        mTextSwitcher.setText(status);
    }
}