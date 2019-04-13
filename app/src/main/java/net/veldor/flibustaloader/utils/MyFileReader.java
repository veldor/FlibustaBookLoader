package net.veldor.flibustaloader.utils;

import android.os.Environment;
import android.util.Log;

import net.veldor.flibustaloader.App;

import org.w3c.dom.Document;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MyFileReader {
    private static final String SEARCH_AUTOCOMPLETE_FILE = "searchAutocomplete.xml";
    private static final String SEARCH_AUTOCOMPLETE_NEW = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><search> </search>";

    public static String readFile(String filename){
        StringBuilder text = new StringBuilder();
        BufferedReader br = null;
        try {
            File sdcard = Environment.getDataDirectory();
            File file = new File(sdcard,filename);
            br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        } finally{
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return text.toString();
    }

    public static String getSearchAutocomplete(){

        File autocompleteFile = new File(App.getInstance().getFilesDir(), SEARCH_AUTOCOMPLETE_FILE);
        if(!autocompleteFile.exists()){
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

    static void saveSearchAutocomplete(String value){
        File autocompleteFile = new File(App.getInstance().getFilesDir(), SEARCH_AUTOCOMPLETE_FILE);
        makeFile(autocompleteFile, value);
        Log.d("surprise", "MyFileReader saveSearchAutocomplete: save file " + autocompleteFile);
    }

    private static void makeFile(File file, String content){
            try {
                FileWriter writer = new FileWriter(file);
                writer.append(content);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }
}
