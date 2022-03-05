package net.veldor.flibustaloader.ui.first_use_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.databinding.FragmentFirstUseIntroductionBinding
import net.veldor.flibustaloader.databinding.FragmentSelectConnectionTypeBinding
import net.veldor.flibustaloader.ui.ConnectivityGuideActivity
import net.veldor.flibustaloader.ui.IntroductionGuideActivity
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.view_models.ConnectivityGuideViewModel

class FirstUseIntroductionFragment : Fragment() {


    private lateinit var binding: FragmentFirstUseIntroductionBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFirstUseIntroductionBinding.inflate(inflater, container, false)
        binding.nextTestBtn.setOnClickListener {
            goToNext()
        }
        return binding.root
    }

    private fun goToNext() {
        val navController =
            Navigation.findNavController(
                requireActivity() as IntroductionGuideActivity,
                R.id.nav_host_fragment
            )
        navController.navigate(R.id.go_to_permissions_action)
    }

}