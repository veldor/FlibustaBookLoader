package net.veldor.flibustaloader.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.databinding.ActivityLoginBinding
import net.veldor.flibustaloader.view_models.LoginViewModel

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private var mPasswordReady = false
    private var mLoginReady = false
    private lateinit var viewModel: LoginViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
        handleInput()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.liveLoginResult.observe(this) {
            if (it == LoginViewModel.LOGIN_SUCCESS) {
                setStatusText(getString(R.string.done_message))
                done()
            } else if (it == LoginViewModel.LOGIN_FAILED) {
                setStatusText(getString(R.string.failed_message))
                unlockElements()
            }
        }
    }

    private fun handleInput() {
        binding.statusWrapper.setInAnimation(this, android.R.anim.slide_in_left)
        binding.statusWrapper.setOutAnimation(this, android.R.anim.slide_out_right)
        binding.nameEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                mLoginReady = if (s.isNotEmpty()) {
                    if (mPasswordReady) {
                        binding.login.isEnabled = true
                    }
                    true
                } else {
                    false
                }
            }
        })
        binding.passEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                mPasswordReady = if (s.isNotEmpty()) {
                    if (mLoginReady) {
                        binding.login.isEnabled = true
                    }
                    true
                } else {
                    false
                }
            }
        })
        binding.login.setOnClickListener {
            binding.loading.visibility = View.VISIBLE
            lockElements()
                viewModel.logMeIn(binding.nameEditText.text.toString(), binding.passEditText.text.toString())
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
        binding.nameEditText.isEnabled = true
        binding.login.isEnabled = true
        binding.passEditText.isEnabled = true
        binding.loading.visibility = View.GONE
    }

    private fun lockElements() {
        binding.nameEditText.isEnabled = false
        binding.login.isEnabled = false
        binding.passEditText.isEnabled = false
    }

    private fun setStatusText(status: String) {
        binding.statusWrapper.setText(status)
    }
}