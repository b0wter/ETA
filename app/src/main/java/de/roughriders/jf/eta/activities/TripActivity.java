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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.ArcProgress;
import com.google.maps.model.Distance;

import de.roughriders.jf.eta.R;
import de.roughriders.jf.eta.services.DistanceNotificationService;

public class TripActivity extends AppCompatActivity {

    private String destination;
    private String phoneNumber;
    private String name;
    private BroadcastReceiver serviceUpdateBroadcastReceiver;

    private TextView destinationTextView;
    private TextView remainingTimeTextView;
    private TextView nameTextView;
    private ArcProgress progressBar;

    public static String DESTINATION_EXTRA = "destinationExtra";
    public static String PHONE_NUMBER_EXTRA = "phoneExtra";
    public static String NAME_EXTRA = "nameExtra";
    private static final int REQUEST_SMS_PERMISSION_KEY = 0;
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
        registerServiceBroadCastReceiver();
    }

    @Override
    protected void onDestroy(){
        unregisterServiceBroadCastReceiver();
        stopService(new Intent(TripActivity.this, DistanceNotificationService.class));
        super.onDestroy();
    }

    @Override
    public void onStart(){
        super.onStart();
        askOrCheckForSmsPermission();
        startBackgroundService();
    }

    private void startBackgroundService(){
        if(DistanceNotificationService.IsServiceRunning) {
            Log.i(TAG, "The DistanceNotificationService is already running, no need to start it again.");
            return;
        }

        Log.d(TAG, "starting background service");
        Intent intent = new Intent(this, DistanceNotificationService.class);
        intent.putExtra(DistanceNotificationService.COMMAND_EXTRA, DistanceNotificationService.COMMAND_START);
        intent.putExtra(DistanceNotificationService.DESTINATION_EXTRA, destination);
        intent.putExtra(DistanceNotificationService.PHONE_EXTRA, phoneNumber);
        startService(intent);
    }

    // ----- SMS permission stuff -----
    //
    private void askOrCheckForSmsPermission(){
        if(wasSmsPermissionGranted())
            return;
        else
            showSmsPermissionExplanationAndAsk();
    }

    private boolean wasSmsPermissionGranted(){
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void showSmsPermissionExplanationAndAsk(){
        new AlertDialog.Builder(this)
                .setTitle("ETA")
                .setMessage("The app needs your permission to send sms messages to the recipient to tell him/her how long it takes you to arrive.")
                .setCancelable(false)
                .setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        askForSmsPermission();
                    }
                }).show();
    }

    private void askForSmsPermission(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS_PERMISSION_KEY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode){
        case REQUEST_SMS_PERMISSION_KEY:{
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    // permission granted, yeah!
                }
                else {
                    finish();
                }
            }
        }
    }
    //
    // -----------

    private void initControls(){
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
        else
            name = phoneNumber;
        nameTextView.setText(name);
        progressBar.setMax(Integer.MAX_VALUE);
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

    private void unregisterServiceBroadCastReceiver(){
        if(serviceUpdateBroadcastReceiver != null) {
            unregisterReceiver(serviceUpdateBroadcastReceiver);
            serviceUpdateBroadcastReceiver = null;
        }
    }

    @Override
    public void onBackPressed(){
        showExitAlert();
    }

    private void showExitAlert(){
        new AlertDialog.Builder(this)
                .setTitle("ETA")
                .setMessage("If you go back the app will no longer monitor your position and send status updates. Do you want to go back?")
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
        progressBar.setProgress(remainingTimeInSeconds);

        int minutes = remainingTimeInSeconds / 60;
        remainingTimeTextView.setText(minutes + "");
    }
}
