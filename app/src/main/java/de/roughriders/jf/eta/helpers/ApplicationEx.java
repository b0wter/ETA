package de.roughriders.jf.eta.helpers;

import android.app.Application;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Created by b0wter on 17-Oct-16.
 */

public class ApplicationEx extends Application {
    public void onCreate(){

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler(){
            @Override
            public void uncaughtException(Thread thread, Throwable e){
                handleUncaughtException(thread, e);
            }
        });
    }

    private void handleUncaughtException(Thread t, Throwable e){
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        e.printStackTrace(printWriter);

        try{
            Logger.getInstance().e("Application", "Uncaught exception:\r\n" + e.getMessage() + "\r\n" + writer.toString());
        } catch(Exception ex){
            // doesnt matter what happens here, at least we tried
        }

        getApplicationContext().
    }
}
