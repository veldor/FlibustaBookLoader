package net.veldor.flibustaloader.ui.connection_test_fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.databinding.FragmentFinishConnectionTestBinding
import net.veldor.flibustaloader.databinding.FragmentTestTorConnectionBinding
import net.veldor.flibustaloader.dialogs.DonationDialog
import net.veldor.flibustaloader.ui.BrowserActivity
import net.veldor.flibustaloader.ui.ConnectivityGuideActivity
import net.veldor.flibustaloader.view_models.ConnectivityGuideViewModel

class TestFlibustaFinishedFragment : Fragment() {

    private lateinit var binding: FragmentFinishConnectionTestBinding
    private lateinit var viewModel: ConnectivityGuideViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(ConnectivityGuideViewModel::class.java)
        binding = FragmentFinishConnectionTestBinding.inflate(inflater, container, false)
        binding.donateBtn.setOnClickListener {
            DonationDialog.Builder(requireContext()).build().show()
        }
        binding.nextTestBtn.setOnClickListener {
            val targetActivityIntent = Intent(requireContext(), BrowserActivity::class.java)
            targetActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(targetActivityIntent)
        }
        return binding.root
    }
}