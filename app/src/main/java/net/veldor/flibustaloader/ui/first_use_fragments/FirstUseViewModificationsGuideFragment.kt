package net.veldor.flibustaloader.ui.first_use_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.databinding.FragmentFirstUseViewGuideBinding
import net.veldor.flibustaloader.ui.IntroductionGuideActivity
import net.veldor.flibustaloader.utils.PreferencesHandler

class FirstUseViewModificationsGuideFragment : Fragment() {


    private lateinit var binding: FragmentFirstUseViewGuideBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFirstUseViewGuideBinding.inflate(inflater, container, false)
        binding.nextTestBtn.setOnClickListener {
            goToNext()
        }

        binding.useHardwareAccelerationSwitcher.isChecked =
            PreferencesHandler.instance.hardwareAcceleration
        binding.isEbook.isChecked = PreferencesHandler.instance.isEInk
        binding.isNightMode.isChecked = PreferencesHandler.instance.nightMode
        binding.useHardwareAccelerationSwitcher.setOnCheckedChangeListener { _, isChecked ->
            PreferencesHandler.instance.hardwareAcceleration = isChecked
        }
        binding.isEbook.setOnCheckedChangeListener { _, isChecked ->
            PreferencesHandler.instance.setEInk(isChecked)
        }
        binding.isNightMode.setOnCheckedChangeListener { _, isChecked ->
            PreferencesHandler.instance.nightMode = isChecked
        }
        return binding.root
    }

    private fun goToNext() {
        val navController =
            Navigation.findNavController(
                requireActivity() as IntroductionGuideActivity,
                R.id.nav_host_fragment
            )
        navController.navigate(R.id.go_to_finish_first_setup_action)
    }

}