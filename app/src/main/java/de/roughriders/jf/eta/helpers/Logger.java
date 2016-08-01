package de.roughriders.jf.eta.helpers;

import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class Logger {

    private static Logger instance;
    public static Logger getInstance(){
        if(instance == null)
            instance = new Logger();
        return instance;
    }

    private static String folderName = "/eta";
    private static String fileName = "log.txt";
    private static FileOutputStream stream;

    private boolean isStreamOpen = false;

    private Logger() {
        open();
    }

    private void open(){
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + folderName);
        dir.mkdirs();
        File file = new File(dir, fileName);
        try {
            stream = new FileOutputStream(file, true);
            isStreamOpen = true;
        } catch(FileNotFoundException ex){
            Log.e("Logger", "Could not open log file!");
        } catch(NullPointerException ex){
            Log.e("Logger", "Could not find file. Most liklely due to not grating the write external storage.");
        }
    }

    public void close(){
        try {
            stream.close();
            isStreamOpen = false;
        }catch(IOException ex){
            Log.e("Logger", ex.getMessage());
        }
    }

    public void i(String tag, String message){
        Log.i(tag, message);
        writeNiceString(message, tag, "info");
    }

    public void d(String tag, String message){
        Log.d(tag, message);
        writeNiceString(message, tag, "debug");
    }

    public void e(String tag, String message){
        Log.e(tag, message);
        writeNiceString(message, tag, "error");
    }

    public void w(String tag, String message){
        Log.w(tag, message);
        writeNiceString(message, tag, "warning");
    }

    private void writeNiceString(String message, String tag, String level){
        String nice = getNiceString(message, tag, level);
        try{
            if(!isStreamOpen)
                open();
            stream.write(nice.getBytes());
        } catch(IOException ex){
            Log.e("Logger", ex.getMessage());
        } catch(NullPointerException ex){
            Log.e("Logger", "NullPointerException while trying to write to log file. Most likely the write external storage permission was not granted.");
        }
    }

    private static String getNiceString(String s, String tag, String level){
        Date date = new Date(System.currentTimeMillis());
        return date.toString() + " [" + level.toUpperCase() + "] <" + tag + "> " + s + "\r\n";
    }
}
