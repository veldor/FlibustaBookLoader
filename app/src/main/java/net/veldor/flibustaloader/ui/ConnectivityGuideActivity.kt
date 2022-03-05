package net.veldor.flibustaloader.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import net.veldor.flibustaloader.databinding.ConnectivityGuideActivityBinding

class ConnectivityGuideActivity : AppCompatActivity() {

    private lateinit var binding: ConnectivityGuideActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ConnectivityGuideActivityBinding.inflate(layoutInflater)
        setContentView(binding.container)
    }
}