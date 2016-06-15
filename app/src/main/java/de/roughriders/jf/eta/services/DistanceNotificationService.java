package de.roughriders.jf.eta.services;

import android.Manifest;
import android.app.Dialog;
import android.app.IntentService;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
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
import com.google.maps.DistanceMatrixApi;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.LatLng;

import de.roughriders.jf.eta.BuildConfig;
import de.roughriders.jf.eta.R;

/**
 * Created by b0wter on 6/12/16.
 */
public class DistanceNotificationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "DistanceNotification";
    public static final String COMMAND_EXTRA = "command";
    public static final String COMMAND_START = "start";
    public static final String COMMAND_STOP = "stop";
    public static final String PHONE_EXTRA = "phoneExtra";
    public static final String DESTINATION_EXTRA = "destinationExtra";

    private String phoneNumber;
    private String destination;
    private GoogleApiClient apiClient;
    private GeoApiContext geoApiContext;
    private Location currentLocation;

    public DistanceNotificationService(){
        Log.d(TAG, "Instantiating new DistanceNotificationService");

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.d(TAG, "received start command");
        int result = super.onStartCommand(intent, flags, startId);

        if(intent == null)
            return result;

        Bundle extras = intent.getExtras();
        String command = extras.getString(COMMAND_EXTRA);
        switch(command){
            case COMMAND_START:
                Log.d(TAG, "Service received COMMAND_START");
                phoneNumber = extras.getString(PHONE_EXTRA);
                destination = extras.getString(DESTINATION_EXTRA);
                initService();
                start();
                break;
            case COMMAND_STOP:
                Log.d(TAG, "Service received COMMAND_STOP");
                stop();
                break;
            default:
                throw new IllegalArgumentException("Unknown command given: " + command);
        }
        return result;
    }

    private void initService(){
        if(BuildConfig.DEBUG)
            geoApiContext = new GeoApiContext().setApiKey(getString(R.string.api_debug_key));
        else
            geoApiContext = new GeoApiContext().setApiKey(getString(R.string.api_production_key));
    }

    private void start(){
        apiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        apiClient.connect();
    }

    private void stop(){
        if(apiClient != null)
            apiClient.disconnect();
    }

    // GoogleAPIClient callback
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Connection to Google Api Client established.");
        startLocationUpdates();
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
                    Log.i(TAG, "Received location update: " + location.getLatitude() + "-" + location.getLongitude() + " " + location.getAccuracy());
                    currentLocation = location;
                    requestTripDuration(location);
                }
            });
        } catch(SecurityException ex){
            Log.e(TAG, ex.getLocalizedMessage());
        }
    }

    private void requestTripDuration(Location startLocation){
        DistanceMatrixApiRequest request = DistanceMatrixApi.newRequest(geoApiContext);
        request.origins(convertLocationToLatLng(startLocation));
        request.destinations(destination);
    }

    private LatLng convertLocationToLatLng(Location location){
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        return latLng;
    }
}
