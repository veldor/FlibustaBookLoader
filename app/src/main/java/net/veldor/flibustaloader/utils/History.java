package net.veldor.flibustaloader.utils;


import java.util.Stack;

public class History {

    private static History instance;

    public static History getInstance() {
        if(instance == null){
            instance = new History();
        }
        return instance;
    }

    private final Stack<String> mHistory = new Stack<>();

    private History(){}

    public void addToHistory(String url){
        mHistory.push(url);
    }

    public boolean isEmpty() {
        return !mHistory.isEmpty();
    }


    public String getLastPage() {
        mHistory.pop();
        if(isEmpty()){
            return mHistory.pop();
        }
        return null;
    }

    public String showLastPage() {
        return mHistory.peek();
    }
}
