package net.veldor.flibustaloader.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.LiveData;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.WorkManager;

import com.google.android.material.snackbar.Snackbar;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.R;
import net.veldor.flibustaloader.updater.Updater;
import net.veldor.flibustaloader.utils.FilesHandler;
import net.veldor.flibustaloader.utils.MyPreferences;
import net.veldor.flibustaloader.utils.TransportUtils;
import net.veldor.flibustaloader.workers.ReserveSettingsWorker;
import net.veldor.flibustaloader.workers.RestoreSettingsWorker;

import java.io.File;

import lib.folderpicker.FolderPicker;

public class SettingsActivity extends BaseActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.new_preferences_activity);

        setupInterface();

        // добавлю главный фрагмент
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
    }

    @Override
    protected void setupInterface() {
        super.setupInterface();
        // скрою переход на данное активити
        Menu menuNav = mNavigationView.getMenu();
        MenuItem item = menuNav.findItem(R.id.goToSettings);
        item.setEnabled(false);
        item.setChecked(true);
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        return false;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_root, rootKey);
        }
    }

    public static class ReservePreferencesFragment extends PreferenceFragmentCompat {

        private static final int BACKUP_FILE_REQUEST_CODE = 10;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_reserve, rootKey);
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // получу ссылку на резервирование настроек
            Preference settingsBackupPref = findPreference(getString(R.string.backup_settings));
            Preference settingsRestorePref = findPreference(getString(R.string.restore_settings));
            if (settingsBackupPref != null) {
                settingsBackupPref.setOnPreferenceClickListener(preference -> {
                    // запущу рабочего, сохраняющего базу данных прочитанного и скачанного в XML
                    Toast.makeText(getContext(), "Запущено резервирование настроек", Toast.LENGTH_SHORT).show();
                    OneTimeWorkRequest reserveWorker = new OneTimeWorkRequest.Builder(ReserveSettingsWorker.class).build();
                    LiveData<Operation.State> workState = WorkManager.getInstance(App.getInstance()).enqueue(reserveWorker).getState();
                    // Отслежу резервирование настроек, когда оно будет закончено- предложу отправить файл с настройками
                    workState.observe(ReservePreferencesFragment.this, state -> {
                        if (state.toString().equals("SUCCESS")) {
                            // отправка файла
                            File zip = new File(new File(Environment.getExternalStorageDirectory(), App.BACKUP_DIR_NAME), App.BACKUP_FILE_NAME);
                            if (zip.isFile()) {
                                Toast.makeText(getContext(), "Настройки сохранены. Вот файл с ними", Toast.LENGTH_SHORT).show();
                                FilesHandler.shareFile(zip);
                            }
                            workState.removeObservers(ReservePreferencesFragment.this);
                        }
                    });
                    return false;
                });
            }
            if (settingsRestorePref != null) {
                settingsRestorePref.setOnPreferenceClickListener(preference -> {
                    Toast.makeText(getContext(), "Выберите сохранённый ранее файл с настройками.", Toast.LENGTH_LONG).show();
                    // открою окно выбота файла для восстановления
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("application/zip");
                    if (TransportUtils.intentCanBeHandled(intent)) {
                        Toast.makeText(getContext(), "Восстанавливаю настройки.", Toast.LENGTH_LONG).show();
                        startActivityForResult(intent, BACKUP_FILE_REQUEST_CODE);
                    } else {
                        Toast.makeText(getContext(), "Упс, не нашлось приложения, которое могло бы это сделать.", Toast.LENGTH_LONG).show();
                    }
                    return false;
                });
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            if (requestCode == BACKUP_FILE_REQUEST_CODE) {
                // выбран файл, вероятно с бекапом
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri;
                    if (data != null) {
                        uri = data.getData();
                        if (uri != null) {
                            Data inputData = new Data.Builder()
                                    .putString(RestoreSettingsWorker.URI, uri.toString())
                                    .build();
                            OneTimeWorkRequest restoreWorker = new OneTimeWorkRequest.Builder(RestoreSettingsWorker.class).setInputData(inputData).build();
                            LiveData<Operation.State> workState = WorkManager.getInstance(App.getInstance()).enqueue(restoreWorker).getState();
                            workState.observe(ReservePreferencesFragment.this, state -> {
                                if (state.toString().equals("SUCCESS")) {
                                    // отправка файла
                                    File zip = new File(new File(Environment.getExternalStorageDirectory(), App.BACKUP_DIR_NAME), App.BACKUP_FILE_NAME);
                                    if (zip.isFile()) {
                                        Toast.makeText(getContext(), "Настройки приложения восстановлены", Toast.LENGTH_SHORT).show();
                                        FilesHandler.shareFile(zip);
                                    }
                                    workState.removeObservers(ReservePreferencesFragment.this);
                                }
                            });
                        }
                    }
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    public static class ConnectionPreferencesFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_connection, rootKey);
        }
    }

    public static class UpdatePreferencesFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_update, rootKey);
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Preference checkUpdatePref = findPreference(getString(R.string.pref_check_update_now));
            if (checkUpdatePref != null) {
                checkUpdatePref.setOnPreferenceClickListener(preference -> {
                    Toast.makeText(getContext(), "Проверяю обновления", Toast.LENGTH_SHORT).show();
                    LiveData<Boolean> updateCheckStatus = Updater.checkUpdate();
                    updateCheckStatus.observe(UpdatePreferencesFragment.this, aBoolean -> {
                        if (aBoolean != null && aBoolean) {
                            // показываю Snackbar с уведомлением
                            makeUpdateSnackbar();
                            updateCheckStatus.removeObservers(UpdatePreferencesFragment.this);
                        }
                    });
                    return false;
                });
            }

        }

        private void makeUpdateSnackbar() {
            View view = getView();
            if (view != null) {
                Snackbar updateSnackbar = Snackbar.make(getView(), getString(R.string.snackbar_found_update_message), Snackbar.LENGTH_INDEFINITE);
                updateSnackbar.setAction(getString(R.string.snackbar_update_action_message), v -> {
                    Toast.makeText(getContext(), "Загружаю обновление", Toast.LENGTH_SHORT).show();
                    Updater.update();
                });
                updateSnackbar.setActionTextColor(getResources().getColor(android.R.color.white));
                updateSnackbar.show();
            }
        }
    }

    public static class DownloadPreferencesFragment extends PreferenceFragmentCompat {

        private static final int REQUEST_CODE = 10;
        private static final int READ_REQUEST_CODE = 11;
        private Preference mChangeDownloadFolderPreference;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences_download, rootKey);
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // получу ссылку на выбор папки загрузок
            mChangeDownloadFolderPreference = findPreference(getString(R.string.pref_download_location));
            if (mChangeDownloadFolderPreference != null) {
                // отображу текущую выбранную папку
                mChangeDownloadFolderPreference.setSummary("Текущая папка: " + MyPreferences.getInstance().getDownloadDirLocation());
                mChangeDownloadFolderPreference.setOnPreferenceClickListener(preference -> {
                    // открою диалог выбора папки
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_CODE);
                    } else {
                        Intent intent = new Intent(getContext(), FolderPicker.class);
                        startActivityForResult(intent, READ_REQUEST_CODE);
                    }
                    return false;
                });
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            if (requestCode == REQUEST_CODE) {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        Uri treeUri = data.getData();
                        if (treeUri != null) {
                            // проверю наличие файла
                            DocumentFile dl = DocumentFile.fromTreeUri(App.getInstance(), treeUri);
                            if (dl != null && dl.isDirectory()) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                    App.getInstance().getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    App.getInstance().getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                }
                                App.getInstance().setDownloadDir(treeUri);
                                Toast.makeText(getContext(), getText(R.string.download_folder_changed_message_new), Toast.LENGTH_LONG).show();
                                mChangeDownloadFolderPreference.setSummary("Текущая папка: " + MyPreferences.getInstance().getDownloadDirLocation());
                            }
                        }
                    }
                }
            } else if (requestCode == READ_REQUEST_CODE) {
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null && data.getExtras() != null) {
                        String folderLocation = data.getExtras().getString("data");
                        if (folderLocation != null) {
                            File destination = new File(folderLocation);
                            if (destination.exists()) {
                                App.getInstance().setDownloadDir(Uri.parse(folderLocation));
                                Toast.makeText(getContext(), getText(R.string.download_folder_changed_message) + folderLocation, Toast.LENGTH_LONG).show();
                                mChangeDownloadFolderPreference.setSummary("Текущая папка: " + MyPreferences.getInstance().getDownloadDirLocation());
                            }
                        }
                    }
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
