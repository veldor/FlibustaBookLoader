package net.veldor.flibustaloader.ui.fragments

import net.veldor.flibustaloader.view_models.SubscriptionsViewModel
import androidx.recyclerview.widget.RecyclerView
import net.veldor.flibustaloader.utils.SubscribeBooks
import net.veldor.flibustaloader.adapters.SubscribesAdapter
import net.veldor.flibustaloader.utils.SubscribeAuthors
import net.veldor.flibustaloader.utils.SubscribeSequences
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import net.veldor.flibustaloader.R
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import net.veldor.flibustaloader.App
import androidx.appcompat.widget.SwitchCompat
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import net.veldor.flibustaloader.utils.PreferencesHandler

class SubscriptionsFragment : Fragment() {
    private var mRoot: View? = null
    private var mSubscribeInput: EditText? = null
    private var mViewModel: SubscriptionsViewModel? = null
    private var mRadioContainer: RadioGroup? = null
    private lateinit var mRecycler: RecyclerView
    private var mBooksSubscribeContainer: SubscribeBooks? = null
    private lateinit var mSubscribesAdapter: SubscribesAdapter
    private var mAuthorsSubscribeContainer: SubscribeAuthors? = null
    private var mSequencesSubscribeContainer: SubscribeSequences? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        mViewModel = ViewModelProvider(this).get(SubscriptionsViewModel::class.java)
        mRoot = inflater.inflate(R.layout.fragment_subscribe, container, false)
        if (mRoot != null) {
            Log.d("surprise", "SubscriptionsFragment onCreate 32: setup ui")
            setupUI()
            val activity = activity
            if (activity != null) {
                // буду отслеживать изменения списка книг
                val refresh: LiveData<Boolean> = mBooksSubscribeContainer!!.mListRefreshed
                refresh.observe(requireActivity(), { aBoolean: Boolean? ->
                    if (aBoolean != null && aBoolean && mRadioContainer!!.checkedRadioButtonId == R.id.searchBook) {
                        refreshSubscriptionList()
                    }
                })

                // буду отслеживать изменения списка авторов
                val authorRefresh: LiveData<Boolean> = mAuthorsSubscribeContainer!!.mListRefreshed
                authorRefresh.observe(requireActivity(), { aBoolean: Boolean? ->
                    if (aBoolean != null && aBoolean && mRadioContainer!!.checkedRadioButtonId == R.id.searchAuthor) {
                        refreshAuthorSubscriptionList()
                    }
                })

                // буду отслеживать изменения списка серий
                val sequenceRefresh: LiveData<Boolean> =
                    mSequencesSubscribeContainer!!.mListRefreshed
                sequenceRefresh.observe(requireActivity(), { aBoolean: Boolean? ->
                    if (aBoolean != null && aBoolean && mRadioContainer!!.checkedRadioButtonId == R.id.searchSequence) {
                        refreshSequenceSubscriptionList()
                    }
                })
            }
        }
        return mRoot
    }

    private fun refreshAuthorSubscriptionList() {
        val autocompleteValues = mAuthorsSubscribeContainer!!.getSubscribes()
        mSubscribesAdapter.changeList(autocompleteValues)
        mSubscribesAdapter!!.notifyDataSetChanged()
    }

    private fun refreshSequenceSubscriptionList() {
        val autocompleteValues = mSequencesSubscribeContainer!!.getSubscribes()
        mSubscribesAdapter.changeList(autocompleteValues)
        mSubscribesAdapter!!.notifyDataSetChanged()
    }

    private fun refreshSubscriptionList() {
        val autocompleteValues = mBooksSubscribeContainer!!.getSubscribes()
        mSubscribesAdapter.changeList(autocompleteValues)
        mSubscribesAdapter!!.notifyDataSetChanged()
    }

    private fun setupUI() {
        mRecycler = mRoot!!.findViewById(R.id.resultsList)
        mRecycler.setLayoutManager(LinearLayoutManager(context))
        mBooksSubscribeContainer = App.instance.booksSubscribe
        mAuthorsSubscribeContainer = App.instance.authorsSubscribe
        mSequencesSubscribeContainer = App.instance.sequencesSubscribe
        showBooks()

        // отслежу переключение типа подписки
        mRadioContainer = mRoot!!.findViewById(R.id.subscribe_type)
        if (mRadioContainer != null) {
            mRadioContainer!!.setOnCheckedChangeListener { group: RadioGroup?, checkedId: Int ->
                if (checkedId == R.id.searchBook) {
                    showBooks()
                } else if (checkedId == R.id.searchAuthor) {
                    showAuthors()
                } else if (checkedId == R.id.searchSequence) {
                    showSequences()
                }
            }
        }

        // обработаю добавление книги в список загрузки
        // добавлю идентификатор строки поиска
        mSubscribeInput = mRoot!!.findViewById(R.id.subscribe_name)
        val subscribeBtn = mRoot!!.findViewById<Button>(R.id.add_to_blacklist_btn)
        subscribeBtn?.setOnClickListener { view: View? -> addSubscribe(view) }

        // назначу действие переключателю автоматической подписки
        val switcher: SwitchCompat = mRoot!!.findViewById(R.id.switchAutoCheckSubscribes)
        switcher.isChecked = PreferencesHandler.instance.isSubscriptionsAutoCheck
        switcher.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            PreferencesHandler.instance.isSubscriptionsAutoCheck = !PreferencesHandler.instance.isSubscriptionsAutoCheck
            mViewModel!!.switchSubscriptionsAutoCheck()
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
        // назначу действие кнопкам проверки подписок
        val fullSubCheckBtn = mRoot!!.findViewById<Button>(R.id.totalCheckButton)
        fullSubCheckBtn?.setOnClickListener { v: View ->
            Toast.makeText(
                context,
                "Выполняю поиск подписок по всем новинкам, это займёт время. Найденные результаты будут отображаться во вкладке 'Найденное'.",
                Toast.LENGTH_SHORT
            ).show()
            v.isEnabled = false
            mViewModel!!.fullCheckSubscribes()
        }
        // назначу действие кнопкам проверки подписок
        val subCheckBtn = mRoot!!.findViewById<Button>(R.id.fastCheckButton)
        if (fullSubCheckBtn != null) {
            subCheckBtn.setOnClickListener { v: View ->
                Toast.makeText(
                    context,
                    "Выполняю поиск по книгам, которые поступили после последней проверки. Найденные результаты будут отображаться во вкладке 'Найденное'.",
                    Toast.LENGTH_SHORT
                ).show()
                v.isEnabled = false
                mViewModel!!.checkSubscribes()
            }
        }
    }

    private fun showBooks() {
        // получу подписки на книги
        val autocompleteValues = mBooksSubscribeContainer!!.getSubscribes()
        mSubscribesAdapter = SubscribesAdapter(autocompleteValues!!)
        mRecycler!!.adapter = mSubscribesAdapter
    }

    private fun showAuthors() {
        // получу подписки на авторов
        val autocompleteValues = mAuthorsSubscribeContainer!!.getSubscribes()
        mSubscribesAdapter = SubscribesAdapter(autocompleteValues!!)
        mRecycler!!.adapter = mSubscribesAdapter
    }

    private fun showSequences() {
        // получу подписки на серии
        val autocompleteValues = mSequencesSubscribeContainer!!.getSubscribes()
        mSubscribesAdapter = SubscribesAdapter(autocompleteValues!!)
        mRecycler!!.adapter = mSubscribesAdapter
    }

    fun addSubscribe(view: View?) {
        val value = mSubscribeInput!!.text.toString().trim { it <= ' ' }
        if (!value.isEmpty()) {
            // добавлю подписку в зависимости от типа
            val checkedRadioButtonId = mRadioContainer!!.checkedRadioButtonId
            if (checkedRadioButtonId == R.id.searchBook) {
                Log.d("surprise", "SubscribeActivity addSubscribe add book")
                mBooksSubscribeContainer!!.addValue(value)
            } else if (checkedRadioButtonId == R.id.searchAuthor) {
                Log.d("surprise", "SubscribeActivity addSubscribe add author")
                mAuthorsSubscribeContainer!!.addValue(value)
            } else if (checkedRadioButtonId == R.id.searchSequence) {
                Log.d("surprise", "SubscribeActivity addSubscribe add sequence")
                mSequencesSubscribeContainer!!.addValue(value)
            }
            mSubscribeInput!!.setText("")
            Toast.makeText(context, "Добавляю значение $value", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Введите значение для подписки", Toast.LENGTH_LONG).show()
            mSubscribeInput!!.requestFocus()
        }
    }
}