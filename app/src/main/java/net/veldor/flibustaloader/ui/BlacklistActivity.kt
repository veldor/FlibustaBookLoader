package net.veldor.flibustaloader.ui

import androidx.recyclerview.widget.RecyclerView
import android.os.Bundle
import net.veldor.flibustaloader.R
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import net.veldor.flibustaloader.App
import androidx.appcompat.widget.SwitchCompat
import net.veldor.flibustaloader.adapters.BlacklistAdapter
import android.util.Log
import android.view.*
import android.widget.*
import net.veldor.flibustaloader.utils.*

class BlacklistActivity : BaseActivity() {
    private lateinit var mRadioContainer: RadioGroup
    private lateinit var mBlacklistAdapter: BlacklistAdapter
    private lateinit var mRecycler: RecyclerView
    private var mBooksBlacklistContainer: BlacklistBooks? = null
    private var mInput: EditText? = null
    private var mAuthorsBlacklistContainer: BlacklistAuthors? = null
    private var mSequencesBlacklistContainer: BlacklistSequences? = null
    private var mGenresBlacklistContainer: BlacklistGenres? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.new_blacklist_activity)
        setupUi()
        setupObservers()
    }

    private fun setupUi() {
        setupInterface()

        // скрою переход на данное активити
        val menuNav = mNavigationView.menu
        val item = menuNav.findItem(R.id.goToBlacklist)
        item.isEnabled = false
        item.isChecked = true
        // добавлю идентификатор строки поиска
        mInput = findViewById(R.id.blacklist_name)
        mRecycler = findViewById(R.id.resultsList)
        mRecycler.layoutManager = LinearLayoutManager(this)
        mBooksBlacklistContainer = App.instance.booksBlacklist
        mAuthorsBlacklistContainer = App.instance.authorsBlacklist
        mSequencesBlacklistContainer = App.instance.sequencesBlacklist
        mGenresBlacklistContainer = App.instance.genresBlacklist
        showBooks()
        // отслежу переключение типа добавления
        mRadioContainer = findViewById(R.id.blacklist_type)
        mRadioContainer.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            if (checkedId == R.id.blacklistBook) {
                showBooks()
            } else if (checkedId == R.id.blacklistAuthor) {
                showAuthors()
            } else if (checkedId == R.id.blacklistSequence) {
                showSequences()
            } else if (checkedId == R.id.blacklistGenre) {
                showGenres()
            }
        }
        val subscribeBtn = findViewById<Button>(R.id.add_to_blacklist_btn)
        subscribeBtn?.setOnClickListener { view: View? -> addToBlacklist(view) }
        val switchOnlyRussian = findViewById<SwitchCompat>(R.id.switchOnlyRussian)
        switchOnlyRussian.isChecked = PreferencesHandler.instance.isOnlyRussian
        switchOnlyRussian.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            PreferencesHandler.instance.isOnlyRussian = isChecked
        }
    }

    private fun addToBlacklist(view: View?) {
        val value = mInput!!.text.toString().trim { it <= ' ' }
        if (value.isNotEmpty()) {
            // добавлю подписку в зависимости от типа
            when (mRadioContainer.checkedRadioButtonId) {
                R.id.blacklistBook -> {
                    mBooksBlacklistContainer!!.addValue(value)
                }
                R.id.blacklistAuthor -> {
                    mAuthorsBlacklistContainer!!.addValue(value)
                }
                R.id.blacklistSequence -> {
                    mSequencesBlacklistContainer!!.addValue(value)
                }
                R.id.blacklistGenre -> {
                    mGenresBlacklistContainer!!.addValue(value)
                }
            }
            mInput!!.setText("")
            Toast.makeText(this, "Добавляю значение $value", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Введите значение", Toast.LENGTH_LONG).show()
            mInput!!.requestFocus()
        }
    }

    private fun showBooks() {
        // получу подписки на книги
        val autocompleteValues = mBooksBlacklistContainer!!.getBlacklist()
        mBlacklistAdapter = BlacklistAdapter(autocompleteValues!!)
        mRecycler.adapter = mBlacklistAdapter
    }

    private fun showAuthors() {
        // получу подписки на авторов
        val autocompleteValues = mAuthorsBlacklistContainer!!.getBlacklist()
        mBlacklistAdapter = BlacklistAdapter(autocompleteValues!!)
        mRecycler.adapter = mBlacklistAdapter
    }

    private fun showSequences() {
        // получу чёрный список серий
        val autocompleteValues = mSequencesBlacklistContainer!!.getBlacklist()
        mBlacklistAdapter = BlacklistAdapter(autocompleteValues!!)
        mRecycler.adapter = mBlacklistAdapter
    }

    private fun showGenres() {
        // получу чёрный список жанров
        val autocompleteValues = mGenresBlacklistContainer!!.getBlacklist()
        mBlacklistAdapter = BlacklistAdapter(autocompleteValues!!)
        mRecycler.adapter = mBlacklistAdapter
    }

    override fun setupObservers() {
        super.setupObservers()
        Log.d("surprise", "BlacklistActivity.java 95 setupObservers: setting up observers")
        // буду отслеживать изменения списка книг
        val refresh: LiveData<Boolean> = BlacklistBooks.mListRefreshed
        refresh.observe(this, { aBoolean: Boolean? ->
            if (aBoolean != null && aBoolean && mRadioContainer.checkedRadioButtonId == R.id.blacklistBook) {
                refreshBooksBlacklist()
            }
        })


        // буду отслеживать изменения списка авторов
        val authorRefresh: LiveData<Boolean> = BlacklistAuthors.mListRefreshed
        authorRefresh.observe(this, { aBoolean: Boolean? ->
            if (aBoolean != null && aBoolean && mRadioContainer.checkedRadioButtonId == R.id.blacklistAuthor) {
                refreshAuthorBlacklist()
            }
        })

        // буду отслеживать изменения списка серий
        val sequencesRefresh: LiveData<Boolean> = BlacklistSequences.mListRefreshed
        sequencesRefresh.observe(this, { aBoolean: Boolean? ->
            if (aBoolean != null && aBoolean && mRadioContainer.checkedRadioButtonId == R.id.blacklistSequence) {
                refreshSequencesBlacklist()
            }
        })

        // буду отслеживать изменения списка жанров
        val genresRefresh: LiveData<Boolean> = BlacklistGenres.mListRefreshed
        genresRefresh.observe(this, { aBoolean: Boolean? ->
            if (aBoolean != null && aBoolean && mRadioContainer.checkedRadioButtonId == R.id.blacklistGenre) {
                refreshGenresBlacklist()
            }
        })
    }

    private fun refreshBooksBlacklist() {
        val autocompleteValues = mBooksBlacklistContainer!!.getBlacklist()
        mBlacklistAdapter.changeList(autocompleteValues!!)
        mBlacklistAdapter.notifyDataSetChanged()
    }

    private fun refreshAuthorBlacklist() {
        val blacklist = mAuthorsBlacklistContainer!!.getBlacklist()
        mBlacklistAdapter.changeList(blacklist!!)
        mBlacklistAdapter.notifyDataSetChanged()
    }

    private fun refreshSequencesBlacklist() {
        val blacklist = mSequencesBlacklistContainer!!.getBlacklist()
        mBlacklistAdapter.changeList(blacklist!!)
        mBlacklistAdapter.notifyDataSetChanged()
    }

    private fun refreshGenresBlacklist() {
        val blacklist = mGenresBlacklistContainer!!.getBlacklist()
        mBlacklistAdapter.changeList(blacklist!!)
        mBlacklistAdapter.notifyDataSetChanged()
    }
}