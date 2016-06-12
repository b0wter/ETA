package de.roughriders.jf.eta.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

import de.roughriders.jf.eta.R;

public class TripActivity extends AppCompatActivity {

    private String destination;
    private String phoneNumber;
    private String name;

    private TextView destinationTextView;
    private TextView remainingTimeTextView;
    private TextView nameTextView;

    public static String DESTINATION_EXTRA = "destinationExtra";
    public static String PHONE_NUMBER_EXTRA = "phoneExtra";
    public static String NAME_EXTRA = "nameExtra";
    private static final int REQUEST_SMS_PERMISSION_KEY = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);
        initControls();
        setIntentData();
        //setStatusBarColor();
    }

    private void setStatusBarColor(){
        Window window = getWindow();
        window.setStatusBarColor(getColor(R.color.colorPrimaryDarkDark));
    }

    @Override
    public void onStart(){
        super.onStart();
        askOrCheckForSmsPermission();
    }

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




    private void initControls(){
        destinationTextView = (TextView)findViewById(R.id.trip_activity_destination_textview);
        remainingTimeTextView = (TextView)findViewById(R.id.trip_activity_minutes_remaining);
        nameTextView = (TextView)findViewById(R.id.trip_activity_name_textview);
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
                        finish();
                    }
                }).show();
    }

}
