package net.veldor.flibustaloader.ui

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.adapters.BookmarksAdapter
import net.veldor.flibustaloader.databinding.ActivityBookmarksBinding

class BookmarksActivity : BaseActivity() {
    private lateinit var binding: ActivityBookmarksBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookmarksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupInterface()
    }

    override fun setupInterface() {
        super.setupInterface()
        // скрою переход на данное активити
        val menuNav = mNavigationView.menu
        val item = menuNav.findItem(R.id.goToBookmarks)
        item.isEnabled = false
        item.isChecked = true

        // получу список закладок
        val bookmarksList = App.instance.mDatabase.bookmarksDao().allBookmarks
        binding.resultsList.layoutManager = LinearLayoutManager(this)
        binding.resultsList.adapter = BookmarksAdapter(bookmarksList)
    }
}