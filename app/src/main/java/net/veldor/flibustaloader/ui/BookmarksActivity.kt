package net.veldor.flibustaloader.ui

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.adapters.BookmarksAdapter
import net.veldor.flibustaloader.databinding.ActivityBookmarksBinding
import net.veldor.flibustaloader.utils.PreferencesHandler

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
        if(PreferencesHandler.instance.isEInk){
            paintToolbar(binding.toolbar)
        }
        binding.toolbar.title = getString(R.string.bookmarks_title)
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