package net.veldor.flibustaloader.ui.connection_test_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.databinding.FragmentSelectConnectionTypeBinding
import net.veldor.flibustaloader.ui.ConnectivityGuideActivity
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.view_models.ConnectivityGuideViewModel

class SelectConnectionGuideFragment : Fragment() {

    private lateinit var binding: FragmentSelectConnectionTypeBinding
    private lateinit var viewModel: ConnectivityGuideViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(ConnectivityGuideViewModel::class.java)
        binding = FragmentSelectConnectionTypeBinding.inflate(inflater, container, false)

        if (PreferencesHandler.instance.isExternalVpn) {
            binding.selectGroup.check(R.id.use_vpn_button)
            binding.testStatusText.text = getString(R.string.use_vpn_info)
        } else {
            binding.selectGroup.check(R.id.use_tor_button)
            binding.testStatusText.text = getString(R.string.use_tor_info)
        }

        binding.selectGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.use_vpn_button) {
                PreferencesHandler.instance.isExternalVpn = true
                binding.testStatusText.text = getString(R.string.use_vpn_info)
            } else {
                PreferencesHandler.instance.isExternalVpn = false
                binding.testStatusText.text = getString(R.string.use_tor_info)
            }
        }

        binding.nextTestBtn.setOnClickListener {
            goToNext()
        }
        return binding.root
    }

    private fun goToNext() {
        val navController =
            Navigation.findNavController(
                requireActivity() as ConnectivityGuideActivity,
                R.id.nav_host_fragment
            )
        if(PreferencesHandler.instance.isExternalVpn){
            navController.navigate(R.id.test_vpn_connection_action)
        }
        else{
            navController.navigate(R.id.test_tor_connection_action)
        }
    }

}