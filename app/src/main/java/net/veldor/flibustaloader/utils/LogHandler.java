package net.veldor.flibustaloader.utils;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * This singleton class is for debug purposes only. Use it to log your selected classes into file. <br> Needed permissions:
 * READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, READ_LOGS" <br><br>Example usage:<br> <code> FileLogHelper.getInstance().addLogTag(TAG);</code>
 * <p/>
 * Created by bendaf on 2016-04-28
 */
public class LogHandler{
    private static final String cmdBegin = "logcat -f ";
    private static final boolean shouldLog = true; //TODO: set to false in final version of the app
    private static final String TAG = "FileLogHelper";

    private String logFileAbsolutePath;
    private String cmdEnd = " *:F";
    private boolean isLogStarted = false;
    private static LogHandler mInstance;

    private LogHandler(){}

    public static LogHandler getInstance(){
        if(mInstance == null){
            mInstance = new LogHandler();
        }
        return mInstance;
    }

    public void initLog(){
        if(!isLogStarted && shouldLog){
            SimpleDateFormat dF = new SimpleDateFormat("yy-MM-dd_HH_mm''ss", Locale.getDefault());
            String fileName = "logcat_" + dF.format(new Date()) + ".txt";
            File outputFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/logcat/");
            if(outputFile.mkdirs() || outputFile.isDirectory()){
                logFileAbsolutePath = outputFile.getAbsolutePath() + "/" + fileName;
               // startLog();
                // clear the previous logcat and then write the new one to the file
                try {
                    Process process = Runtime.getRuntime().exec("logcat -c");
                    process = Runtime.getRuntime().exec("logcat -f " + logFileAbsolutePath);
                } catch ( IOException e ) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void startLog(){
        if(shouldLog){
            try{
                File prevLogFile = new File(logFileAbsolutePath);
                prevLogFile.delete();
                Runtime.getRuntime().exec(cmdBegin + logFileAbsolutePath + cmdEnd);
                isLogStarted = true;
            }catch(IOException ignored){
                Log.e(TAG, "initLogCat: failed");
            }
        }
    }

    /**
     * Add a new tag to file log.
     *
     * @param tag      The android {@link Log} tag, which should be logged into the file.
     * @param priority The priority which should be logged into the file. Can be V, D, I, W, E, F
     *
     * @see <a href="http://developer.android.com/tools/debugging/debugging-log.html#filteringOutput">Filtering Log Output</a>
     */
    public void addLogTag(String tag, String priority){
        String newEntry = " " + tag + ":" + priority;
        if(!cmdEnd.contains(newEntry)){
            cmdEnd = newEntry + cmdEnd;
            if(isLogStarted){
                startLog();
            }else{
                initLog();
            }
        }
    }

    /**
     * Add a new tag to file log with default priority, which is Verbose.
     *
     * @param tag The android {@link Log} tag, which should be logged into the file.
     */
    public void addLogTag(String tag){
        addLogTag(tag, "V");
    }
}
