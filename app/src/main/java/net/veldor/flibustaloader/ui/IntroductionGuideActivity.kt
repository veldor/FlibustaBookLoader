package net.veldor.flibustaloader.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import net.veldor.flibustaloader.databinding.ConnectivityGuideActivityBinding
import net.veldor.flibustaloader.databinding.IntroductionGuideActivityBinding

class IntroductionGuideActivity : AppCompatActivity() {

    private lateinit var binding: IntroductionGuideActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = IntroductionGuideActivityBinding.inflate(layoutInflater)
        setContentView(binding.container)
    }
}