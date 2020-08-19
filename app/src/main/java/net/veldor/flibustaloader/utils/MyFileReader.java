package net.veldor.flibustaloader.utils;

import android.util.Log;

import net.veldor.flibustaloader.App;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class MyFileReader {
    public static final String SEARCH_AUTOCOMPLETE_FILE = "searchAutocomplete.xml";
    public static final String SUBSCRIPTIONS_FILE = "subscriptions.list";
    private static final String SEARCH_AUTOCOMPLETE_NEW = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><search> </search>";
    private static final String SUBSCRIBE_NEW = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><subscribe> </subscribe>";
    private static final String BLACKLIST_NEW = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><blacklist> </blacklist>";
    public static final String BOOKS_SUBSCRIBE_FILE = "booksSubscribe.xml";
    public static final String BOOKS_BLACKLIST_FILE = "booksBlacklist.xml";
    public static final String AUTHORS_SUBSCRIBE_FILE = "authorsSubscribe.xml";
    public static final String AUTHORS_BLACKLIST_FILE = "authorsBlacklist.xml";
    public static final String SEQUENCES_SUBSCRIBE_FILE = "sequencesSubscribe.xml";
    public static final String SEQUENCES_BLACKLIST_FILE = "sequencesBlacklist.xml";
    public static final String GENRES_BLACKLIST_FILE = "genresBlacklist.xml";

    public static String getSearchAutocomplete() {

        File autocompleteFile = new File(App.getInstance().getFilesDir(), SEARCH_AUTOCOMPLETE_FILE);
        if (!autocompleteFile.exists()) {
            makeFile(autocompleteFile, SEARCH_AUTOCOMPLETE_NEW);
        }
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(autocompleteFile));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text.toString();
    }

    static String getBooksSubscribe() {

        File booksSubscribeFile = new File(App.getInstance().getFilesDir(), BOOKS_SUBSCRIBE_FILE);
        if (!booksSubscribeFile.exists()) {
            makeFile(booksSubscribeFile, SUBSCRIBE_NEW);
        }
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(booksSubscribeFile));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text.toString();
    }
    static String getBooksBlacklist() {

        File booksBlacklistFile = new File(App.getInstance().getFilesDir(), BOOKS_BLACKLIST_FILE);
        if (!booksBlacklistFile.exists()) {
            makeFile(booksBlacklistFile, BLACKLIST_NEW);
        }
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(booksBlacklistFile));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text.toString();
    }

    static String getAuthorsSubscribe() {
        File authorsSubscribeFile = new File(App.getInstance().getFilesDir(), AUTHORS_SUBSCRIBE_FILE);
        if (!authorsSubscribeFile.exists()) {
            makeFile(authorsSubscribeFile, SUBSCRIBE_NEW);
        }
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(authorsSubscribeFile));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text.toString();
    }

    static String getAuthorsBlacklist() {
        File authorsBlacklistFile = new File(App.getInstance().getFilesDir(), AUTHORS_BLACKLIST_FILE);
        if (!authorsBlacklistFile.exists()) {
            makeFile(authorsBlacklistFile, BLACKLIST_NEW);
        }
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(authorsBlacklistFile));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text.toString();
    }
    static String getSequencesBlacklist() {
        File blacklistFile = new File(App.getInstance().getFilesDir(), SEQUENCES_BLACKLIST_FILE);
        if (!blacklistFile.exists()) {
            makeFile(blacklistFile, BLACKLIST_NEW);
        }
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(blacklistFile));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text.toString();
    }
    static String getGenresBlacklist() {
        File blacklistFile = new File(App.getInstance().getFilesDir(), GENRES_BLACKLIST_FILE);
        if (!blacklistFile.exists()) {
            makeFile(blacklistFile, BLACKLIST_NEW);
        }
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(blacklistFile));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text.toString();
    }

    static String getSequencesSubscribe() {
        File sequencesSubscribeFile = new File(App.getInstance().getFilesDir(), SEQUENCES_SUBSCRIBE_FILE);
        if (!sequencesSubscribeFile.exists()) {
            makeFile(sequencesSubscribeFile, SUBSCRIBE_NEW);
        }
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(sequencesSubscribeFile));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text.toString();
    }

    static void saveSearchAutocomplete(String value) {
        File autocompleteFile = new File(App.getInstance().getFilesDir(), SEARCH_AUTOCOMPLETE_FILE);
        makeFile(autocompleteFile, value);
        Log.d("surprise", "MyFileReader saveSearchAutocomplete: save file " + autocompleteFile);
    }

    public static void clearAutocomplete() {
        File autocompleteFile = new File(App.getInstance().getFilesDir(), SEARCH_AUTOCOMPLETE_FILE);
        makeFile(autocompleteFile, SEARCH_AUTOCOMPLETE_NEW);
    }

    private static void makeFile(File file, String content) {
        try {
            FileWriter writer = new FileWriter(file);
            writer.append(content);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void saveBooksSubscription(String value) {
        File subscriptionFile = new File(App.getInstance().getFilesDir(), BOOKS_SUBSCRIBE_FILE);
        makeFile(subscriptionFile, value);
    }
    static void saveBooksBlacklist(String value) {
        File blacklistFile = new File(App.getInstance().getFilesDir(), BOOKS_BLACKLIST_FILE);
        makeFile(blacklistFile, value);
    }


    static void saveAuthorsSubscription(String value) {
        File subscriptionFile = new File(App.getInstance().getFilesDir(), AUTHORS_SUBSCRIBE_FILE);
        makeFile(subscriptionFile, value);
    }
    static void saveAuthorsBlacklist(String value) {
        File blacklistFile = new File(App.getInstance().getFilesDir(), AUTHORS_BLACKLIST_FILE);
        makeFile(blacklistFile, value);
    }

    static void saveSequencesSubscription(String value) {
        File subscriptionFile = new File(App.getInstance().getFilesDir(), SEQUENCES_SUBSCRIBE_FILE);
        makeFile(subscriptionFile, value);
    }
    static void saveSequencesBlacklist(String value) {
        File blacklistFile = new File(App.getInstance().getFilesDir(), SEQUENCES_BLACKLIST_FILE);
        makeFile(blacklistFile, value);
    }
    static void saveGenresBlacklist(String value) {
        File blacklistFile = new File(App.getInstance().getFilesDir(), GENRES_BLACKLIST_FILE);
        makeFile(blacklistFile, value);
    }
}
