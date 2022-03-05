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
import net.veldor.flibustaloader.databinding.FragmentTestInternetConnectionBinding
import net.veldor.flibustaloader.ui.ConnectivityGuideActivity
import net.veldor.flibustaloader.view_models.ConnectivityGuideViewModel

class TestInternetConnectionGuideFragment : Fragment() {

    private lateinit var binding: FragmentTestInternetConnectionBinding
    private lateinit var viewModel: ConnectivityGuideViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(ConnectivityGuideViewModel::class.java)
        binding = FragmentTestInternetConnectionBinding.inflate(inflater, container, false)
        binding.startTestBtn.setOnClickListener {
            binding.startTestBtn.isEnabled = false
            binding.checkProgress.visibility = View.VISIBLE
            viewModel.testInternetConnection()
        }
        binding.skipTestBtn.setOnClickListener {
            goToNext()
        }
        binding.nextTestBtn.setOnClickListener {
            goToNext()
        }
        binding.errorDescriptionBtn.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Не удалось проверить соединение с интернетом.\nПроверьте подключение к сети, попробуйте перезагрузить телефон.",
                Toast.LENGTH_LONG
            )
                .show()
        }
        setupObservers()
        return binding.root
    }

    private fun goToNext() {
        val navController =
            Navigation.findNavController(
                requireActivity() as ConnectivityGuideActivity,
                R.id.nav_host_fragment
            )
        navController.navigate(R.id.from_1_to_2_test_action)
    }

    private fun setupObservers() {
        viewModel.testConnectionState.observe(viewLifecycleOwner) {
            if (it.equals(ConnectivityGuideViewModel.STATE_PASSED)) {
                binding.checkProgress.visibility = View.INVISIBLE
                binding.startTestBtn.isEnabled = true
                binding.nextTestBtn.isEnabled = true
                binding.testStatusText.text = getString(R.string.check_passed)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    binding.testStatusText.setTextColor(resources.getColor(R.color.genre_text_color, null))
                }
                else{
                    binding.testStatusText.setTextColor(resources.getColor(R.color.genre_text_color))
                }
                binding.errorDescriptionBtn.visibility = View.GONE
            } else if (it.equals(ConnectivityGuideViewModel.STATE_FAILED)) {
                binding.startTestBtn.text = getString(R.string.try_again_message)
                binding.checkProgress.visibility = View.INVISIBLE
                binding.startTestBtn.isEnabled = true
                binding.testStatusText.text = getString(R.string.check_failed)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    binding.testStatusText.setTextColor(resources.getColor(R.color.book_name_color, null))
                }
                else{
                    binding.testStatusText.setTextColor(resources.getColor(R.color.book_name_color))
                }
                binding.errorDescriptionBtn.visibility = View.VISIBLE
            } else if (it.equals(ConnectivityGuideViewModel.STATE_WAIT)) {
                binding.startTestBtn.isEnabled = false
                binding.testStatusText.text = ""
                binding.errorDescriptionBtn.visibility = View.GONE
            }
        }
    }
}