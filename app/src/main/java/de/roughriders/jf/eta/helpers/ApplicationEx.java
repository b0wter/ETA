package de.roughriders.jf.eta.helpers;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import de.roughriders.jf.eta.BuildConfig;
import de.roughriders.jf.eta.R;

/**
 * Created by b0wter on 17-Oct-16.
 */

public class ApplicationEx extends Application {

    public void onCreate(){

        if(BuildConfig.DEBUG) {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable e) {
                    handleUncaughtException(thread, e);
                }
            });
        }
    }

    private void handleUncaughtException(Thread t, Throwable e){
        final Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        e.printStackTrace(printWriter);
        final String message = "Uncaught exception:\r\n" + e.getMessage() + "\r\n" + writer.toString();

        try{
            Logger.getInstance().e("ApplicationEx", message);
            Logger.getInstance().close();
        } catch(Exception ex){
            // doesnt matter what happens here, at least we tried
        }

        Intent intent = new Intent (Intent.ACTION_SEND);
        intent.setType ("plain/text");
        intent.putExtra (Intent.EXTRA_EMAIL, new String[] {"etanotifier@gmail.com"});
        intent.putExtra (Intent.EXTRA_SUBJECT, "Uncaught Exception");
        intent.putExtra(Intent.EXTRA_TEXT, message);
        startActivity (intent);
        System.exit(1);
    }
}
