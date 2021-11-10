package net.veldor.flibustaloader.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.inputmethod.EditorInfo
import android.widget.CompoundButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import co.mobiwise.materialintro.shape.Focus
import co.mobiwise.materialintro.shape.FocusGravity
import co.mobiwise.materialintro.shape.ShapeType
import co.mobiwise.materialintro.view.MaterialIntroView
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.adapters.BlacklistAdapter
import net.veldor.flibustaloader.databinding.ActivityBlacklistBinding
import net.veldor.flibustaloader.utils.*


class BlacklistActivity : BaseActivity() {
    lateinit var binding: ActivityBlacklistBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlacklistBinding.inflate(layoutInflater)
        setContentView(binding.drawerLayout)
        setupUi()
        setupObservers()
    }

    private fun setupUi() {
        setupInterface()
        if (PreferencesHandler.instance.isEInk) {
            binding.blacklistBook.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.black,
                    null
                )
            )
            binding.blacklistAuthor.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.black,
                    null
                )
            )
            binding.blacklistSequence.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.black,
                    null
                )
            )
            binding.blacklistGenre.setTextColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.black,
                    null
                )
            )
            paintToolbar(binding.toolbar)
        }
        showHints()
        // скрою переход на данное активити
        val menuNav = mNavigationView.menu
        val item = menuNav.findItem(R.id.goToBlacklist)
        item.isEnabled = false
        item.isChecked = true
        // добавлю идентификатор строки поиска
        binding.resultsList.layoutManager = LinearLayoutManager(this)
        binding.resultsList.adapter = BlacklistAdapter(BlacklistBooks.instance.getBlacklist())
        binding.blacklistType.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            when (checkedId) {
                R.id.blacklistBook -> {
                    (binding.resultsList.adapter as BlacklistAdapter).changeList(BlacklistBooks.instance.getBlacklist())
                }
                R.id.blacklistAuthor -> {
                    (binding.resultsList.adapter as BlacklistAdapter).changeList(BlacklistAuthors.instance.getBlacklist())
                }
                R.id.blacklistSequence -> {
                    (binding.resultsList.adapter as BlacklistAdapter).changeList(BlacklistSequences.instance.getBlacklist())
                }
                R.id.blacklistGenre -> {
                    (binding.resultsList.adapter as BlacklistAdapter).changeList(BlacklistGenres.instance.getBlacklist())
                }
            }
        }
        binding.addToBlacklistBtn.setOnClickListener { addToBlacklist() }
        binding.switchOnlyRussian.isChecked = PreferencesHandler.instance.isOnlyRussian
        binding.switchOnlyRussian.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            PreferencesHandler.instance.isOnlyRussian = isChecked
        }

        binding.blacklistItemInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // Do whatever you want here
                addToBlacklist()
                true
            } else false
        }
    }

    private fun showHints() {
        showFirstHelp()
    }

    private fun addToBlacklist() {
        val value = binding.blacklistItemInput.text.toString().trim { it <= ' ' }
        if (value.isNotEmpty()) {
            // добавлю подписку в зависимости от типа
            when (binding.blacklistType.checkedRadioButtonId) {
                R.id.blacklistBook -> {
                    BlacklistBooks.instance.addValue(value)
                }
                R.id.blacklistAuthor -> {
                    BlacklistAuthors.instance.addValue(value)
                }
                R.id.blacklistSequence -> {
                    BlacklistSequences.instance.addValue(value)
                }
                R.id.blacklistGenre -> {
                    BlacklistGenres.instance.addValue(value)
                }
            }
            binding.blacklistItemInput.setText("")
            Toast.makeText(this, "Добавляю значение $value", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Введите значение", Toast.LENGTH_LONG).show()
            binding.blacklistItemInput.requestFocus()
        }
    }

    override fun setupObservers() {
        super.setupObservers()
        // буду отслеживать изменения списка книг
        BlacklistBooks.instance.liveBlacklistAdd.observe(this, {
            if (binding.blacklistType.checkedRadioButtonId == R.id.blacklistBook) {
                (binding.resultsList.adapter as BlacklistAdapter).itemAdded(it)
            }
        })
        BlacklistBooks.instance.liveBlacklistRemove.observe(this, {
            if (binding.blacklistType.checkedRadioButtonId == R.id.blacklistBook) {
                (binding.resultsList.adapter as BlacklistAdapter).itemRemoved(it)
            }
        })
        // буду отслеживать изменения списка книг
        BlacklistAuthors.instance.liveBlacklistAdd.observe(this, {
            if (binding.blacklistType.checkedRadioButtonId == R.id.blacklistAuthor) {
                (binding.resultsList.adapter as BlacklistAdapter).itemAdded(it)
            }
        })
        BlacklistAuthors.instance.liveBlacklistRemove.observe(this, {
            if (binding.blacklistType.checkedRadioButtonId == R.id.blacklistAuthor) {
                (binding.resultsList.adapter as BlacklistAdapter).itemRemoved(it)
            }
        })
        // буду отслеживать изменения списка книг
        BlacklistGenres.instance.liveBlacklistAdd.observe(this, {
            if (binding.blacklistType.checkedRadioButtonId == R.id.blacklistGenre) {
                (binding.resultsList.adapter as BlacklistAdapter).itemAdded(it)
            }
        })
        BlacklistGenres.instance.liveBlacklistRemove.observe(this, {
            if (binding.blacklistType.checkedRadioButtonId == R.id.blacklistGenre) {
                (binding.resultsList.adapter as BlacklistAdapter).itemRemoved(it)
            }
        })
        // буду отслеживать изменения списка книг
        BlacklistSequences.instance.liveBlacklistAdd.observe(this, {
            if (binding.blacklistType.checkedRadioButtonId == R.id.blacklistSequence) {
                (binding.resultsList.adapter as BlacklistAdapter).itemAdded(it)
            }
        })
        BlacklistSequences.instance.liveBlacklistRemove.observe(this, {
            if (binding.blacklistType.checkedRadioButtonId == R.id.blacklistSequence) {
                (binding.resultsList.adapter as BlacklistAdapter).itemRemoved(it)
            }
        })
    }
}

private fun BlacklistActivity.showFirstHelp() {
    Handler(Looper.getMainLooper()).postDelayed({
        kotlin.run {
            MaterialIntroView.Builder(this)
                .enableDotAnimation(true)
                .enableIcon(false)
                .setFocusGravity(FocusGravity.CENTER)
                .setFocusType(Focus.MINIMUM)
                .setDelayMillis(300)
                .setUsageId("blacklist type select")
                .enableFadeAnimation(true)
                .performClick(true)
                .setListener {
                    showSecondHelp()
                }
                .setInfoText(getString(R.string.blacklist_first_help_text))
                .setTarget(binding.blacklistBook)
                .setShape(ShapeType.CIRCLE)
                .show()
        }
    }, 100)
}

private fun BlacklistActivity.showSecondHelp() {
    Handler(Looper.getMainLooper()).postDelayed({
        kotlin.run {
            MaterialIntroView.Builder(this)
                .enableDotAnimation(true)
                .enableIcon(false)
                .setFocusGravity(FocusGravity.CENTER)
                .setFocusType(Focus.MINIMUM)
                .setDelayMillis(700)
                .setUsageId("blacklist value enter")
                .enableFadeAnimation(true)
                .performClick(true)
                .setListener {

                }
                .setInfoText(getString(R.string.blacklist_second_hint_text))
                .setTarget(binding.blacklistItemInput)
                .setShape(ShapeType.CIRCLE)
                .show()
        }
    }, 100)
}