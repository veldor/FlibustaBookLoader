package net.veldor.flibustaloader.ui.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.adapters.SubscribeResultsAdapter
import net.veldor.flibustaloader.databinding.FragmentSubscribeResultsBinding
import net.veldor.flibustaloader.delegates.FoundedItemActionDelegate
import net.veldor.flibustaloader.selections.DownloadLink
import net.veldor.flibustaloader.selections.FoundedEntity
import net.veldor.flibustaloader.utils.MimeTypes
import net.veldor.flibustaloader.utils.PreferencesHandler
import net.veldor.flibustaloader.view_models.SubscriptionsViewModel
import java.util.*

class SubscriptionsResultFragment : Fragment(), FoundedItemActionDelegate {
    private lateinit var binding: FragmentSubscribeResultsBinding
    private lateinit var viewModel: SubscriptionsViewModel
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSubscribeResultsBinding.inflate(layoutInflater, container, false)
        viewModel = ViewModelProvider(this).get(SubscriptionsViewModel::class.java)
        setupUI()
        addObservers()
        return binding.root
    }

    private fun addObservers() {
        SubscriptionsViewModel.liveCheckInProgress.observe(viewLifecycleOwner, {
            binding.totalCheckButton.isEnabled = !it
            binding.fastCheckButton.isEnabled = !it
            binding.waiter.visibility = if (it) View.VISIBLE else View.GONE
        })
        viewModel.liveFoundedSubscription.observe(viewLifecycleOwner, {
            if (it != null) {
                (binding.resultsList.adapter as SubscribeResultsAdapter).bookFound(it)
            }
        })
    }

    private fun setupUI() {
        binding.resultsList.layoutManager = LinearLayoutManager(context)
        binding.resultsList.adapter =
            SubscribeResultsAdapter(SubscriptionsViewModel.foundedSubscribes.value!!, this)

        binding.totalCheckButton.setOnClickListener {
            (binding.resultsList.adapter as SubscribeResultsAdapter).clear()
            viewModel.fullCheckSubscribes()
        }
        binding.fastCheckButton.setOnClickListener {
            (binding.resultsList.adapter as SubscribeResultsAdapter).clear()
            viewModel.checkSubscribes()
        }
    }

    override fun buttonPressed(item: FoundedEntity) {
        // обработаю загрузку
        if (item.downloadLinks.size > 1) {
            val savedMime: String? = PreferencesHandler.instance.favoriteMime
            if (!savedMime.isNullOrEmpty()) {
                // проверю, нет ли в списке выбранного формата
                item.downloadLinks.forEach {
                    if (it.mime!!.contains(savedMime)) {
                        viewModel.addToDownloadQueue(it)
                        return
                    }
                }
            }
            showDownloadsDialog(item.downloadLinks)

        }
        viewModel.addToDownloadQueue(item.downloadLinks[0])
    }

    override fun imageClicked(item: FoundedEntity) {

    }

    override fun itemPressed(item: FoundedEntity) {

    }

    override fun buttonLongPressed(item: FoundedEntity) {

    }

    override fun itemLongPressed(item: FoundedEntity) {

    }

    override fun menuItemPressed(item: FoundedEntity, button: View) {

    }

    override fun loadMoreBtnClicked() {

    }

    override fun authorClicked(item: FoundedEntity) {

    }

    override fun sequenceClicked(item: FoundedEntity) {

    }

    override fun nameClicked(item: FoundedEntity) {

    }

    override fun itemSelectedForDownload() {
    }

    private fun showDownloadsDialog(downloadLinks: ArrayList<DownloadLink>) {
        val inflate = layoutInflater
        @SuppressLint("InflateParams") val view =
            inflate.inflate(R.layout.confirm_book_type_select, null)
        val checker: SwitchCompat = view.findViewById(R.id.reDownload)
        checker.isChecked = PreferencesHandler.instance.isReDownload
        val dialogBuilder = AlertDialog.Builder(requireContext(), R.style.MyDialogStyle)
        dialogBuilder.setTitle(R.string.downloads_dialog_header)
        // получу список типов данных
        val linksLength = downloadLinks.size
        val linksArray = arrayOfNulls<String>(linksLength)
        var counter = 0
        var mime: String?
        while (counter < linksLength) {
            mime = downloadLinks[counter].mime
            linksArray[counter] = MimeTypes.getMime(mime!!)
            counter++
        }
        dialogBuilder.setItems(linksArray) { dialogInterface: DialogInterface, i: Int ->
            // проверю, выбрано ли сохранение формата загрузки
            val dialog = dialogInterface as Dialog
            var switcher: SwitchCompat = dialog.findViewById(R.id.save_type_selection)
            if (switcher.isChecked) {
                // запомню выбор формата
                Toast.makeText(
                    requireContext(),
                    "Предпочтительный формат для скачивания сохранён (" + MimeTypes.getFullMime(
                        linksArray[i]
                    ) + "). Вы можете сбросить его в настройки +> разное.",
                    Toast.LENGTH_LONG
                ).show()
                PreferencesHandler.instance.favoriteMime = MimeTypes.getFullMime(linksArray[i])
            }
            switcher = dialog.findViewById(R.id.reDownload)
            PreferencesHandler.instance.isReDownload = switcher.isChecked
            // получу сокращённый MIME
            val shortMime = linksArray[i]
            val longMime = MimeTypes.getFullMime(shortMime)
            var counter1 = 0
            val linksLength1 = downloadLinks.size
            var item: DownloadLink
            while (counter1 < linksLength1) {
                item = downloadLinks[counter1]
                if (item.mime == longMime) {
                    viewModel.addToDownloadQueue(item)
                    break
                }
                counter1++
            }
            Toast.makeText(
                requireContext(),
                "Книга добавлена в очередь загрузок",
                Toast.LENGTH_SHORT
            ).show()
        }
            .setView(view)
        val dialog = dialogBuilder.create()
        dialog.show()
    }
}