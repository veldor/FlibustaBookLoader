package net.veldor.flibustaloader.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextSwitcher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.view_models.LoginViewModel

class LoginActivity : AppCompatActivity() {
    private lateinit var mLoginButton: Button
    private var mPasswordReady = false
    private var mLoginReady = false
    private lateinit var viewModel: LoginViewModel
    private lateinit var mTextSwitcher: TextSwitcher
    private lateinit var mLogin: EditText
    private lateinit var mPassword: EditText
    private lateinit var mProgressBar: View
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
        handleInput()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.liveLoginResult.observe(this, {
            if(it == LoginViewModel.LOGIN_SUCCESS){
                setStatusText(getString(R.string.done_message))
                done()
            }
            else if(it == LoginViewModel.LOGIN_FAILED){
                setStatusText(getString(R.string.failed_message))
                unlockElements()
            }
        })
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
                mLoginReady = if (s.isNotEmpty()) {
                    if (mPasswordReady) {
                        mLoginButton.isEnabled = true
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
                mPasswordReady = if (s.isNotEmpty()) {
                    if (mLoginReady) {
                        mLoginButton.isEnabled = true
                    }
                    true
                } else {
                    false
                }
            }
        })
        mLoginButton.setOnClickListener {
            mProgressBar.visibility = View.VISIBLE
            lockElements()
                viewModel.logMeIn(mLogin.text.toString(), mPassword.text.toString())
        }
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