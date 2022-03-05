package net.veldor.flibustaloader.ui.first_use_fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.databinding.FragmentFirstUseFinishGuideBinding
import net.veldor.flibustaloader.databinding.FragmentFirstUseIntroductionBinding
import net.veldor.flibustaloader.databinding.FragmentSelectConnectionTypeBinding
import net.veldor.flibustaloader.ui.ConnectivityGuideActivity
import net.veldor.flibustaloader.ui.IntroductionGuideActivity
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.view_models.ConnectivityGuideViewModel

class FirstUseFinishGuideFragment : Fragment() {


    private lateinit var binding: FragmentFirstUseFinishGuideBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFirstUseFinishGuideBinding.inflate(inflater, container, false)
        binding.toConnectionSettingsBtn.setOnClickListener {
            PreferencesHandler.instance.setFirstUse(false)
            goToNext()
        }
        return binding.root
    }

    private fun goToNext() {
        val targetActivityIntent = Intent(requireContext(), ConnectivityGuideActivity::class.java)
        targetActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(targetActivityIntent)
        requireActivity().finish()
    }

}