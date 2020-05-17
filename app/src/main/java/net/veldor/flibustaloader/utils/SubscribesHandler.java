package net.veldor.flibustaloader.utils;

import net.veldor.flibustaloader.App;
import net.veldor.flibustaloader.selections.SubscriptionItem;

import java.util.ArrayList;

public class SubscribesHandler {
    public static ArrayList<SubscriptionItem> getAllSubscribes() {
        SubscribeBooks booksSubscribeContainer = App.getInstance().getBooksSubscribe();
        SubscribeAuthors authorsSubscribeContainer = App.getInstance().getAuthorsSubscribe();
        SubscribeSequences sequencesSubscribeContainer = App.getInstance().getSequencesSubscribe();
        ArrayList<SubscriptionItem> subscribingList = new ArrayList<>(booksSubscribeContainer.getSubscribes());
        subscribingList.addAll(authorsSubscribeContainer.getSubscribes());
        subscribingList.addAll(sequencesSubscribeContainer.getSubscribes());
        return subscribingList;
    }
}
