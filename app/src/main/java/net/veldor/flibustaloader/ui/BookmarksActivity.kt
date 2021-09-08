package net.veldor.flibustaloader.ui

import androidx.recyclerview.widget.RecyclerView
import android.os.Bundle
import net.veldor.flibustaloader.R
import androidx.recyclerview.widget.LinearLayoutManager
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.adapters.BookmarksAdapter

class BookmarksActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.new_bookmarks_activity)
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
        val recycler = findViewById<RecyclerView>(R.id.resultsList)
        val adapter = BookmarksAdapter(bookmarksList)
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this)
    }
}