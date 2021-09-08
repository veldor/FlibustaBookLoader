package net.veldor.flibustaloader.utils

import net.veldor.flibustaloader.App
import net.veldor.flibustaloader.selections.SubscriptionItem
import java.util.ArrayList

object SubscribesHandler {
    @kotlin.jvm.JvmStatic
    fun getAllSubscribes(): ArrayList<SubscriptionItem> {
        val booksSubscribeContainer = App.instance.booksSubscribe
        val authorsSubscribeContainer = App.instance.authorsSubscribe
        val sequencesSubscribeContainer = App.instance.sequencesSubscribe
        val subscribingList = ArrayList(booksSubscribeContainer.getSubscribes())
        subscribingList.addAll(authorsSubscribeContainer.getSubscribes())
        subscribingList.addAll(sequencesSubscribeContainer.getSubscribes())
        return subscribingList
    }
}