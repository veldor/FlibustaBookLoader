package net.veldor.flibustaloader.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.SearchView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.databinding.FragmentWebViewBinding
import net.veldor.flibustaloader.dialogs.ChangelogDialog
import net.veldor.flibustaloader.ui.BaseActivity
import net.veldor.flibustaloader.ui.LoginActivity
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.utils.URLHelper
import net.veldor.flibustaloader.utils.XMLHandler
import net.veldor.flibustaloader.view_models.WebViewViewModel
import java.util.*

open class WebViewFragment : Fragment(), SearchView.OnQueryTextListener {
    lateinit var binding: FragmentWebViewBinding
    lateinit var viewModel: WebViewViewModel
    private lateinit var autocompleteStrings: ArrayList<String>
    private lateinit var mSearchView: SearchView
    private var mSearchAdapter: ArrayAdapter<String>? = null
    private lateinit var mSearchAutocomplete: SearchView.SearchAutoComplete

    private var loginLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                requireActivity().invalidateOptionsMenu()
                binding.myWebView.loadUrl(PreferencesHandler.instance.lastLoadedUrl)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        activity?.invalidateOptionsMenu()
        viewModel = ViewModelProvider(this).get(WebViewViewModel::class.java)
        binding = FragmentWebViewBinding.inflate(inflater, container, false)
        val root = binding.root
        setHasOptionsMenu(true)
        setupInterface()
        showChangesList()
        setupObservers()
        autocompleteStrings = viewModel.searchAutocomplete
        handleLoading()
        return root
    }

    private fun handleLoading() {
        binding.myWebView.setup()
        Log.d(
            "surprise",
            "startBrowsing: last loaded is ${PreferencesHandler.instance.lastLoadedUrl}"
        )
        binding.myWebView.loadUrl(PreferencesHandler.instance.lastLoadedUrl)
    }

    private fun showChangesList() {
        // покажу список изменений, если он ещё не показан для этой версии
        if (PreferencesHandler.instance.isShowChanges()) {
            ChangelogDialog.Builder(requireContext()).build().show()
            PreferencesHandler.instance.setChangesViewed()
        }
    }

    private fun setupObservers() {
        WebViewViewModel.pageText.observe(viewLifecycleOwner, {
            viewModel.parseText()
        })
        viewModel.pageParseResult.observe(viewLifecycleOwner, {
            if (it != null) {
                if (it.linksList.size > 0) {
                    binding.floatingMenu.visibility = View.VISIBLE
                } else {
                    binding.floatingMenu.visibility = View.GONE
                }
            }
        })

        // буду отслеживать событие логина
        val cookieObserver: LiveData<Boolean> = App.sResetLoginCookie
        cookieObserver.observe(viewLifecycleOwner, { aBoolean: Boolean ->
            if (aBoolean) {
                Toast.makeText(
                    requireContext(),
                    "Данные для входа устарели, придётся войти ещё раз",
                    Toast.LENGTH_SHORT
                ).show()
                requireActivity().invalidateOptionsMenu()
            }
        })
    }

    private fun showSelectBookDownloadTypeDialog() {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setTitle("Выберите формат скачивания")
            .setItems(viewModel.pageParseResult.value!!.types.toTypedArray()) { _: DialogInterface, i: Int ->
                viewModel.webViewDownload(i)
            }
            .create()
            .show()
    }

    private fun setupInterface() {
        // настрою FAB
        binding.fabDownloadAll.setOnClickListener {
            // select type for download
            binding.floatingMenu.close(true)
            showSelectBookDownloadTypeDialog()
        }

        // буду отслеживать событие логина
        val cookieObserver: LiveData<Boolean> = App.sResetLoginCookie
        cookieObserver.observe(requireActivity(), { aBoolean: Boolean ->
            if (aBoolean) {
                Toast.makeText(
                    requireContext(),
                    "Данные для входа устарели, придётся войти ещё раз",
                    Toast.LENGTH_SHORT
                ).show()
                requireActivity().invalidateOptionsMenu()
            }
        })
    }


    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }

        // добавлю обработку поиска
        val searchMenuItem = menu.findItem(R.id.action_search)
        mSearchView = searchMenuItem.actionView as SearchView
        mSearchView.inputType = InputType.TYPE_CLASS_TEXT
        val size = Point()
        requireActivity().windowManager.defaultDisplay.getSize(size)
        mSearchView.maxWidth = size.x - 340
        mSearchView.setOnQueryTextListener(this)
        mSearchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(i: Int): Boolean {
                return true
            }

            override fun onSuggestionClick(i: Int): Boolean {
                val value = autocompleteStrings[i]
                mSearchView.setQuery(value, true)
                return true
            }
        })
        mSearchAutocomplete = mSearchView.findViewById(R.id.search_src_text)
        if (PreferencesHandler.instance.isEInk) {
            mSearchAutocomplete.setTextColor(Color.WHITE)
        }
        mSearchAutocomplete.setDropDownBackgroundResource(android.R.color.white)
        mSearchAutocomplete.dropDownAnchor = R.id.action_search
        mSearchAutocomplete.threshold = 0
        mSearchAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, autocompleteStrings)
        mSearchAutocomplete.setAdapter(mSearchAdapter)
        when (viewModel.viewMode) {
            VIEW_MODE_NORMAL -> menu.findItem(R.id.menuUseNormalStyle).isChecked = true
            VIEW_MODE_LIGHT -> menu.findItem(R.id.menuUseLightStyle).isChecked = true
            VIEW_MODE_FAST -> menu.findItem(R.id.menuUseLightFastStyle).isChecked = true
            VIEW_MODE_FAT -> menu.findItem(R.id.menuUseLightFatStyle).isChecked = true
            VIEW_MODE_FAST_FAT -> menu.findItem(R.id.menuUseFatFastStyle).isChecked = true
        }
        var menuItem = menu.findItem(R.id.menuUseDarkMode)
        menuItem.isChecked = viewModel.nightModeEnabled
        menuItem = menu.findItem(R.id.logOut)
        menuItem.isVisible = PreferencesHandler.instance.authCookie != null
        menuItem = menu.findItem(R.id.login)
        if (PreferencesHandler.instance.authCookie != null) {
            menuItem.isVisible = false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.menuUseLightStyle || itemId == R.id.menuUseLightFastStyle || itemId == R.id.menuUseLightFatStyle || itemId == R.id.menuUseNormalStyle || itemId == R.id.menuUseFatFastStyle) {
            viewModel.switchViewMode(item.itemId)
            requireActivity().invalidateOptionsMenu()
            binding.myWebView.reload()
            return true
        } else if (itemId == R.id.goHome) {
            binding.myWebView.loadUrl("")
            return true
        } else if (itemId == R.id.showNew) {
            binding.myWebView.loadUrl(NEW_BOOKS)
            return true
        } else if (itemId == R.id.randomBook) {
            binding.myWebView.loadUrl(viewModel.randomBookUrl)
            return true
        } else if (itemId == R.id.shareLink) {
            viewModel.shareLink(binding.myWebView)
            return true
        } else if (itemId == R.id.login) {
            showLoginDialog()
            return true
        } else if (itemId == R.id.clearSearchHistory) {
            clearHistory()
            return true
        } else if (itemId == R.id.logOut) { // удалю куку
            PreferencesHandler.instance.authCookie = null
            requireActivity().recreate()
            return true
        }
        if (item.itemId == R.id.menuUseDarkMode) {
            viewModel.switchNightMode()
            Handler().postDelayed(BaseActivity.ResetApp(), 100)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun clearHistory() {
        viewModel.clearHistory()
        autocompleteStrings = ArrayList()
        mSearchAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, autocompleteStrings)
        mSearchAutocomplete.setAdapter(mSearchAdapter)
        Toast.makeText(requireContext(), "Автозаполнение сброшено", Toast.LENGTH_SHORT).show()
    }

    private fun showLoginDialog() {
        loginLauncher.launch(Intent(requireContext(), LoginActivity::class.java))
    }

    override fun onQueryTextSubmit(s: String): Boolean {
        if (!TextUtils.isEmpty(s.trim { it <= ' ' })) {
            // ищу введённое значение
            makeSearch(s)
        }
        return true
    }


    override fun onQueryTextChange(s: String): Boolean {
        return false
    }

    private fun makeSearch(s: String) {
        val searchString = URLHelper.getFlibustaUrl() + SEARCH_URL + s.trim { it <= ' ' }
        binding.myWebView.loadUrl(searchString)
        // занесу значение в список автозаполнения
        if (XMLHandler.putSearchValue(s)) {
            // обновлю список поиска
            autocompleteStrings = viewModel.searchAutocomplete
            mSearchAdapter!!.clear()
            mSearchAdapter!!.addAll(autocompleteStrings)
            mSearchAdapter!!.notifyDataSetChanged()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.myWebView.saveState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        binding.myWebView.restoreState(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        setWebViewBackground()
    }

    private fun setWebViewBackground() {
        if (viewModel.nightModeEnabled) {
            binding.myWebView.setBackgroundColor(
                ResourcesCompat.getColor(
                    resources,
                    android.R.color.black,
                    null
                )
            )
        } else {
            binding.myWebView.setBackgroundColor(
                ResourcesCompat.getColor(
                    resources,
                    android.R.color.white,
                    null
                )
            )
        }
    }

    companion object {
        const val VIEW_MODE_NORMAL = 1
        const val VIEW_MODE_LIGHT = 2
        const val VIEW_MODE_FAT = 3
        const val VIEW_MODE_FAST = 4
        const val VIEW_MODE_FAST_FAT = 5
        const val NEW_BOOKS = "/new"
        const val SEARCH_URL = "/booksearch?ask="
    }
}