package net.veldor.flibustaloader.ui.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import co.mobiwise.materialintro.shape.Focus
import co.mobiwise.materialintro.shape.FocusGravity
import co.mobiwise.materialintro.shape.ShapeType
import co.mobiwise.materialintro.view.MaterialIntroView
import net.veldor.flibustaloader.R
import net.veldor.flibustaloader.adapters.SubscribesAdapter
import net.veldor.flibustaloader.databinding.FragmentSubscribeBinding
import net.veldor.flibustaloader.utils.*
import net.veldor.flibustaloader.view_models.SubscriptionsViewModel

class SubscriptionsFragment : Fragment() {
    lateinit var binding: FragmentSubscribeBinding
    private lateinit var mViewModel: SubscriptionsViewModel
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        mViewModel = ViewModelProvider(this).get(SubscriptionsViewModel::class.java)
        binding = FragmentSubscribeBinding.inflate(inflater, container, false)
        val root = binding.root

        setupUI()
        setupObservers()


        return root
    }

    private fun setupObservers() {
        // буду отслеживать изменения списка книг
        SubscribeBooks.instance.liveSubscribeListAdd.observe(viewLifecycleOwner, {
            if (it != null) {
                if (binding.subscribeType.checkedRadioButtonId == R.id.searchBook) {
                    (binding.resultsList.adapter as SubscribesAdapter).itemAdded(it)
                    SubscribeBooks.instance.liveSubscribeListAdd.value = null
                }
            }
        })
        SubscribeBooks.instance.liveSubscribeListRemove.observe(viewLifecycleOwner, {
            if (binding.subscribeType.checkedRadioButtonId == R.id.searchBook) {
                (binding.resultsList.adapter as SubscribesAdapter).itemRemoved(it)
            }
        })
        // буду отслеживать изменения списка авторов
        SubscribeAuthors.instance.liveSubscribeListAdd.observe(viewLifecycleOwner, {
            if (it != null) {
                if (binding.subscribeType.checkedRadioButtonId == R.id.searchAuthor) {
                    (binding.resultsList.adapter as SubscribesAdapter).itemAdded(it)
                    SubscribeAuthors.instance.liveSubscribeListAdd.value = null
                }
            }
        })
        SubscribeAuthors.instance.liveSubscribeListRemove.observe(viewLifecycleOwner, {
            if (binding.subscribeType.checkedRadioButtonId == R.id.searchAuthor) {
                (binding.resultsList.adapter as SubscribesAdapter).itemRemoved(it)
            }
        })
        // буду отслеживать изменения списка серий
        SubscribeSequences.instance.liveSubscribeListAdd.observe(viewLifecycleOwner, {
            if (it != null) {
                if (binding.subscribeType.checkedRadioButtonId == R.id.searchSequence) {
                    (binding.resultsList.adapter as SubscribesAdapter).itemAdded(it)
                    SubscribeSequences.instance.liveSubscribeListAdd.value = null
                }
            }
        })
        SubscribeSequences.instance.liveSubscribeListRemove.observe(viewLifecycleOwner, {
            if (binding.subscribeType.checkedRadioButtonId == R.id.searchSequence) {
                (binding.resultsList.adapter as SubscribesAdapter).itemRemoved(it)
            }
        })
        // буду отслеживать изменения списка жанров
        SubscribeGenre.instance.liveSubscribeListAdd.observe(viewLifecycleOwner, {
            if (it != null) {
                if (binding.subscribeType.checkedRadioButtonId == R.id.searchGenre) {
                    (binding.resultsList.adapter as SubscribesAdapter).itemAdded(it)
                    SubscribeGenre.instance.liveSubscribeListAdd.value = null
                }
            }
        })
        SubscribeGenre.instance.liveSubscribeListRemove.observe(viewLifecycleOwner, {
            if (binding.subscribeType.checkedRadioButtonId == R.id.searchGenre) {
                (binding.resultsList.adapter as SubscribesAdapter).itemRemoved(it)
            }
        })
    }


    private fun setupUI() {
        showHints()
        if(PreferencesHandler.instance.isEInk){
            binding.searchBook.setTextColor(ResourcesCompat.getColor(resources, R.color.black, null))
            binding.searchAuthor.setTextColor(ResourcesCompat.getColor(resources, R.color.black, null))
            binding.searchSequence.setTextColor(ResourcesCompat.getColor(resources, R.color.black, null))
            binding.searchGenre.setTextColor(ResourcesCompat.getColor(resources, R.color.black, null))
        }
        binding.subscribeItemInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // Do whatever you want here
                addSubscribe()
                true
            } else false
        }

        binding.resultsList.layoutManager = LinearLayoutManager(context)
        binding.resultsList.adapter = SubscribesAdapter(SubscribeBooks.instance.getSubscribes())
        binding.subscribeType.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            when (checkedId) {
                R.id.searchBook -> {
                    (binding.resultsList.adapter as SubscribesAdapter).changeList(SubscribeBooks.instance.getSubscribes())
                }
                R.id.searchAuthor -> {
                    (binding.resultsList.adapter as SubscribesAdapter).changeList(SubscribeAuthors.instance.getSubscribes())
                }
                R.id.searchSequence -> {
                    (binding.resultsList.adapter as SubscribesAdapter).changeList(SubscribeSequences.instance.getSubscribes())
                }
                R.id.searchGenre -> {
                    (binding.resultsList.adapter as SubscribesAdapter).changeList(SubscribeGenre.instance.getSubscribes())
                }
            }
        }

        binding.addSubscriptionBtn.setOnClickListener { addSubscribe() }

        // назначу действие переключателю автоматической подписки
        binding.switchAutoCheckSubscribes.isChecked =
            PreferencesHandler.instance.isSubscriptionsAutoCheck
        binding.switchAutoCheckSubscribes.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            PreferencesHandler.instance.isSubscriptionsAutoCheck =
                !PreferencesHandler.instance.isSubscriptionsAutoCheck
            mViewModel.switchSubscriptionsAutoCheck()
            if (isChecked) {
                Toast.makeText(
                    context,
                    "Подписки будут автоматически проверяться раз в сутки",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    context,
                    "Автоматическая провека подписок отключена, чтобы проверить- нажмите на кнопки выше",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showHints() {
        showFirstHelp()
    }

    private fun addSubscribe() {
        val value = binding.subscribeItemInput.text.toString().trim { it <= ' ' }
        if (value.isNotEmpty()) {
            // добавлю подписку в зависимости от типа
            when (binding.subscribeType.checkedRadioButtonId) {
                R.id.searchBook -> {
                    SubscribeBooks.instance.addValue(value)
                }
                R.id.searchAuthor -> {
                    SubscribeAuthors.instance.addValue(value)
                }
                R.id.searchSequence -> {
                    SubscribeSequences.instance.addValue(value)
                }
                R.id.searchGenre -> {
                    SubscribeGenre.instance.addValue(value)
                }
            }
            binding.subscribeItemInput.setText("")
            Toast.makeText(context, "Добавляю значение $value", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Введите значение для подписки", Toast.LENGTH_LONG).show()
            binding.subscribeItemInput.requestFocus()
        }
    }
}


private fun SubscriptionsFragment.showFirstHelp() {
    Handler(Looper.getMainLooper()).postDelayed({
        kotlin.run {
            MaterialIntroView.Builder(requireActivity())
                .enableDotAnimation(true)
                .enableIcon(false)
                .setFocusGravity(FocusGravity.CENTER)
                .setFocusType(Focus.MINIMUM)
                .setDelayMillis(300)
                .setUsageId("subscribe type select")
                .enableFadeAnimation(true)
                .performClick(true)
                .setListener {
                    showSecondHelp()
                }
                .setInfoText(getString(R.string.select_subscription_type_message))
                .setTarget(binding.searchBook)
                .setShape(ShapeType.CIRCLE)
                .show()
        }
    }, 100)
}

private fun SubscriptionsFragment.showSecondHelp() {
    Handler(Looper.getMainLooper()).postDelayed({
        kotlin.run {
            MaterialIntroView.Builder(requireActivity())
                .enableDotAnimation(true)
                .enableIcon(false)
                .setFocusGravity(FocusGravity.CENTER)
                .setFocusType(Focus.MINIMUM)
                .setDelayMillis(700)
                .setUsageId("subscribe value enter")
                .enableFadeAnimation(true)
                .performClick(true)
                .setListener {

                }
                .setInfoText(getString(R.string.subscription_second_hint_text))
                .setTarget(binding.subscribeItemInput)
                .setShape(ShapeType.CIRCLE)
                .show()
        }
    }, 100)
}