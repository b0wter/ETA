package de.roughriders.jf.eta.activities;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import de.roughriders.jf.eta.R;

public class TripActivity extends AppCompatActivity {

    private String destination;
    private String phoneNumber;

    private TextView destinationTextView;
    private TextView remainingTimeTextView;

    public static String DESTINATION_EXTRA = "destinationExtra";
    public static String PHONE_NUMBER_EXTRA = "phoneExtra";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);
        initControls();
        setIntentData();
    }

    private void initControls(){
        destinationTextView = (TextView)findViewById(R.id.trip_activity_destination_textview);
        remainingTimeTextView = (TextView)findViewById(R.id.trip_activity_minutes_remaining);
    }

    private void setIntentData(){
        Bundle extras = getIntent().getExtras();
        destination = extras.getString(DESTINATION_EXTRA);
        destinationTextView.setText(destination);
        phoneNumber = extras.getString(PHONE_NUMBER_EXTRA);
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
