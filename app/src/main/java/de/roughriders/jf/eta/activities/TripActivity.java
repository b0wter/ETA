package de.roughriders.jf.eta.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.lzyzsd.circleprogress.ArcProgress;
import com.google.maps.model.Distance;

import java.util.Timer;
import java.util.TimerTask;

import de.roughriders.jf.eta.R;
import de.roughriders.jf.eta.helpers.Logger;
import de.roughriders.jf.eta.services.DistanceNotificationService;

public class TripActivity extends AppCompatActivity {

    private String destination;
    private String phoneNumber;
    private String name;
    private BroadcastReceiver serviceUpdateBroadcastReceiver;
    private BroadcastReceiver serviceStoppedBroadcastReceiver;

    private TextView destinationTextView;
    private TextView remainingTimeTextView;
    private TextView nameTextView;
    private ArcProgress progressBar;

    public static String DESTINATION_EXTRA = "destinationExtra";
    public static String PHONE_NUMBER_EXTRA = "phoneExtra";
    public static String NAME_EXTRA = "nameExtra";
    private static final String TAG = "TripActivity";
    public static final String SERVICE_BROADCAST_ACTION = "DISTANCE_NOTIFICATION_SERVICE_UPDATE";
    public static final String SERVICE_UPDATE_TIME_KEY = "DISTANCE_NOTIFICATION_SERVICE_REMAINING_TIME";
    public static final String SERVICE_UPDATE_DISTANCE_KEY = "DISTANCE_NOTIFICATION_SERVICE_REMAINING_DISTANCE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);
        initControls();
        setIntentData();
        registerBroadcastReceivers();
    }

    @Override
    protected void onDestroy(){
        unregisterBroadCastReceivers();
        if(isFinishing()) {
            stopService(new Intent(TripActivity.this, DistanceNotificationService.class));
            Logger.getInstance().close();
        }
        super.onDestroy();
    }

    @Override
    public void onStart(){
        super.onStart();
        tryUpdatingFromService();
    }

    private void tryUpdatingFromService(){
        if (DistanceNotificationService.IsServiceRunning) {
            Log.d(TAG, "Trying to get an update from the service");
            Intent intent = new Intent(DistanceNotificationService.REQUEST_STATUS_BROADCAST);
            sendBroadcast(intent);
        }
        else
            Log.d(TAG, "The DistanceNotificationService is not running.");
    }

    private void initControls(){
        Log.d(TAG, "initControls");
        destinationTextView = (TextView)findViewById(R.id.trip_activity_destination_textview);
        remainingTimeTextView = (TextView)findViewById(R.id.trip_activity_minutes_remaining);
        nameTextView = (TextView)findViewById(R.id.trip_activity_name_textview);
        progressBar = (ArcProgress)findViewById(R.id.trip_activity_time_remaining);
    }

    private void setIntentData(){
        Bundle extras = getIntent().getExtras();
        destination = extras.getString(DESTINATION_EXTRA);
        destinationTextView.setText(destination);
        phoneNumber = extras.getString(PHONE_NUMBER_EXTRA);
        if(extras.containsKey(NAME_EXTRA))
            name = extras.getString(NAME_EXTRA);
        if (name != null || name.isEmpty())
            name = phoneNumber;
        nameTextView.setText(name);
        progressBar.setMax(Integer.MAX_VALUE);
    }

    private void registerBroadcastReceivers(){
        registerServiceBroadCastReceiver();
        registerServiceStoppedBroadCastReceiver();
    }

    private void registerServiceBroadCastReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(SERVICE_BROADCAST_ACTION);
        serviceUpdateBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                long remainingTimeInSeconds = extras.getLong(SERVICE_UPDATE_TIME_KEY);
                long remainingDistanceInMeters = extras.getLong(SERVICE_UPDATE_DISTANCE_KEY);

                int time, distance;
                if(remainingTimeInSeconds > (long)Integer.MAX_VALUE)
                    time = Integer.MAX_VALUE;
                else
                    time = (int)remainingTimeInSeconds;
                if(remainingDistanceInMeters > (long)Integer.MAX_VALUE)
                    distance = Integer.MAX_VALUE;
                else
                    distance = (int)remainingDistanceInMeters;

                updateUi(time, distance);
            }
        };
        registerReceiver(serviceUpdateBroadcastReceiver, filter);
    }

    private void registerServiceStoppedBroadCastReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(DistanceNotificationService.SERVICE_STOPPED_BROADCAST);
        serviceStoppedBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "Received a service stopped broadcast.");
                Bundle extras = intent.getExtras();
                boolean finishedSuccessfully = extras.getBoolean(DistanceNotificationService.SERVICE_STOPPED_BROADCAST_SUCCESS_EXTRA);
                finish();
            }
        };
        registerReceiver(serviceStoppedBroadcastReceiver, filter);
    }

    private void unregisterBroadCastReceivers(){
        if(serviceUpdateBroadcastReceiver != null) {
            unregisterReceiver(serviceUpdateBroadcastReceiver);
            serviceUpdateBroadcastReceiver = null;
        }

        if(serviceStoppedBroadcastReceiver != null) {
            unregisterReceiver(serviceStoppedBroadcastReceiver);
            serviceStoppedBroadcastReceiver = null;
        }
    }

    public void onSendUpdateSmsClick(View view){
        Log.d(TAG, "Trying to send an update sms.");
        Intent intent = new Intent(DistanceNotificationService.REQUEST_SEND_SMS_UPDATE);
        sendBroadcast(intent);
        Toast.makeText(this, getString(R.string.sms_update_sent), Toast.LENGTH_SHORT).show();

        // temporarily disable the button
        //
        final Button smsButton = (Button)findViewById(R.id.trip_activity_send_notification_button);
        smsButton.setEnabled(false);
        smsButton.postDelayed(new Runnable() {
            @Override
            public void run() {
                smsButton.setEnabled(true);
            }
        }, 1500);
    }


    @Override
    public void onBackPressed(){
        showExitAlert();
    }

    private void showExitAlert(){
        new AlertDialog.Builder(this)
                .setTitle("ETA")
                .setMessage(getString(R.string.exit_hint))
                .setCancelable(false)
                .setNegativeButton(getString(android.R.string.no), null)
                .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        stopService(new Intent(TripActivity.this, DistanceNotificationService.class));
                        finish();
                    }
                }).show();
    }

    private void updateUi(int remainingTimeInSeconds, int remainingDistanceInMeters){
        ArcProgress progressBar = (ArcProgress)findViewById(R.id.trip_activity_time_remaining);
        if(progressBar.getMax() == Integer.MAX_VALUE)
            progressBar.setMax(remainingTimeInSeconds);
        progressBar.setProgress(Math.min(remainingTimeInSeconds, progressBar.getMax()));

        int minutes = remainingTimeInSeconds / 60;
        if(remainingTimeTextView != null)
            remainingTimeTextView.setText(minutes + "");
        else
            Log.e(TAG, "For whatever reason the remainingTimeTextView is a null reference! Is the view present in the landscape and regular layout?");
    }
}
