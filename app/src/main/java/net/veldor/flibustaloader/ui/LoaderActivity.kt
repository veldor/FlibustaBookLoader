package net.veldor.flibustaloader.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent

class LoaderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}