package net.veldor.flibustaloader.ui

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.documentfile.provider.DocumentFile
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.work.*
import lib.folderpicker.FolderPicker
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.databinding.ActivityPreferencesBinding
import net.veldor.flibustaloader.utils.FilesHandler
import net.veldor.flibustaloader.utils.Grammar
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.utils.TransportUtils
import net.veldor.flibustaloader.view_models.SettingsViewModel
import java.io.File
import java.util.*

class SettingsActivity : BaseActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    private lateinit var binding: ActivityPreferencesBinding
    private lateinit var viewModel: SettingsViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(SettingsViewModel::class.java)
        binding = ActivityPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.drawerLayout)
        setupInterface()

        // добавлю главный фрагмент
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
    }

    override fun setupInterface() {
        super.setupInterface()
        if (PreferencesHandler.instance.isEInk) {
            paintToolbar(binding.toolbar)
        }
        setTheme(R.style.preferencesStyle)
        // скрою переход на данное активити
        val menuNav = mNavigationView.menu
        val item = menuNav.findItem(R.id.goToSettings)
        item.isEnabled = false
        item.isChecked = true
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        return false
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_root, rootKey)
        }
    }

    @Suppress("unused")
    class ReservePreferencesFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_reserve, rootKey)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            // получу ссылку на резервирование настроек
            val settingsBackupPref = findPreference<Preference>(getString(R.string.backup_settings))
            val settingsRestorePref =
                findPreference<Preference>(getString(R.string.restore_settings))
            if (settingsBackupPref != null) {
                settingsBackupPref.onPreferenceClickListener =
                    Preference.OnPreferenceClickListener {
                        Toast.makeText(
                            context,
                            "Выберите папку для сохранения резервной копии",
                            Toast.LENGTH_SHORT
                        ).show()

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            var intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            intent.addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                        or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                        or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                            )
                            if (TransportUtils.intentCanBeHandled(intent)) {
                                backupDirSelectResultLauncher.launch(intent)
                            } else {
                                intent = Intent(context, FolderPicker::class.java)
                                intent.addFlags(
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                            or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                )
                                //compatBackupDirSelectResultLauncher.launch(intent)
                            }
                        } else {
                            val intent = Intent(context, FolderPicker::class.java)
                            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                                intent.addFlags(
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                            or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                )
                            }
                            compatBackupDirSelectResultLauncher.launch(intent)
                        }
                        false
                    }
            }
            if (settingsRestorePref != null) {
                settingsRestorePref.onPreferenceClickListener =
                    Preference.OnPreferenceClickListener {
                        Toast.makeText(
                            context,
                            "Выберите сохранённый ранее файл с настройками.",
                            Toast.LENGTH_LONG
                        ).show()
                        // открою окно выбота файла для восстановления
                        val intent: Intent =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                Intent(Intent.ACTION_OPEN_DOCUMENT)
                            } else {
                                Intent(Intent.ACTION_GET_CONTENT)
                            }
                        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        intent.type = "application/zip"
                        if (TransportUtils.intentCanBeHandled(intent)) {
                            Toast.makeText(context, "Восстанавливаю настройки.", Toast.LENGTH_LONG)
                                .show()
                            restoreFileSelectResultLauncher.launch(intent)
                        } else {
                            Toast.makeText(
                                context,
                                "Упс, не нашлось приложения, которое могло бы это сделать.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        false
                    }
            }
        }


        private var backupDirSelectResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    if (result != null) {
                        val treeUri = result.data!!.data
                        if (treeUri != null) {
                            // проверю наличие файла
                            val dl = DocumentFile.fromTreeUri(App.instance, treeUri)
                            if (dl != null && dl.isDirectory) {
                                val builder = AlertDialog.Builder(requireContext())
                                // покажу список с выбором того, что нужно резервировать
                                val backupOptions = arrayOf(
                                    "Базовые настройки", // 0
                                    "Загруженные книги", // 1
                                    "Прочитанные книги", // 2
                                    "Автозаполнение поиска", // 3
                                    "Список закладок", // 4
                                    "Подписки на книги", // 5
                                    "Подписки на авторов", // 6
                                    "Подписки на жанры", // 7
                                    "Подписки на серии", // 8
                                    "Фильтры книг",  // 9
                                    "Фильтры авторов",  // 10
                                    "Фильтры жанров", // 11
                                    "Фильтры серий", // 12
                                    "Фильтры форматов", // 13
                                    "Список книг для загрузки", // 14
                                )
                                val checkedOptions = booleanArrayOf(
                                    true,
                                    true,
                                    true,
                                    true,
                                    true,
                                    true,
                                    true,
                                    true,
                                    true,
                                    true,
                                    true,
                                    true,
                                    true,
                                    true,
                                    true
                                )
                                builder.setMultiChoiceItems(
                                    backupOptions, checkedOptions
                                ) { _, which, isChecked ->
                                    checkedOptions[which] = isChecked
                                }
                                    .setTitle("Выберите элементы для резервирования")
                                // Set the positive/yes button click listener

                                // Set the positive/yes button click listener
                                builder.setPositiveButton("OK") { _, _ ->
                                    (requireActivity() as SettingsActivity).viewModel.backup(
                                        dl,
                                        checkedOptions
                                    )
                                    (requireActivity() as SettingsActivity).viewModel.liveBackupData.observe(
                                        viewLifecycleOwner,
                                        {
                                            if (it != null) {
                                                // send file
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                                    FilesHandler.shareFile(it, "Share settings")
                                                }
                                                (requireActivity() as SettingsActivity).viewModel.liveBackupData.removeObservers(
                                                    viewLifecycleOwner
                                                )
                                            } else {
                                                Toast.makeText(
                                                    requireContext(),
                                                    "Can't create backup file, try again!",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        })
                                }
                                builder.show()
                            }
                        }
                    }
                }
            }

        private var compatBackupDirSelectResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    if (result != null) {
                        val data: Intent? = result.data
                        if (data != null && data.extras != null && data.extras!!.containsKey("data")) {
                            val folderLocation = data.extras!!.getString("data")
                            val file = File(folderLocation)
                            if (file.isDirectory) {
                                val builder = AlertDialog.Builder(requireContext())
                                // покажу список с выбором того, что нужно резервировать
                                val backupOptions = arrayOf(
                                    "Базовые настройки", // 0
                                    "Загруженные книги", // 1
                                    "Прочитанные книги", // 2
                                    "Автозаполнение поиска", // 3
                                    "Список закладок", // 4
                                    "Подписки на книги", // 5
                                    "Подписки на авторов", // 6
                                    "Подписки на жанры", // 7
                                    "Подписки на серии", // 8
                                    "Фильтры книг",  // 9
                                    "Фильтры авторов",  // 10
                                    "Фильтры жанров", // 11
                                    "Фильтры серий", // 12
                                    "Фильтры форматов", // 13
                                    "Список книг для загрузки" // 14
                                )

                                val checkedOptions = booleanArrayOf(
                                    true, // 0
                                    true, // 1
                                    true, // 2
                                    true, // 3
                                    true, // 4
                                    true, // 5
                                    true, // 6
                                    true, // 7
                                    true, // 8
                                    true, // 9
                                    true, // 10
                                    true, // 11
                                    true, // 12
                                    true, // 13
                                    true // 14
                                )
                                builder.setMultiChoiceItems(
                                    backupOptions, checkedOptions
                                ) { _, which, isChecked ->
                                    checkedOptions[which] = isChecked
                                }
                                    .setTitle("Выберите элементы для резервирования")
                                // Set the positive/yes button click listener

                                // Set the positive/yes button click listener
                                builder.setPositiveButton("OK") { _, _ ->
                                    (requireActivity() as SettingsActivity).viewModel.backup(
                                        file,
                                        options = checkedOptions
                                    )
                                    (requireActivity() as SettingsActivity).viewModel.liveCompatBackupData.observe(
                                        viewLifecycleOwner,
                                        {
                                            if (it != null) {
                                                // send file
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                                    FilesHandler.shareFile(
                                                        it,
                                                        getString(R.string.share_settings_message)
                                                    )
                                                }
                                                (requireActivity() as SettingsActivity).viewModel.liveCompatBackupData.removeObservers(
                                                    viewLifecycleOwner
                                                )
                                            } else {
                                                Toast.makeText(
                                                    requireContext(),
                                                    "Can't create backup file, try again!",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        })
                                }
                                builder.show()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Не удалось сохранить папку, попробуйте ещё раз!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }

        private var restoreFileSelectResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val uri: Uri?
                        if (result != null) {
                            uri = result.data?.data
                            if (uri != null) {
                                val dl = DocumentFile.fromSingleUri(App.instance, uri)
                                if (dl != null) {
                                    // буду восстанавливать настройки
                                    // попробую получить список файлов в архиве
                                    val builder = AlertDialog.Builder(requireContext())
                                    val checkResults =
                                        (requireActivity() as SettingsActivity).viewModel.checkReserve(
                                            dl
                                        )
                                    // покажу список с выбором того, что нужно резервировать
                                    val backupOptions = arrayOf(
                                        "Базовые настройки", // 0
                                        "Загруженные книги", // 1
                                        "Прочитанные книги", // 2
                                        "Автозаполнение поиска", // 3
                                        "Список закладок", // 4
                                        "Подписки на книги", // 5
                                        "Подписки на авторов", // 6
                                        "Подписки на жанры", // 7
                                        "Подписки на серии", // 8
                                        "Фильтры книг",  // 9
                                        "Фильтры авторов",  // 10
                                        "Фильтры жанров", // 11
                                        "Фильтры серий", // 12
                                        "Фильтры форматов", // 13
                                        "Список книг для загрузки" // 14
                                    )
                                    builder.setMultiChoiceItems(
                                        backupOptions, checkResults
                                    ) { _, which, isChecked ->
                                        checkResults[which] = isChecked
                                    }
                                        .setTitle("Выберите элементы для резервирования")
                                        .setPositiveButton("OK") { _, _ ->
                                            (requireActivity() as SettingsActivity).viewModel.restore(
                                                dl, checkResults
                                            )
                                            (requireActivity() as SettingsActivity).viewModel.livePrefsRestored.observe(
                                                viewLifecycleOwner,
                                                {
                                                    if (it) {
                                                        Toast.makeText(
                                                            requireContext(),
                                                            "Preferences restored, reboot app",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                        Handler().postDelayed(ResetApp(), 3000)
                                                    }
                                                })
                                        }
                                        .show()

                                }
                            }
                        }
                    } else {
                        if (result != null) {
                            val data: Intent? = result.data
                            if (data != null && data.extras != null && data.extras!!.containsKey("data")) {
                                val folderLocation = data.extras!!.getString("data")
                                val file = File(folderLocation)
                                if (file.isFile) {
                                    // буду восстанавливать настройки
                                    // попробую получить список файлов в архиве
                                    val builder = AlertDialog.Builder(requireContext())
                                    val checkResults =
                                        (requireActivity() as SettingsActivity).viewModel.checkReserve(
                                            file
                                        )
                                    // покажу список с выбором того, что нужно резервировать
                                    val backupOptions = arrayOf(
                                        "Базовые настройки", // 0
                                        "Загруженные книги", // 1
                                        "Прочитанные книги", // 2
                                        "Автозаполнение поиска", // 3
                                        "Список закладок", // 4
                                        "Подписки на книги", // 5
                                        "Подписки на авторов", // 6
                                        "Подписки на жанры", // 7
                                        "Подписки на серии", // 8
                                        "Фильтры книг",  // 9
                                        "Фильтры авторов",  // 10
                                        "Фильтры жанров", // 11
                                        "Фильтры серий", // 12
                                        "Фильтры форматов", // 13
                                        "Список книг для загрузки" // 14
                                    )
                                    builder.setMultiChoiceItems(
                                        backupOptions, checkResults
                                    ) { _, which, isChecked ->
                                        checkResults[which] = isChecked
                                    }
                                        .setTitle("Выберите элементы для резервирования")
                                        .setPositiveButton("OK") { _, _ ->
                                            (requireActivity() as SettingsActivity).viewModel.restore(
                                                file, checkResults
                                            )
                                            (requireActivity() as SettingsActivity).viewModel.livePrefsRestored.observe(
                                                viewLifecycleOwner,
                                                {
                                                    if (it) {
                                                        Toast.makeText(
                                                            requireContext(),
                                                            "Preferences restored, reboot app",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                        Handler().postDelayed(ResetApp(), 3000)
                                                    }
                                                })
                                        }
                                        .show()
                                }
                            }
                        }
                    }
                }
            }

        companion object {
            private const val BACKUP_FILE_REQUEST_CODE = 10
            private const val REQUEST_CODE = 1
            private const val READ_REQUEST_CODE = 2
        }
    }

    @Suppress("unused")
    class ConnectionPreferencesFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_connection, rootKey)
        }

        override fun onResume() {
            super.onResume()
            val useMirrorPref =
                findPreference<Preference>(getString(R.string.pref_use_custom_mirror))
            useMirrorPref?.setOnPreferenceChangeListener { _: Preference?, _: Any? ->
                Log.d(
                    "surprise",
                    "onResume: pref change! ${PreferencesHandler.instance.customMirror}"
                )
                if (PreferencesHandler.instance.customMirror == PreferencesHandler.BASE_URL) {
                    Toast.makeText(
                        context,
                        "Сначала нужно ввести адрес зеркала ниже",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnPreferenceChangeListener false
                }
                true
            }

            val mirrorAddress =
                findPreference<Preference>(getString(R.string.pref_custom_flibusta_mirror))
            mirrorAddress?.summary =
                if (PreferencesHandler.instance.customMirror == PreferencesHandler.BASE_URL) getString(
                    R.string.custom_mirror_hint
                ) else PreferencesHandler.instance.customMirror
            mirrorAddress?.setOnPreferenceChangeListener { _: Preference?, value: Any? ->
                val newValue = value as String
                if (newValue.isEmpty()) {
                    PreferencesHandler.instance.customMirror = PreferencesHandler.BASE_URL
                    PreferencesHandler.instance.isCustomMirror = false
                    mirrorAddress.summary = getString(R.string.custom_mirror_hint)
                    return@setOnPreferenceChangeListener false
                } else {
                    if (Grammar.isValidUrl(newValue)) {
                        mirrorAddress.summary = newValue
                    } else {
                        Toast.makeText(
                            context,
                            "$newValue - неверный формат Url. Введите ещё раз в формате http://flibusta.is",
                            Toast.LENGTH_LONG
                        ).show()
                        mirrorAddress.summary = getString(R.string.custom_mirror_hint)
                        return@setOnPreferenceChangeListener false
                    }
                }
                true
            }
            val picMirrorAddress =
                findPreference<Preference>(getString(R.string.pref_custom_pic_mirror))
            picMirrorAddress?.summary =
                if (PreferencesHandler.instance.picMirror == PreferencesHandler.BASE_PIC_URL) getString(
                    R.string.custom_mirror_hint
                ) else PreferencesHandler.instance.picMirror

            picMirrorAddress?.setOnPreferenceChangeListener { _: Preference?, value: Any? ->
                val newValue = value as String
                if (newValue.isEmpty()) {
                    PreferencesHandler.instance.customMirror = PreferencesHandler.BASE_PIC_URL
                    picMirrorAddress.summary = getString(R.string.custom_mirror_hint)
                    return@setOnPreferenceChangeListener false
                } else {
                    if (Grammar.isValidUrl(newValue)) {
                        picMirrorAddress.summary = newValue
                    } else {
                        Toast.makeText(
                            context,
                            "$newValue - неверный формат Url. Введите ещё раз в формате http://flibusta.is",
                            Toast.LENGTH_LONG
                        ).show()
                        picMirrorAddress.summary = getString(R.string.custom_mirror_hint)
                        return@setOnPreferenceChangeListener false
                    }
                }
                true
            }
        }
    }

    @Suppress("unused")
    class ViewPreferencesFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_view, rootKey)
        }

        override fun onResume() {
            super.onResume()
            Log.d("surprise", "ViewPreferencesFragment onCreate 270: create view preferences")
            val switchViewPref = findPreference<Preference>(getString(R.string.pref_is_eink))
            val switchNightModePref = findPreference<Preference>("night mode")
            if (switchViewPref != null) {
                switchViewPref.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { _: Preference?, _: Any? ->
                        Toast.makeText(
                            requireContext(),
                            requireContext().getString(R.string.app_restart_message),
                            Toast.LENGTH_LONG
                        ).show()
                        Handler().postDelayed(ResetApp(), 1000)
                        true
                    }
            }
            if (switchNightModePref != null) {
                switchNightModePref.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { _, _ ->
                        Toast.makeText(
                            requireContext(),
                            requireContext().getString(R.string.app_restart_message),
                            Toast.LENGTH_LONG
                        ).show()
                        Handler().postDelayed(ResetApp(), 1000)
                        true
                    }
            }
        }
    }

    @Suppress("unused")
    class UpdatePreferencesFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_update, rootKey)
        }
    }

    @Suppress("unused")
    class FilterPreferencesFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_filter, rootKey)
        }
    }

    @Suppress("unused")
    class DownloadPreferencesFragment : PreferenceFragmentCompat() {

        private var compatDirSelectResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    if (data != null && data.extras != null && data.extras!!.containsKey("data")) {
                        val folderLocation = data.extras!!.getString("data")
                        val file = File(folderLocation)
                        if (file.isDirectory && PreferencesHandler.instance.saveDownloadFolder(
                                folderLocation
                            )
                        ) {
                            Toast.makeText(requireContext(), "Папка сохранена!", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Не удалось сохранить папку, попробуйте ещё раз!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

        private var dirSelectResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    // There are no request codes
                    val data: Intent? = result.data
                    if (data != null) {
                        val treeUri = data.data
                        if (treeUri != null) {
                            // проверю наличие файла
                            val dl = DocumentFile.fromTreeUri(App.instance, treeUri)
                            if (dl != null && dl.isDirectory) {
                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                        App.instance.contentResolver.takePersistableUriPermission(
                                            treeUri,
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        )
                                        App.instance.contentResolver.takePersistableUriPermission(
                                            treeUri,
                                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                        )
                                    }
                                    PreferencesHandler.instance.setDownloadDir(dl)
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Не удалось выдать разрешения на доступ, попробуем другой метод",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    val intent = Intent(requireContext(), FolderPicker::class.java)
                                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                                        intent.addFlags(
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                                    or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                        )
                                    }
                                    compatDirSelectResultLauncher.launch(intent)
                                }
                            }
                        }
                    }
                }
            }

        private var mChangeDownloadFolderPreference: Preference? = null
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_download, rootKey)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            // получу ссылку на выбор папки загрузок
            mChangeDownloadFolderPreference =
                findPreference(getString(R.string.pref_download_location))
            if (mChangeDownloadFolderPreference != null) {
                // отображу текущую выбранную папку
                mChangeDownloadFolderPreference!!.summary =
                    "Текущая папка: " + PreferencesHandler.instance.getDownloadDirLocation()
                mChangeDownloadFolderPreference!!.onPreferenceClickListener =
                    Preference.OnPreferenceClickListener {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            var intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            intent.addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                        or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                        or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
                            )
                            if (TransportUtils.intentCanBeHandled(intent)) {
                                dirSelectResultLauncher.launch(intent)
                            } else {
                                intent = Intent(requireContext(), FolderPicker::class.java)
                                intent.addFlags(
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                            or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                )
                                compatDirSelectResultLauncher.launch(intent)
                            }
                        } else {
                            val intent = Intent(requireContext(), FolderPicker::class.java)
                            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                                intent.addFlags(
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                            or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                                )
                            }
                            compatDirSelectResultLauncher.launch(intent)
                        }
                        false
                    }
            }
            val changeDownloadFolderAltPreference =
                findPreference<Preference>(getString(R.string.pref_download_location_alt))
            if (changeDownloadFolderAltPreference != null) {
                changeDownloadFolderAltPreference.onPreferenceClickListener =
                    Preference.OnPreferenceClickListener {
                        showAlterDirSelectDialog()
                        false
                    }
            }
        }

        private fun showAlterDirSelectDialog() {
            Log.d("surprise", "SettingsActivity.java 331 showAlterDirSelectDialog: start select")
            val activity = activity
            if (activity != null) {
                AlertDialog.Builder(activity, R.style.MyDialogStyle)
                    .setTitle("Альтернативный выбор папки")
                    .setMessage("На случай, если папка для скачивания не выбирается основным методом. Только для совместимости, никаких преимуществ этот способ не даёт, также выбранная папка может сбрасываться при перезагрузке смартфона и её придётся выбирать заново")
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok) { _: DialogInterface?, _: Int ->
                        val intent = Intent(requireContext(), FolderPicker::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            intent.addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                        or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                            )
                        } else {
                            intent.addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )
                        }
                        compatDirSelectResultLauncher.launch(intent)
                    }
                    .create().show()
            }
        }

        companion object {
            private const val REQUEST_CODE = 10
            private const val READ_REQUEST_CODE = 11
        }
    }

    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}