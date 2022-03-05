package net.veldor.flibustaloader.ui.connection_test_fragments

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.google.android.material.textfield.TextInputEditText
import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.databinding.FragmentTestTorConnectionBinding
import net.veldor.flibustaloader.ui.ConnectivityGuideActivity
import net.veldor.flibustaloader.utils.FilesHandler
import net.veldor.flibustaloader.view_models.ConnectivityGuideViewModel
import java.util.*

class TestTorConnectionFragment : Fragment() {

    private var mTorFixDialog: AlertDialog? = null
    private lateinit var binding: FragmentTestTorConnectionBinding
    private lateinit var viewModel: ConnectivityGuideViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(ConnectivityGuideViewModel::class.java)
        binding = FragmentTestTorConnectionBinding.inflate(inflater, container, false)
        binding.launchTorBtn.setOnClickListener {
            binding.torFixActions.visibility = View.GONE
            viewModel.initTor()

        }
        binding.backBtn.setOnClickListener {
            val navController =
                Navigation.findNavController(
                    requireActivity() as ConnectivityGuideActivity,
                    R.id.nav_host_fragment
                )
            navController.navigateUp()
        }
        binding.nextTestBtn.setOnClickListener {
            val navController =
                Navigation.findNavController(
                    requireActivity() as ConnectivityGuideActivity,
                    R.id.nav_host_fragment
                )
            navController.navigate(R.id.check_flibusta_via_tor_action)
        }
        binding.cancelLaunchTorBtn.setOnClickListener {
            viewModel.cancelTorLaunch()
            binding.launchTorBtn.isEnabled = true
            binding.launchTorBtn.text = getString(R.string.launch_message)
            binding.checkProgress.visibility = View.INVISIBLE
            binding.cancelLaunchTorBtn.visibility = View.INVISIBLE
            binding.cancelLaunchTorBtn.visibility = View.GONE
            binding.testStatusText.text = "Отменено"
        }

        binding.torFixActions.setOnClickListener {
            val view = layoutInflater.inflate(R.layout.dialog_tor_fix_options, null, false)
            view.findViewById<Button>(R.id.dropTorCache).setOnClickListener {
                FilesHandler.dropTorCache()
                Toast.makeText(requireContext(), getString(R.string.tor_cache_dropped_message), Toast.LENGTH_LONG).show()
                mTorFixDialog?.dismiss()
            }
            view.findViewById<Button>(R.id.downloadTorBridges).setOnClickListener {
                Toast.makeText(requireContext(), getString(R.string.start_load_bridges_title), Toast.LENGTH_LONG).show()
                viewModel.loadTorBridges()

                mTorFixDialog?.dismiss()
            }
            view.findViewById<Button>(R.id.useCustomTorBridges).setOnClickListener {
                showCustomBridgesDialog()
                mTorFixDialog?.dismiss()
            }
            mTorFixDialog = AlertDialog.Builder(requireContext())
                .setView(view)
                .setTitle(getString(R.string.tor_fix_options_title))
                .create()
            mTorFixDialog?.show()
        }
        setupObservers()
        return binding.root
    }

    private fun showCustomBridgesDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_tor_custom_gridges, null, false)
        AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle(getString(R.string.tor_fix_options_title))
            .setPositiveButton("Save") { _, _ ->
                viewModel.saveBridges(view.findViewById<TextInputEditText>(R.id.bridgesInput).text)
                Toast.makeText(requireContext(), getString(R.string.custom_bridges_saved_message), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    private fun finishTest() {
        // test successful
    }

    private fun setupObservers() {
        viewModel.testTorInit.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it == ConnectivityGuideViewModel.STATE_PASSED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        binding.testStatusText.setTextColor(resources.getColor(R.color.genre_text_color, null))
                    }
                    else{
                        binding.testStatusText.setTextColor(resources.getColor(R.color.genre_text_color))
                    }
                    binding.checkProgress.visibility = View.GONE
                    binding.launchTorBtn.isEnabled = true
                    binding.torFixActions.isEnabled = true
                    binding.nextTestBtn.isEnabled = true
                    binding.launchTorBtn.text = getString(R.string.launched_message)
                    binding.testStatusText.text = getString(R.string.check_passed)
                    binding.cancelLaunchTorBtn.visibility = View.GONE
                }
                else if (it == ConnectivityGuideViewModel.STATE_FAILED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        binding.testStatusText.setTextColor(resources.getColor(R.color.book_name_color, null))
                    }
                    else{
                        binding.testStatusText.setTextColor(resources.getColor(R.color.book_name_color))
                    }
                    binding.torFixActions.isEnabled = true
                    binding.launchTorBtn.text = getString(R.string.try_again_message)
                    binding.launchTorBtn.isEnabled = true
                    binding.cancelLaunchTorBtn.visibility = View.GONE
                    binding.checkProgress.visibility = View.GONE
                    binding.torFixActions.visibility = View.VISIBLE
                    binding.testStatusText.text = String.format(Locale.ENGLISH, getString(R.string.fail_tor_message), App.instance.torException?.message)
                    binding.errorDescriptionBtn.visibility = View.VISIBLE
                    binding.errorDescriptionBtn.setOnClickListener {
                        Toast.makeText(
                            requireContext(),
                            "Не удалось запустить встроенный клиент. Возможно, у вас проблемы с сетью\nВозможно, проблемы с самим клиентом TOR\nВы можете попробовать ещё раз, выбрать одно из действий для решения проблемы ниже, или использовать режим VPN",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                } else if (it == ConnectivityGuideViewModel.STATE_CHECK_ERROR) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        binding.testStatusText.setTextColor(resources.getColor(R.color.book_name_color, null))
                    }
                    else{
                        binding.testStatusText.setTextColor(resources.getColor(R.color.book_name_color))
                    }
                    binding.launchTorBtn.text = getString(R.string.try_again_message)
                    binding.launchTorBtn.isEnabled = true
                    binding.checkProgress.visibility = View.GONE
                    binding.testStatusText.text = getString(R.string.check_failed)
                    binding.errorDescriptionBtn.visibility = View.VISIBLE
                    binding.torFixActions.isEnabled = true
                    binding.errorDescriptionBtn.setOnClickListener {
                        Toast.makeText(
                            requireContext(),
                            "Не удалось запустить встроенный клиент. Возможно, у вас проблемы с сетью\nВозможно, проблемы с самим клиентом TOR\nВы можете попробовать ещё раз, выбрать одно из действий для решения проблемы ниже, или использовать режим VPN",
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                } else if (it == ConnectivityGuideViewModel.STATE_WAIT) {
                    binding.launchTorBtn.isEnabled = false
                    binding.testStatusText.text = "Инициализация"
                    binding.checkProgress.visibility = View.VISIBLE
                    binding.launchTorBtn.text = getString(R.string.in_progress_message)
                    binding.cancelLaunchTorBtn.visibility = View.VISIBLE
                    binding.torFixActions.isEnabled = false
                } else {
                    binding.testStatusText.text = it
                }
            }
        }
    }
}