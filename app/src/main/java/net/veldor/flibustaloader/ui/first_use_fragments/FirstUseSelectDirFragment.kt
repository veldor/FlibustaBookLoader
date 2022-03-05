package net.veldor.flibustaloader.ui.first_use_fragments

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import lib.folderpicker.FolderPicker
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.databinding.FragmentFirstUseSelectDirBinding
import net.veldor.flibustaloader.ui.IntroductionGuideActivity
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.utils.TransportUtils
import java.io.File

class FirstUseSelectDirFragment : Fragment() {

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
                        Toast.makeText(
                            requireContext(),
                            "Папка сохранена! В дальнейшем вы можете изменить её в настройках приложения",
                            Toast.LENGTH_SHORT
                        ).show()
                        goToNext()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Не удалось сохранить папку, попробуйте ещё раз!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Для продолжения использования приложения необходимо выбрать папку для хранения книг",
                    Toast.LENGTH_SHORT
                ).show()
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
                                if(PreferencesHandler.instance.setDownloadDir(dl)){
                                    goToNext()
                                }
                                else{
                                    Toast.makeText(
                                        requireContext(),
                                        "Не удалось сохранить папку, попробуйте ещё раз!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
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
            } else {
                Toast.makeText(
                    requireContext(),
                    "Для продолжения использования приложения необходимо выбрать папку для хранения книг",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private lateinit var binding: FragmentFirstUseSelectDirBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFirstUseSelectDirBinding.inflate(inflater, container, false)

        binding.selectDirBtn.setOnClickListener {
            // first option, try to select dir by typical way
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
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.useDefaultDirBtn.visibility = View.GONE
            binding.orTextView.visibility = View.GONE
        }

        binding.useDefaultDirBtn.setOnClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                val file = File(Environment.DIRECTORY_DOWNLOADS)
                if (file.isDirectory && file.canWrite()) {
                    PreferencesHandler.instance.saveDownloadFolder(
                        file.absolutePath
                    )
                    goToNext()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Не удалось назначить папку по умолчанию, выберите папку вручную.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        return binding.root
    }

    private fun goToNext() {
        Toast.makeText(requireContext(), "Папка сохранена", Toast.LENGTH_SHORT).show()
        val navController =
            Navigation.findNavController(
                requireActivity() as IntroductionGuideActivity,
                R.id.nav_host_fragment
            )
            navController.navigate(R.id.go_to_view_settings_guide)
    }
}