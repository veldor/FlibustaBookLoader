package net.veldor.flibustaloader.ui

import net.veldor.flibustaloader.utils.FilesHandler.shareFile
import net.veldor.flibustaloader.updater.Updater.checkUpdate
import net.veldor.flibustaloader.updater.Updater.update
import android.os.Bundle
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.App
import android.widget.Toast
import android.content.DialogInterface
import androidx.drawerlayout.widget.DrawerLayout
import android.os.Build
import androidx.core.view.GravityCompat
import android.content.Intent
import lib.folderpicker.FolderPicker
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.snackbar.Snackbar
import androidx.preference.PreferenceFragmentCompat
import net.veldor.flibustaloader.utils.TransportUtils
import net.veldor.flibustaloader.workers.RestoreSettingsWorker
import androidx.work.Operation.State.FAILURE
import net.veldor.flibustaloader.workers.ReserveSettingsWorker
import android.net.Uri
import android.os.Handler
import android.util.Log
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.work.*
import net.veldor.flibustaloader.utils.PreferencesHandler
import java.io.File

class SettingsActivity : BaseActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.new_preferences_activity)
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
                        // открою диалог выбора папки
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            startActivityForResult(
                                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE),
                                REQUEST_CODE
                            )
                        } else {
                            val intent = Intent(context, FolderPicker::class.java)
                            startActivityForResult(intent, READ_REQUEST_CODE)
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
                            startActivityForResult(intent, BACKUP_FILE_REQUEST_CODE)
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

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            if (requestCode == BACKUP_FILE_REQUEST_CODE) {
                // выбран файл, вероятно с бекапом
                if (resultCode == RESULT_OK) {
                    val uri: Uri?
                    if (data != null) {
                        uri = data.data
                        if (uri != null) {
                            // закодирую данные для передачи рабочему
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                App.instance.contentResolver.takePersistableUriPermission(
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                )
                            }
                            val inputData = Data.Builder()
                                .putString(RestoreSettingsWorker.URI, uri.toString())
                                .build()
                            val restoreWorker = OneTimeWorkRequest.Builder(
                                RestoreSettingsWorker::class.java
                            ).setInputData(inputData).build()
                            val workState = WorkManager.getInstance(App.instance)
                                .enqueue(restoreWorker).state
                            workState.observe(
                                this@ReservePreferencesFragment,
                                { state: Operation.State ->
                                    if (state.toString() == "SUCCESS") {
                                        // настройки приложения восстановлены
                                        Toast.makeText(
                                            context,
                                            "Настройки приложения восстановлены",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        workState.removeObservers(this@ReservePreferencesFragment)
                                        Handler().postDelayed(ResetApp(), 1000)
                                    } else if (state is FAILURE) {
                                        Toast.makeText(
                                            context,
                                            "Не удалось восстановить настройки приложения",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        workState.removeObservers(this@ReservePreferencesFragment)
                                    }
                                })
                        }
                    }
                }
            } else if (requestCode == REQUEST_CODE) {
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        val treeUri = data.data
                        if (treeUri != null) {
                            // проверю наличие файла
                            val dl = DocumentFile.fromTreeUri(App.instance, treeUri)
                            if (dl != null && dl.isDirectory) {
                                // зарезирвирую настройки в данную директорию
                                ReserveSettingsWorker.sSaveDir = dl
                                val reserveWorker = OneTimeWorkRequest.Builder(
                                    ReserveSettingsWorker::class.java
                                ).build()
                                WorkManager.getInstance(App.instance).enqueue(reserveWorker)
                                val workStatus = WorkManager.getInstance(App.instance)
                                    .getWorkInfoByIdLiveData(reserveWorker.id)
                                // Отслежу резервирование настроек, когда оно будет закончено- предложу отправить файл с настройками
                                workStatus.observe(
                                    this@ReservePreferencesFragment,
                                    { state: WorkInfo ->
                                        Log.d(
                                            "surprise",
                                            "ReservePreferencesFragment onActivityResult 174: state is $state"
                                        )
                                        if (state.state == WorkInfo.State.SUCCEEDED) {
                                            Log.d(
                                                "surprise",
                                                "ReservePreferencesFragment onActivityResult 173: here"
                                            )
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                                // отправка файла
                                                val zip = ReserveSettingsWorker.sBackupFile
                                                Log.d(
                                                    "surprise",
                                                    "ReservePreferencesFragment onActivityResult 177: zip is $zip"
                                                )
                                                if (zip != null && zip.isFile) {
                                                    Toast.makeText(
                                                        context,
                                                        "Настройки сохранены. Вот файл с ними",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    shareFile(zip)
                                                }
                                            }
                                            workStatus.removeObservers(this@ReservePreferencesFragment)
                                        }
                                    })
                            }
                        }
                    }
                }
            } else if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK) {
                // сохраню файл
                if (data != null && data.extras != null) {
                    val folderLocation = data.extras!!.getString("data")
                    if (folderLocation != null) {
                        val destination = File(folderLocation)
                        if (destination.exists()) {
                            ReserveSettingsWorker.sCompatSaveDir = destination
                            val reserveWorker = OneTimeWorkRequest.Builder(
                                ReserveSettingsWorker::class.java
                            ).build()
                            val workState = WorkManager.getInstance(App.instance)
                                .enqueue(reserveWorker).state
                            // Отслежу резервирование настроек, когда оно будет закончено- предложу отправить файл с настройками
                            workState.observe(
                                this@ReservePreferencesFragment,
                                { state: Operation.State ->
                                    if (state.toString() == "SUCCESS") {
                                        Log.d(
                                            "surprise",
                                            "ReservePreferencesFragment onActivityResult 173: here"
                                        )
                                        // отправка файла
                                        val zip = ReserveSettingsWorker.sCompatBackupFile
                                        Log.d(
                                            "surprise",
                                            "ReservePreferencesFragment onActivityResult 177: zip is $zip"
                                        )
                                        if (zip != null && zip.isFile) {
                                            Toast.makeText(
                                                context,
                                                "Настройки сохранены. Вот файл с ними",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            shareFile(zip)
                                        }
                                        workState.removeObservers(this@ReservePreferencesFragment)
                                    }
                                })
                        }
                    }
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }

        companion object {
            private const val BACKUP_FILE_REQUEST_CODE = 10
            private const val REQUEST_CODE = 1
            private const val READ_REQUEST_CODE = 2
        }
    }

    class ConnectionPreferencesFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_connection, rootKey)
        }

        override fun onResume() {
            super.onResume()
            val useMirrorPref =
                findPreference<Preference>(getString(R.string.pref_use_custom_mirror))
            useMirrorPref?.summary =
                if (PreferencesHandler.instance.isCustomMirror) getString(R.string.used_title) else getString(
                    R.string.disabled_title
                )
            useMirrorPref?.setOnPreferenceChangeListener { _: Preference?, _: Any? ->
                useMirrorPref.summary =
                    if (PreferencesHandler.instance.isCustomMirror) getString(R.string.disabled_title) else getString(
                        R.string.used_title
                    )
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
                if(newValue.isEmpty()){
                    PreferencesHandler.instance.customMirror = null
                    mirrorAddress.summary = getString(R.string.custom_mirror_hint)
                    return@setOnPreferenceChangeListener false
                }
                else{
                    mirrorAddress.summary = newValue
                }
                true
            }
        }
    }

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

    class UpdatePreferencesFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_update, rootKey)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val checkUpdatePref =
                findPreference<Preference>(getString(R.string.pref_check_update_now))
            if (checkUpdatePref != null) {
                checkUpdatePref.onPreferenceClickListener =
                    Preference.OnPreferenceClickListener {
                        Toast.makeText(context, "Проверяю обновления", Toast.LENGTH_SHORT).show()
                        val updateCheckStatus = checkUpdate()
                        updateCheckStatus.observe(
                            this@UpdatePreferencesFragment,
                            { aBoolean: Boolean? ->
                                if (aBoolean != null && aBoolean) {
                                    // показываю Snackbar с уведомлением
                                    makeUpdateSnackbar()
                                    updateCheckStatus.removeObservers(this@UpdatePreferencesFragment)
                                }
                            })
                        false
                    }
            }
        }

        private fun makeUpdateSnackbar() {
            val view = view
            if (view != null) {
                val updateSnackbar = Snackbar.make(
                    requireView(),
                    getString(R.string.snackbar_found_update_message),
                    Snackbar.LENGTH_INDEFINITE
                )
                updateSnackbar.setAction(getString(R.string.snackbar_update_action_message)) {
                    Toast.makeText(context, "Загружаю обновление", Toast.LENGTH_SHORT).show()
                    update()
                }
                updateSnackbar.setActionTextColor(resources.getColor(android.R.color.white))
                updateSnackbar.show()
            }
        }
    }

    class DownloadPreferencesFragment : PreferenceFragmentCompat() {
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
                        // открою диалог выбора папки
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            startActivityForResult(
                                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE),
                                REQUEST_CODE
                            )
                        } else {
                            val intent = Intent(context, FolderPicker::class.java)
                            startActivityForResult(intent, READ_REQUEST_CODE)
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
                        val intent = Intent(context, FolderPicker::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            intent.addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                        or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                            )
                        }
                        startActivityForResult(intent, READ_REQUEST_CODE)
                    }
                    .create().show()
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            if (requestCode == REQUEST_CODE) {
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        val treeUri = data.data
                        if (treeUri != null) {
                            // проверю наличие файла
                            val dl = DocumentFile.fromTreeUri(App.instance, treeUri)
                            if (dl != null && dl.isDirectory) {
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
                                PreferencesHandler.instance.downloadDir = dl
                                Toast.makeText(
                                    context,
                                    getText(R.string.download_folder_changed_message_new),
                                    Toast.LENGTH_LONG
                                ).show()
                                mChangeDownloadFolderPreference!!.summary =
                                    "Текущая папка: " + PreferencesHandler.instance
                                        .getDownloadDirLocation()
                            }
                        }
                    }
                }
            } else if (requestCode == READ_REQUEST_CODE) {
                if (resultCode == RESULT_OK) {
                    if (data != null && data.extras != null) {
                        val folderLocation = data.extras!!.getString("data")
                        if (folderLocation != null) {
                            val destination = File(folderLocation)
                            if (destination.exists()) {
                                PreferencesHandler.instance.compatDownloadDir = destination
                                Toast.makeText(
                                    context,
                                    getText(R.string.download_folder_changed_message).toString() + folderLocation,
                                    Toast.LENGTH_LONG
                                ).show()
                                mChangeDownloadFolderPreference!!.summary =
                                    "Текущая папка: " + PreferencesHandler.instance.getDownloadDirLocation()
                            }
                        }
                    }
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data)
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