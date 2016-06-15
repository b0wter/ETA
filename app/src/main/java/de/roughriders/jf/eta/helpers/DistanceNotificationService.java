package de.roughriders.jf.eta.helpers;

import android.Manifest;
import android.app.Dialog;
import android.app.IntentService;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;

/**
 * Created by b0wter on 6/12/16.
 */
public class DistanceNotificationService extends IntentService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "DistanceNotification";
    public static final String COMMAND_EXTRA = "command";
    public static final String COMMAND_START = "start";
    public static final String COMMAND_STOP = "stop";
    public static final String PHONE_EXTRA = "phoneExtra";
    public static final String DESTINATION_EXTRA = "destinationExtra";

    private String phoneNumber;
    private String destination;
    private GoogleApiClient apiClient;

    public DistanceNotificationService(){
        super("DistanceNotificationService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        String command = extras.getString(COMMAND_EXTRA);
        switch(command){
            case COMMAND_START:
                phoneNumber = extras.getString(PHONE_EXTRA);
                destination = extras.getString(DESTINATION_EXTRA);
                start();
                break;
            case COMMAND_STOP:
                stop();
                break;
            default:
                throw new IllegalArgumentException("Unknown command given: " + command);
        }
    }

    private void start(){
        apiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    private void stop(){
        if(apiClient != null)
            apiClient.disconnect();
    }

    // GoogleAPIClient callback
    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    // GoogleAPIClient callback
    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GoogleApiClient connection suspended.");
    }

    // GoogleAPIClient callback
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "Connection to the Google Api Client failed.");
        stop();
    }

    private void startLocationUpdates(){
        LocationRequest request = new LocationRequest();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setInterval(5);
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, request, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {

                }
            });
        } catch(SecurityException ex){
            Log.e(TAG, ex.getLocalizedMessage());
        }
    }

    private Location getLastKnownLocation(){
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if(permissionCheck == PackageManager.PERMISSION_GRANTED) {
            Location location = LocationServices.FusedLocationApi.getLastLocation(apiClient);
            return location;
        }
        else {
            Toast.makeText(this, "The app is not allowed to access your location.", Toast.LENGTH_SHORT).show();
            throw new IllegalStateException("App cannot run without location permission.");
        }
    }
}
