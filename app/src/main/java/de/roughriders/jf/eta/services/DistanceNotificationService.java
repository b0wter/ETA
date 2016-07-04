package de.roughriders.jf.eta.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.DistanceMatrixElement;
import com.google.maps.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.roughriders.jf.eta.R;
import de.roughriders.jf.eta.activities.TripActivity;
import de.roughriders.jf.eta.helpers.Converter;

/**
 * Created by b0wter on 6/12/16.
 */
public class DistanceNotificationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "DistanceNotification";
    public static final String COMMAND_EXTRA = "command";
    public static final String COMMAND_START = "start";
    public static final String COMMAND_STOP = "stop";
    public static final String PHONE_EXTRA = "phoneExtra";
    public static final String DESTINATION_EXTRA = "destinationExtra";
    private static final int NOTIFICATION_ID = 1;
    private String phoneNumber;
    private String destination;
    private GoogleApiClient apiClient;
    private GeoApiContext geoApiContext;
    private Location currentLocation;
    private Notification.Builder notificationBuilder;

    private long remainingDistanceInMeters;
    private long remainingDuractionInSeconds;
    private long lastupdateCheckTicks = -1;

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

        // TODO: fix the intent the stop button of the notification send:
        if(extras == null && intent.getAction().equals(COMMAND_STOP)) {
            stop();
            return result;
        }

        String command = extras.getString(COMMAND_EXTRA);
        switch(command){
            case COMMAND_START:
                start(extras.getString(PHONE_EXTRA), extras.getString(DESTINATION_EXTRA));
                break;
            case COMMAND_STOP:
                stop();
                break;
            default:
                throw new IllegalArgumentException("Unknown command given: " + command);
        }
        return result;
    }

    @Override
    public void onDestroy(){
        stop();
    }

    private void initService(){
        geoApiContext = new GeoApiContext().setApiKey(getString(R.string.api_server_key));
        apiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        apiClient.connect();
    }

    private void start(String phone, String destination){
        Log.d(TAG, "Service received COMMAND_START");
        this.phoneNumber = phone;
        this.destination = destination;
        initService();
        showNotification();
    }

    private void stop(){
        Log.d(TAG, "Service received COMMAND_STOP");
        if(apiClient != null)
            apiClient.disconnect();
        removeNotification();
    }

    // GoogleAPIClient callback
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Connection to Google Api Client established.");
        startLocationUpdates(5);
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

    /**
     * Starts the first location update to compute the initial remaining distance and time.
     * @param interval
     */
    private void startLocationUpdates(int interval){
        LocationRequest request = new LocationRequest();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setInterval(interval);
        request.setFastestInterval(interval);
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            final LocationListener listener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    LocationServices.FusedLocationApi.removeLocationUpdates(apiClient, this);
                    Log.i(TAG, "Received location update: " + location.getLatitude() + "-" + location.getLongitude() + " " + location.getAccuracy());
                    currentLocation = location;
                    try {
                        requestTripDuration(location);
                    } catch(Exception ex){
                        Log.e(TAG, "Error while trying to locate the user:\r\n" + ex.getMessage());
                    }
                }
            };
            LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, request, listener);
        }
    }

    /**
     * Computes the initial remaining distance and duration.
     * @param startLocation
     * @throws Exception
     */
    private void requestTripDuration(Location startLocation) throws Exception {
        DistanceMatrixApiRequest request = DistanceMatrixApi.newRequest(geoApiContext);
        request.origins(convertLocationToLatLng(startLocation));
        request.destinations(destination);
        request.setCallback(new PendingResult.Callback<DistanceMatrix>() {
            @Override
            public void onResult(DistanceMatrix result) {
                Log.i(TAG, "Distance matrix request was successfull.");
                // each row in the result represents one set of start/end points
                // each row contains a set of several elements that represent one possible route to the target
                // we dont send multiple start/end points so we can skip all but the first row
                // additionally we are only interested in the fastest route so we only use the first element
                DistanceMatrixElement element = result.rows[0].elements[0];
                updateRemainingDistanceAndTime(element.duration.inSeconds, element.distance.inMeters);
                setNewLocationListener();
            }

            @Override
            public void onFailure(Throwable e) {
                Log.e(TAG, e.getMessage());
            }
        });
        request.await();
    }

    /**
     * Starts a new LocationListener. Requires that the remainingDurationInSeconds is up-to-date!
     */
    private void setNewLocationListener(){
        long nextUpdateInterval = computeUpdateInterval(remainingDuractionInSeconds);
        LocationRequest request = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(nextUpdateInterval*1000).setFastestInterval(nextUpdateInterval*1000);
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, request, this);
        else
            Log.e(TAG, "Cannot use the FusedLocationApi because the permission was not granted!");
    }

    /**
     * Callback function for the FusedLocationService.
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "A location update has been recieved.");
        LocationServices.FusedLocationApi.removeLocationUpdates(apiClient, this);

    }

    private void sendSmsIfNeeded(){
        if(isSmsNeeded())
            sendSms();
    }

    private boolean isSmsNeeded(){
        return true;
    }

    private void sendSms(){

    }

    private void showNotification(){
        Intent showTaskIntent = new Intent(getApplicationContext(), TripActivity.class);
        showTaskIntent.setAction(Intent.ACTION_MAIN);
        showTaskIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        showTaskIntent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);

        Intent callbackIntent = new Intent(COMMAND_STOP, null, this, DistanceNotificationService.class);
        callbackIntent.putExtra(COMMAND_EXTRA, COMMAND_STOP);

        PendingIntent serviceIntent = PendingIntent.getService(this, 1, callbackIntent, 0);

        notificationBuilder = new Notification.Builder(getApplicationContext())
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.distancenotificationservice_is_initializing_notification_message))
                .setSmallIcon(R.drawable.ic_directions_car_white_24dp)
                .setWhen(System.currentTimeMillis())
                .addAction(R.drawable.ic_stop_white_24dp, getString(R.string.stopCapital), serviceIntent)
                .setOngoing(true);

        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void updateNotification(){
        String distance = Converter.formatDistance(remainingDistanceInMeters);
        String duration = Converter.formatDuration(remainingDuractionInSeconds);

        Date date = new Date(lastupdateCheckTicks);
        String lastCheck = SimpleDateFormat.getTimeInstance().format(date);

        String content = getString(R.string.distancenotificationservice_is_running_notification_message);
        String longContent = getString(R.string.distancenotificationservice_is_running_notification_message_details);

        content = content.replace("%%DISTANCE%%", distance).replace("%%DURATION%%", duration).replace("%%LASTCHECK%%", lastCheck);
        longContent = longContent.replace("%%DISTANCE%%", distance).replace("%%DURATION%%", duration).replace("%%LASTCHECK%%", lastCheck);

        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notificationBuilder.setContentText(content);
        notificationBuilder.setStyle(new Notification.BigTextStyle().bigText(longContent));
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void removeNotification(){
        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private LatLng convertLocationToLatLng(Location location){
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        return latLng;
    }

    private void onReachedDestination(){
        //TODO: send arrival sms
        // work is done, exit
        stopSelf();
    }

    /**
     * Computes the time for the desired location updates. We try to keep this as big as possible.
     * The closer the user gets to his destination the more frequent the location updates;
     * @param secondsRemaining Duration of the remaining trip in seconds.
     * @return Update interval in seconds.
     */
    private long computeUpdateInterval(long secondsRemaining){
        final long veryFastUpdateIntervalMax = 5*60;
        final long fastUpdateIntervalMax = 10*60;
        final long mediumUpdateIntervalMax = 20*60;

        final long veryFastUpdateInterval = 30;
        final long fastUpdateInterval = 60;
        final long mediumUpdateInterval = 150; // cannot use 2.5*60 without cast
        final long longUpdateInterval = 10*60;

        if(secondsRemaining <= veryFastUpdateIntervalMax)
            return veryFastUpdateInterval;
        else if(secondsRemaining <= fastUpdateIntervalMax)
            return fastUpdateInterval;
        else if(secondsRemaining <= mediumUpdateIntervalMax)
            return mediumUpdateInterval;
        else
            return longUpdateInterval;
    }

    private void updateRemainingDistanceAndTime(long durationInSeconds, long distanceInMeters){
        this.remainingDuractionInSeconds = durationInSeconds;
        this.remainingDistanceInMeters = distanceInMeters;
        this.lastupdateCheckTicks = System.currentTimeMillis();
        updateNotification();
    }
}
