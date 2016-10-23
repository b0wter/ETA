package de.roughriders.jf.eta.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.github.lzyzsd.circleprogress.ArcProgress;
import de.roughriders.jf.eta.R;
import de.roughriders.jf.eta.helpers.Converter;
import de.roughriders.jf.eta.helpers.Logger;
import de.roughriders.jf.eta.services.DistanceNotificationService;

public class TripActivity extends AppCompatActivity {

    private String destination;
    private String phoneNumber;
    private String name;
    private BroadcastReceiver serviceUpdateBroadcastReceiver;
    private BroadcastReceiver serviceStoppedBroadcastReceiver;
    private BroadcastReceiver serviceDestinationNameBroadcastReceiver;
    private BroadcastReceiver serviceEncounteredErrorBroadcastReceiver;

    private TextView destinationTextView;
    private TextView nameTextView;
    private ArcProgress progressBar;
    private ImageView contactImageView;
    private ImageButton toggleKeepScreenOnButton;

    private boolean keepScreenOn = false;
    private Converter converter;
    private boolean isInitializing = true;

    public static String DESTINATION_EXTRA = "destinationExtra";
    public static String PHONE_NUMBER_EXTRA = "phoneExtra";
    public static String NAME_EXTRA = "nameExtra";
    public static String PHOTO_EXTRA = "photoExtra";
    private static final String TAG = "TripActivity";
    public static final String SERVICE_BROADCAST_ACTION = "DISTANCE_NOTIFICATION_SERVICE_UPDATE";
    public static final String SERVICE_UPDATE_TIME_KEY = "DISTANCE_NOTIFICATION_SERVICE_REMAINING_TIME";
    public static final String SERVICE_UPDATE_DISTANCE_KEY = "DISTANCE_NOTIFICATION_SERVICE_REMAINING_DISTANCE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);
        converter = new Converter(this);
        initControls();
        if(!hasIntentDataBeenSet())
            setIntentData();
        registerBroadcastReceivers();
        setDisplayStatus();
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
        setToggleDisplayTimeoutButtonState();
    }

    private void setToggleDisplayTimeoutButtonState(){
        int flags = getWindow().getAttributes().flags;
        if((flags & WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)
            toggleKeepScreenOnButton.setImageDrawable(getDrawable(R.drawable.display_on));
        else
            toggleKeepScreenOnButton.setImageDrawable(getDrawable(R.drawable.display_off));
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
        nameTextView = (TextView)findViewById(R.id.trip_activity_name_textview);
        progressBar = (ArcProgress)findViewById(R.id.trip_activity_time_remaining);
        contactImageView = (ImageView)findViewById(R.id.tripActivity_contactImageView);
        toggleKeepScreenOnButton = (ImageButton)findViewById(R.id.trip_activity_toggle_display_sleep);
    }

    private void setIntentData(){
        Bundle extras = getIntent().getExtras();
        if(extras == null){
            return;
        }
        destination = extras.getString(DESTINATION_EXTRA);
        destinationTextView.setText(destination.replace(",", ",\r\n"));
        phoneNumber = extras.getString(PHONE_NUMBER_EXTRA);
        if(extras.containsKey(NAME_EXTRA))
            name = extras.getString(NAME_EXTRA);
        if (name != null || name.isEmpty())
            nameTextView.setText(name);
        if(extras.containsKey(PHOTO_EXTRA)) {
            contactImageView.setImageURI(Uri.parse(extras.getString(PHOTO_EXTRA)));
            contactImageView.bringToFront();
            (contactImageView.getParent()).requestLayout();
            ((View)contactImageView.getParent()).invalidate();
        }
        progressBar.setMax(Integer.MAX_VALUE);
    }

    private boolean hasIntentDataBeenSet(){
        if(destination == null || phoneNumber == null)
            return false;
        if(destination.isEmpty() || phoneNumber.isEmpty())
            return false;
        return true;
    }

    private void registerBroadcastReceivers(){
        registerServiceBroadCastReceiver();
        registerServiceStoppedBroadCastReceiver();
        registerServiceDestinationNameBroadcastReceiver();
        registerServiceEncounteredErrorBroadcastReceiver();
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

    private void registerServiceDestinationNameBroadcastReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(DistanceNotificationService.REQUEST_DESTINATION_NAME);
        serviceDestinationNameBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "Received a new destination name from the service.");
                Bundle extras = intent.getExtras();
                String destination = extras.getString(DistanceNotificationService.DESTINATION_NAME_REQUEST_NAME_EXTRA);
                destinationTextView.setText(destination.replace(",", "\r\n"));
                TripActivity.this.destination = destination;
            }
        };
        registerReceiver(serviceDestinationNameBroadcastReceiver, filter);
    }

    private void registerServiceEncounteredErrorBroadcastReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(DistanceNotificationService.SERVICE_ENCOUNTERED_ERROR);
        serviceEncounteredErrorBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.e(TAG, "Received an error message from the service.");
                Bundle extras = intent.getExtras();
                String errorReason = extras.getString(DistanceNotificationService.SERVICE_ERROR_REASON_EXTRA);
                Log.e(TAG, "The reason given for the error is: " + errorReason);
                unregisterBroadCastReceivers();
                new AlertDialog.Builder(TripActivity.this).setTitle(getString(R.string.app_name)).setMessage(getString(R.string.trip_activity_catched_service_error)).setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        stopService(new Intent(TripActivity.this, DistanceNotificationService.class));
                        TripActivity.this.finish();
                    }
                }).show();
            }
        };
        registerReceiver(serviceEncounteredErrorBroadcastReceiver, filter);
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

        if(serviceDestinationNameBroadcastReceiver != null){
            unregisterReceiver(serviceDestinationNameBroadcastReceiver);
            serviceDestinationNameBroadcastReceiver = null;
        }

        if(serviceEncounteredErrorBroadcastReceiver != null){
            unregisterReceiver(serviceEncounteredErrorBroadcastReceiver);
            serviceEncounteredErrorBroadcastReceiver = null;
        }
    }

    private void setDisplayStatus(){
        keepScreenOn = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("keep_screen_on", false);
        if(keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            toggleKeepScreenOnButton.setImageDrawable(getDrawable(R.drawable.display_on));
        }
    }

    public void onSendUpdateSmsClick(View view){
        Log.d(TAG, "Trying to send an update sms.");
        Intent intent = new Intent(DistanceNotificationService.REQUEST_SEND_SMS_UPDATE);
        sendBroadcast(intent);
        Toast.makeText(this, getString(R.string.sms_update_sent), Toast.LENGTH_SHORT).show();

        // temporarily disable the button
        //
        final View smsButton = findViewById(R.id.trip_activity_send_notification_button);
    Toast.makeText(this, getString(R.string.sms_sent_toast), Toast.LENGTH_SHORT).show();
        smsButton.setEnabled(false);
        smsButton.postDelayed(new Runnable() {
            @Override
            public void run() {
                smsButton.setEnabled(true);
            }
        }, 1500);
    }

    public void onFinishTripClick(View view){
        Log.d(TAG, "User wants to finish the trip.");
        new AlertDialog.Builder(this)
                .setTitle("ETA")
                .setMessage(getString(R.string.tripActivityFinishButtonMessage))
                .setNegativeButton(getString(android.R.string.no), null)
                .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i){
                        stopService(new Intent(TripActivity.this, DistanceNotificationService.class));
                        finish();
                        SmsManager.getDefault().sendTextMessage(phoneNumber, null, getString(R.string.userFinishedTripSms), null, null);
                    }
                }).show();
    }

    public void onToggleScreenOn(View view){
        keepScreenOn = !keepScreenOn;
        if(keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            toggleKeepScreenOnButton.setImageDrawable(getDrawable(R.drawable.display_on));
            Toast.makeText(this, getString(R.string.show_keep_display_on_hint), Toast.LENGTH_LONG).show();
        }
        else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            toggleKeepScreenOnButton.setImageDrawable(getDrawable(R.drawable.display_off));
            Toast.makeText(this, getString(R.string.show_keep_display_off_hint), Toast.LENGTH_LONG).show();
        }
    }

    public void onStartNavigationClick(View view){
        startNavigationForCurrentDestination();
    }

    private void startNavigationForCurrentDestination(){
        String destination = destinationTextView.getText().toString().replace(' ', '+');
        Uri navigationIntentUri = Uri.parse("google.navigation:q=" + destination);
        Logger.getInstance().i(TAG, "Trying to start navigation intent with uri: " + destination);
        Intent navigationIntent = new Intent(Intent.ACTION_VIEW, navigationIntentUri);
        navigationIntent.setPackage("com.google.android.apps.maps");
        startActivity(navigationIntent);
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
        HideInitializationViewIfNecessary();
        ArcProgress progressBar = (ArcProgress)findViewById(R.id.trip_activity_time_remaining);
        if(progressBar.getMax() == Integer.MAX_VALUE)
            progressBar.setMax(remainingTimeInSeconds);
        progressBar.setProgress(Math.min(remainingTimeInSeconds, progressBar.getMax()));

        String remainingTime = converter.formatDurationWithAbbreviatedUnits(remainingTimeInSeconds).toUpperCase();
        progressBar.setBottomText(remainingTime);
    }

    private void HideInitializationViewIfNecessary(){
        if(isInitializing) {
            View frame = findViewById(R.id.trip_activity_initialization_overlay);
            frame.animate().alpha(0f).setDuration(750);
            isInitializing = false;
            //frame.setVisibility(View.GONE);

        }
    }
}
