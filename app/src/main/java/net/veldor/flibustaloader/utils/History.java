package net.veldor.flibustaloader.utils;


import android.util.Log;

import java.util.Stack;

public class History {

    private static History instance;

    public static History getInstance() {
        if (instance == null) {
            instance = new History();
        }
        return instance;
    }

    private final Stack<String> mHistory = new Stack<>();

    private History() {
    }

    public void addToHistory(String url) {
        mHistory.push(url);
        Log.d("surprise", "History addToHistory 23: now history size is " + mHistory.size());
    }

    public boolean isEmpty() {
        return mHistory.size() == 0;
    }

    public String getLastPage() {
        if(mHistory.size() > 0){
            return mHistory.pop();
        }
        return null;
//        Log.d("surprise", "History getLastPage 34: first backward? " + firstBackward + ", history size is " + mHistory.size());
//        if (mHistory.size() > 1) {
//            mHistory.pop();
//            Log.d("surprise", "History getLastPage 38: delete item from history");
//            String item = mHistory.pop();
//            Log.d("surprise", "History getLastPage 36: left history size " + mHistory.size() + " " + firstBackward);
//            return item;
//        } else if (mHistory.size() == 1 && !firstBackward) {
//            return mHistory.peek();
//        }
//        else if(mHistory.size() > 0){
//            return mHistory.pop();
//        }
//        return null;
    }

    public String showLastPage() {
        return mHistory.peek();
    }

    public boolean isOneElementInQueue() {
        return mHistory.size() == 1;
    }
}
