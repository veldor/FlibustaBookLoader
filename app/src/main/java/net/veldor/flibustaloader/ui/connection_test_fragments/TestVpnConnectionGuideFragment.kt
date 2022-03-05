package net.veldor.flibustaloader.ui.connection_test_fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.databinding.FragmentTestVpnConnectionBinding
import net.veldor.flibustaloader.ui.ConnectivityGuideActivity
import net.veldor.flibustaloader.view_models.ConnectivityGuideViewModel

class TestVpnConnectionGuideFragment : Fragment() {

    private lateinit var binding: FragmentTestVpnConnectionBinding
    private lateinit var viewModel: ConnectivityGuideViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(ConnectivityGuideViewModel::class.java)
        binding = FragmentTestVpnConnectionBinding.inflate(inflater, container, false)
        binding.startTestBtn.setOnClickListener {
            binding.startTestBtn.isEnabled = false
            binding.checkProgress.visibility = View.VISIBLE
            viewModel.finallyTestConnection(binding.customMirrorInput.text)
        }
        binding.nextTestBtn.setOnClickListener {
            finishTest()
        }
        setupObservers()
        return binding.root
    }

    private fun finishTest() {
        val navController =
            Navigation.findNavController(
                requireActivity() as ConnectivityGuideActivity,
                R.id.nav_host_fragment
            )
        navController.navigate(R.id.finish_test_action)
    }

    private fun setupObservers() {

        viewModel.libraryConnectionState.observe(viewLifecycleOwner) {
            if (it != null) {
                when (it) {
                    ConnectivityGuideViewModel.STATE_PASSED -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            binding.testStatusText.setTextColor(
                                resources.getColor(
                                    R.color.genre_text_color,
                                    null
                                )
                            )
                        } else {
                            binding.testStatusText.setTextColor(resources.getColor(R.color.genre_text_color))
                        }
                        binding.checkProgress.visibility = View.INVISIBLE
                        binding.startTestBtn.isEnabled = true
                        binding.nextTestBtn.isEnabled = true
                        binding.testStatusText.text = getString(R.string.check_passed)
                        binding.nextTestBtn.isEnabled = true
                    }
                    ConnectivityGuideViewModel.STATE_FAILED -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            binding.testStatusText.setTextColor(
                                resources.getColor(
                                    R.color.book_name_color,
                                    null
                                )
                            )
                        } else {
                            binding.testStatusText.setTextColor(resources.getColor(R.color.book_name_color))
                        }
                        binding.startTestBtn.text = getString(R.string.try_again_message)
                        binding.startTestBtn.isEnabled = true
                        binding.checkProgress.visibility = View.INVISIBLE
                        binding.testStatusText.text = getString(R.string.check_failed)
                        binding.errorDescriptionBtn.visibility = View.VISIBLE
                        binding.errorDescriptionBtn.setOnClickListener {
                            Toast.makeText(
                                requireContext(),
                                "Не удалось соединиться с библиотекой\nПроверьте, активно ли VPN соединение",
                                Toast.LENGTH_LONG
                            )
                                .show()
                        }
                    }
                    ConnectivityGuideViewModel.STATE_CHECK_ERROR -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            binding.testStatusText.setTextColor(
                                resources.getColor(
                                    R.color.book_name_color,
                                    null
                                )
                            )
                        } else {
                            binding.testStatusText.setTextColor(resources.getColor(R.color.book_name_color))
                        }
                        binding.startTestBtn.text = getString(R.string.try_again_message)
                        binding.startTestBtn.isEnabled = true
                        binding.checkProgress.visibility = View.INVISIBLE
                        binding.testStatusText.text = getString(R.string.check_failed)
                        binding.errorDescriptionBtn.visibility = View.VISIBLE
                        binding.errorDescriptionBtn.setOnClickListener {
                            Toast.makeText(
                                requireContext(),
                                "Не удалось соединиться с библиотекой\n",
                                Toast.LENGTH_LONG
                            )
                                .show()
                        }
                    }
                    ConnectivityGuideViewModel.STATE_WAIT -> {
                        binding.startTestBtn.isEnabled = false
                        binding.testStatusText.text = "Инициализация"
                        binding.checkProgress.visibility = View.VISIBLE
                        binding.startTestBtn.text = getString(R.string.in_progress_message)
                    }
                    else -> {
                        binding.testStatusText.text = it
                    }
                }
            }
        }
    }
}