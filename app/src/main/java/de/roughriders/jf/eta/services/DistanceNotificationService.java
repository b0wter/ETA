package de.roughriders.jf.eta.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
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
import com.google.repacked.kotlin.collections.BooleanIterator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.roughriders.jf.eta.R;
import de.roughriders.jf.eta.activities.TripActivity;
import de.roughriders.jf.eta.helpers.Converter;
import de.roughriders.jf.eta.models.TripSnapshot;

/**
 * Created by b0wter on 6/12/16.
 */
public class DistanceNotificationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, PendingResult.Callback<DistanceMatrix> {

    private static final String TAG = "DistanceNotification";
    public static final String COMMAND_EXTRA = "command";
    public static final String COMMAND_START = "start";
    public static final String COMMAND_STOP = "stop";
    public static final String PHONE_EXTRA = "phoneExtra";
    public static final String DESTINATION_EXTRA = "destinationExtra";
    public static final String REQUEST_STATUS_BROADCAST = "DISTANCE_NOTIFICATION_SERVICE_REQUEST_UPDATE";
    public static final String SERVICE_STOPPED_BROADCAST = "DISTANCE_NOTIFICATION_SERVICE_DESTINATION_REACHED";
    public static final String SERVICE_STOPPED_BROADCAST_SUCCESS_EXTRA = "DISTANCE_NOTIFICATION_SERVICE_DESTINATION_REACHED_SUCCESS";
    public static boolean IsServiceRunning = false;


    private static final int NOTIFICATION_ID = 1;
    private static final long MAX_API_RETRY_INTERVAL_IN_SECONDS = 5*60; // upper limit for the location update interval if the api encountered an error and tries again
    private static final long TARGET_DESTINATION_RADIUS_IN_METERS = 75; // "size" of the target. used to check if the user is at his destination
    private static final long TARGET_DURATION_LOWER_LIMIT_IN_SECONDS = 10;         // maximum duration to decide whether the user has reached his destination or not
    private String phoneNumber;
    private String destination;
    private GoogleApiClient apiClient;
    private GeoApiContext geoApiContext;
    private Notification.Builder notificationBuilder;
    private ArrayList<TripSnapshot> tripSnapshots;
    private int currentReferenceSnapshotIndex = 0;

    private long currentUpdateInterval;
    private long remainingDistanceInMeters;
    private long remainingDuractionInSeconds;
    private long lastupdateCheckTicks = -1;
    private boolean isFirstRequest = true;
    private BroadcastReceiver updateRequestBroadcastReceiver;

    public DistanceNotificationService(){
        Log.d(TAG, "Instantiating new DistanceNotificationService");
        tripSnapshots = new ArrayList<>();
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
        IsServiceRunning = true;
        this.phoneNumber = phone;
        this.destination = destination;
        initService();
        showNotification();
        registerUpdateRequestBroadcastReceiver();
    }

    private void stop(){
        stop(false);
    }

    private void stop(boolean destinationReached){
        Log.d(TAG, "Service received COMMAND_STOP");
        if(apiClient != null)
            apiClient.disconnect();
        removeNotification();
        unregisterUpdateRequestBroadcastReceiver();
        sendServiceStoppedBroadcast(destinationReached);
        IsServiceRunning = false;
    }

    // GoogleAPIClient callback
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Connection to Google Api Client established.");
        setNewLocationListener(10);
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

    private void registerUpdateRequestBroadcastReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(REQUEST_STATUS_BROADCAST);
        updateRequestBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                sendUpdateBroadcast(remainingDuractionInSeconds, remainingDistanceInMeters);
            }
        };
        registerReceiver(updateRequestBroadcastReceiver, filter);
    }

    private void unregisterUpdateRequestBroadcastReceiver(){
        if(updateRequestBroadcastReceiver != null) {
            unregisterReceiver(updateRequestBroadcastReceiver);
            updateRequestBroadcastReceiver = null;
        }
    }

    /**
     * Starts a new LocationListener with the given tick rate.
     * @param interval Interval for the LocationListener
     */
    private void setNewLocationListener(long interval){
        Log.d(TAG, "setNewLocationListener");
        currentUpdateInterval = interval;
        interval *= 1000;
        LocationRequest request = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(interval).setFastestInterval(interval - 5);
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Setting new LocationListener with an interval of " + interval + "ms.");
            LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, request, this);
        }
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
        //LocationServices.FusedLocationApi.removeLocationUpdates(apiClient, this);
        computeRemainingDistanceAndTime(location);
        //setNewLocationListener();
    }

    /**
     * Sends a distance matrix api request to get the remaining distance and duration.
     * @param location
     */
    private void computeRemainingDistanceAndTime(Location location){
        Log.d(TAG, "computeRemainingDistanceAndTime");
        DistanceMatrixApiRequest request = DistanceMatrixApi.newRequest(geoApiContext);
        request.origins(convertLocationToLatLng(location));
        request.destinations(destination);
        try{
            Log.d(TAG, "Trying to send DistanceMatrixApi-request.");
            DistanceMatrix result = request.await();
            Log.d(TAG, "Received result from DistanceMatrixApi");
            //DistanceMatrixElement element = result.rows[0].elements[0];
            //updateRemainingDistanceAndTime(element.duration.inSeconds, element.distance.inMeters);
            onResult(result);
        } catch(Exception ex){
            Log.e(TAG, "Unable to send DistanceMatrixApi-request, error:");
            Log.e(TAG, ex.getMessage());
        }
    }

    /**
     * Callback for the distance matrix api requests.
     * @param result
     */
    @Override
    public void onResult(DistanceMatrix result) {
        Log.i(TAG, "Received distance matrix api result.");
        DistanceMatrixElement element = result.rows[0].elements[0];
        updateRemainingDistanceAndTime(element.duration.inSeconds, element.distance.inMeters);

        sendTripStartSmsIfNeeded();
        sendTripSmsIfNeeded();

        if(hasReachedDestination()) {
            Log.i(TAG, "Destination has been reached.");
            onReachedDestination();
        }
        else {
            Log.i(TAG, "Destination has not yet been reached.");
            updateLocationListener();
        }
    }

    private void sendTripStartSmsIfNeeded(){
        if(isFirstRequest)
        {
            isFirstRequest = false;
            sendStartTripSms();
        }
    }

    private void sendTripSmsIfNeeded(){
        Log.d(TAG, "sendTripSmsIfNeeded");
        if(tripSnapshots.size() < 2) {
            Log.i(TAG, "there is only a single tripLocationSnapshot available, quitting function");
            return;
        }

        if(hasArrivalTimeChanged())
            sendArrivalTimeChangedSms();
        /*
        //TODO: needs some kind of defining an absolute minimum
        TripSnapshot secondToLastSnapshot = tripSnapshots.get(tripSnapshots.size()-2);
        TripSnapshot lastSnapshot = tripSnapshots.get(tripSnapshots.size()-1);
        ArrayList<Boolean> testResults = new ArrayList<>();

        // --- check for relative difference
        testResults.add(isDistanceTimeRatioAcceptable(lastSnapshot, secondToLastSnapshot));
        testResults.add(isAbsoluteDistanceAcceptable(lastSnapshot, secondToLastSnapshot));


        if(boolArrayContainsTrue(testResults))
            sendTripSms();
        */
    }

    private boolean hasArrivalTimeChanged(){
        Log.d(TAG, "hasArrivalTimeChanged");
        float acceptableRelativeTimeDifference = 0.1f;
        TripSnapshot reference = tripSnapshots.get(currentReferenceSnapshotIndex);
        TripSnapshot current = tripSnapshots.get(tripSnapshots.size()-1);

        long lowerLimit = reference.getEstimatedArrivalTime(1-acceptableRelativeTimeDifference);
        long upperLimit = reference.getEstimatedArrivalTime(1+acceptableRelativeTimeDifference);

        Log.i(TAG, "lowerLimit: " + lowerLimit + "; upperLimit: " + upperLimit + "; current: " + current.getEstimatedArrivalTime());

        if(current.getEstimatedArrivalTime() < lowerLimit || current.getEstimatedArrivalTime() > upperLimit) {
            currentReferenceSnapshotIndex = tripSnapshots.indexOf(current);
            return true;
        }
        else{
            return false;
        }
    }

    /**
     * Tests if the speed of the last two distance matrix requests is is roughly the same.
     * The main reason for this test is to check if there a traffic congestions ahead.
     * (this test gets weaker the shorter the remaining distance)
     * @param lastSnapshot
     * @param secondToLastSnapshot
     * @return
     */
    private boolean isDistanceTimeRatioAcceptable(TripSnapshot lastSnapshot, TripSnapshot secondToLastSnapshot){
        float tolerableDistanceTimeRatioDifference = 0.1f;
        if( lastSnapshot.getDistanceTimeRatio() * (1+tolerableDistanceTimeRatioDifference) > secondToLastSnapshot.getDistanceTimeRatio() ||
                lastSnapshot.getDistanceTimeRatio() * (1-tolerableDistanceTimeRatioDifference) < secondToLastSnapshot.getDistanceTimeRatio()) {
            return false;
        }
        return true;
    }

    /**
     * Tests if the actual average speed between the last two snapshots is within acceptable range of the speed that was given by the distance matrix request.
     * @param lastSnapshot
     * @param secondToLastSnapShot
     * @return
     */
    private boolean isAbsoluteDistanceAcceptable(TripSnapshot lastSnapshot, TripSnapshot secondToLastSnapShot){
        float acceptableRelativeSpeedDifference = 0.2f;
        float lastSpeed = lastSnapshot.getDistanceTimeRatio();
        float secondToLastSpeed = secondToLastSnapShot.getDistanceTimeRatio();
        float actualSpeed = ((float)secondToLastSnapShot.remainingDistanceInMeters - lastSnapshot.remainingDistanceInMeters)/((float)lastSnapshot.time - secondToLastSnapShot.time);

        Log.i(TAG, "lastSpeed: " + lastSpeed + "; secondToLastSpeed: " + secondToLastSpeed + "; actualSpeed: " + actualSpeed);

        boolean tooSlow = lastSpeed * (1-acceptableRelativeSpeedDifference) > actualSpeed;
        boolean tooFast = lastSpeed * (1+acceptableRelativeSpeedDifference) < actualSpeed;

        return !tooSlow && !tooFast;
    }

    /**
     * Checks if a list of boolean contains at least one true value;
     * @param bools
     * @return
     */
    private boolean boolArrayContainsTrue(List<Boolean> bools) {
        return boolArrayContainsValue(bools, true);
    }

    /**
     * Tests an array of booleans for a given value.
     * @param bools
     * @param value
     * @return
     */
    private boolean boolArrayContainsValue(List<Boolean> bools, boolean value){
        for(boolean b : bools){
            if(b == value)
                return true;
        }
        return false;
    }

    /**
     * Checks if the update interval needs to change and triggers the setting of a new LocationListener.
     * @return
     */
    private void updateLocationListener(){
        long neededInterval = computeUpdateInterval(remainingDuractionInSeconds);
        Log.d(TAG, "Required update interval: " + neededInterval + "; current interval: " + currentUpdateInterval);
        if(currentUpdateInterval != neededInterval){
            Log.d(TAG, "Updating LocationListener interval");
            LocationServices.FusedLocationApi.removeLocationUpdates(apiClient, this);
            setNewLocationListener(neededInterval);
        }
    }

    /**
     * Gets triggered once the destination reached requirements are met. Is responsible for stopping the service,
     * clearing the notification, etc..
     * @return
     */
    private boolean hasReachedDestination(){
        boolean distanceCheck = remainingDistanceInMeters < TARGET_DESTINATION_RADIUS_IN_METERS;
        boolean durationCheck = remainingDuractionInSeconds < TARGET_DURATION_LOWER_LIMIT_IN_SECONDS;
        return distanceCheck && durationCheck;
    }

    /**
     * Callback for the distance matrix api requests.
     * @param e
     */
    @Override
    public void onFailure(Throwable e) {
        Log.e(TAG, "A distance matrix api call has failed. Reason: " + e.getMessage());
        setRetryLocationListener();
    }

    /**
     * Sets the location listener again in case it failed.
     */
    private void setRetryLocationListener(){
        long defaultUpdateInterval = computeUpdateInterval(remainingDuractionInSeconds);
        long interval = Math.min(defaultUpdateInterval, MAX_API_RETRY_INTERVAL_IN_SECONDS);
        Log.i(TAG, "Scheduling a new LocationListener with an interval of " + String.valueOf(interval) + " seconds.");
        setNewLocationListener(interval);
    }

    private void sendStartTripSms(){
        Log.d(TAG, "sendStartTripSms");
        String text = getString(R.string.startTripSms);
        sendSms(text);
    }

    private void sendTripSms(){
        Log.d(TAG, "sendTripSms");
        String text = getString(R.string.duringTripSms);
        sendSms(text);
    }

    private void sendArrivalTimeChangedSms(){
        Log.d(TAG, "sendArrivalTimeChangedSms");
        String text = getString(R.string.arrivalTimeChangedSms);
        sendSms(text);
    }

    private void sendArrivalSms(){
        Log.d(TAG, "sendArrivalSms");
        String text = getString(R.string.arrivalSms);
        sendSms(text);
    }

    private void sendSms(String text){
        Log.d(TAG, "sendSms");
        text = fillSmsTemplate(text);
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, text, null, null);
    }

    private String fillSmsTemplate(String text){
        text = text.replace("%%DESTINATION%%", destination);
        text = text.replace("%%DURATION%%", Converter.formatDuration(remainingDuractionInSeconds));
        return text;
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
        sendArrivalSms();
        stop(true);
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
        tripSnapshots.add(new TripSnapshot(System.currentTimeMillis(), remainingDistanceInMeters, remainingDuractionInSeconds));
        sendUpdateBroadcast(durationInSeconds, distanceInMeters);
    }

    private void sendUpdateBroadcast(long durationInSeconds, long distanceInMeters){
        Log.d(TAG, "Sending broadcast: " + TripActivity.SERVICE_BROADCAST_ACTION);
        Intent intent = new Intent(TripActivity.SERVICE_BROADCAST_ACTION);
        intent.putExtra(TripActivity.SERVICE_UPDATE_TIME_KEY, durationInSeconds);
        intent.putExtra(TripActivity.SERVICE_UPDATE_DISTANCE_KEY, distanceInMeters);
        sendBroadcast(intent);
    }

    private void sendServiceStoppedBroadcast(boolean destinationReached){
        Log.d(TAG, "Sending broadcast: " + SERVICE_STOPPED_BROADCAST);
        Intent intent = new Intent(SERVICE_STOPPED_BROADCAST);
        intent.putExtra(SERVICE_STOPPED_BROADCAST_SUCCESS_EXTRA, destinationReached);
        sendBroadcast(intent);
    }
}
