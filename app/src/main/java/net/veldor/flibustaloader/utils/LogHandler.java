package net.veldor.flibustaloader.utils;

import android.os.Environment;

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
    private static final boolean shouldLog = true; //TODO: set to false in final version of the app

    private static LogHandler mInstance;

    private LogHandler(){}

    public static LogHandler getInstance(){
        if(mInstance == null){
            mInstance = new LogHandler();
        }
        return mInstance;
    }

    public void initLog(){
        boolean isLogStarted = false;
        if(!isLogStarted && shouldLog){
            SimpleDateFormat dF = new SimpleDateFormat("yy-MM-dd_HH_mm''ss", Locale.getDefault());
            String fileName = "logcat_" + dF.format(new Date()) + ".txt";
            File outputFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/logcat/");
            if(outputFile.mkdirs() || outputFile.isDirectory()){
                String logFileAbsolutePath = outputFile.getAbsolutePath() + "/" + fileName;
               // startLog();
                // clear the previous logcat and then write the new one to the file
                try {
                    Runtime.getRuntime().exec("logcat -c");
                    Runtime.getRuntime().exec("logcat -f " + logFileAbsolutePath);
                } catch ( IOException e ) {
                    e.printStackTrace();
                }
            }
        }
    }

}
