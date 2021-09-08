package net.veldor.flibustaloader.ui

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import net.veldor.flibustaloader.R
import androidx.lifecycle.LiveData
import net.veldor.flibustaloader.App
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import androidx.work.WorkInfo
import net.veldor.flibustaloader.view_models.LoginViewModel
import android.text.TextWatcher
import android.text.Editable
import android.os.Handler
import android.view.*
import android.widget.*

class LoginActivity : AppCompatActivity() {
    private lateinit var mLoginButton: Button
    private var mPasswordReady = false
    private var mLoginReady = false
    private lateinit var mMyViewModel: LoginViewModel
    private lateinit var mTextSwitcher: TextSwitcher
    private lateinit var mLogin: EditText
    private lateinit var mPassword: EditText
    private lateinit var mProgressBar: View
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        mMyViewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
        handleInput()
    }

    private fun handleInput() {
        mLogin = findViewById(R.id.username)
        mPassword = findViewById(R.id.password)
        mLoginButton = findViewById(R.id.login)
        mTextSwitcher = findViewById(R.id.statusWrapper)
        mTextSwitcher.setInAnimation(this, android.R.anim.slide_in_left)
        mTextSwitcher.setOutAnimation(this, android.R.anim.slide_out_right)
        mProgressBar = findViewById(R.id.loading)
        mLogin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                mLoginReady = if (s != null && s.length > 0) {
                    if (mPasswordReady) {
                        mLoginButton.setEnabled(true)
                    }
                    true
                } else {
                    false
                }
            }
        })
        mPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                mPasswordReady = if (s != null && s.length > 0) {
                    if (mLoginReady) {
                        mLoginButton.setEnabled(true)
                    }
                    true
                } else {
                    false
                }
            }
        })
        mLoginButton.setOnClickListener(View.OnClickListener { v: View? ->
            mProgressBar.setVisibility(View.VISIBLE)
            lockElements()
            val worker =
                mMyViewModel!!.logMeIn(mLogin.getText().toString(), mPassword.getText().toString())
            observeRequest(worker)
        })
    }

    private fun observeRequest(worker: LiveData<WorkInfo>) {
        // также подпишусь на обновления статуса запроса
        val requestStatus: LiveData<String> = App.instance.requestStatus
        worker.observe(this, { workInfo: WorkInfo? ->
            if (workInfo != null) {
                if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                    setStatusText(getString(R.string.done_message))
                    requestStatus.removeObservers(this@LoginActivity)
                    done()
                } else if (workInfo.state == WorkInfo.State.FAILED) {
                    setStatusText(getString(R.string.failed_message))
                    unlockElements()
                    // разблокирую всё
                } else if (workInfo.state == WorkInfo.State.RUNNING) {
                    setStatusText(getString(R.string.wait_for_initialize))
                } else if (workInfo.state == WorkInfo.State.ENQUEUED) {
                    setStatusText(getString(R.string.wait_for_internet_message))
                }
            }
        })
        requestStatus.observe(this, { s: String? ->
            if (s != null && !s.isEmpty()) {
                setStatusText(s)
            }
        })
    }

    private fun done() {
        // через секунду закрою активити
        val handler = Handler()
        handler.postDelayed({
            val intent = Intent()
            setResult(RESULT_OK, intent)
            finish()
        }, 1000)
    }

    private fun unlockElements() {
        mLogin.isEnabled = true
        mLoginButton.isEnabled = true
        mPassword.isEnabled = true
        mProgressBar.visibility = View.GONE
    }

    private fun lockElements() {
        mLogin.isEnabled = false
        mLoginButton.isEnabled = false
        mPassword.isEnabled = false
    }

    private fun setStatusText(status: String) {
        mTextSwitcher.setText(status)
    }
}