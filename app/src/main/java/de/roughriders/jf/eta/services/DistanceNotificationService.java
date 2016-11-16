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
import android.preference.PreferenceManager;
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
import com.google.maps.model.TrafficModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import de.roughriders.jf.eta.R;
import de.roughriders.jf.eta.activities.MainActivity;
import de.roughriders.jf.eta.activities.TripActivity;
import de.roughriders.jf.eta.helpers.Converter;
import de.roughriders.jf.eta.helpers.Logger;
import de.roughriders.jf.eta.models.IntervalManager;
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
    public static final String SEND_CONTINUOUS_UPDATES_EXTRA = "continuousUpdatesExtra";
    public static final String SEND_INITIAL_MESSAGE_EXTRA = "initialMessageUpdateExtra";
    public static final String SEND_ALMOST_THERE_MESSAGE_EXTRA = "almostThereMessageExtra";
    public static final String SEND_ARRIVAL_MESSAGE_EXTRA = "arrivalMessageExtra";
    public static final String LATITUDE_EXTRA = "latitudeExtra";
    public static final String LONGITUDE_EXTRA = "longitudeExtra";
    public static final String REQUEST_STATUS_BROADCAST = "DISTANCE_NOTIFICATION_SERVICE_REQUEST_UPDATE";
    public static final String SERVICE_STOPPED_BROADCAST = "DISTANCE_NOTIFICATION_SERVICE_DESTINATION_REACHED";
    public static final String SERVICE_STOPPED_BROADCAST_SUCCESS_EXTRA = "DISTANCE_NOTIFICATION_SERVICE_DESTINATION_REACHED_SUCCESS";
    public static final String REQUEST_SEND_SMS_UPDATE = "DISTANCE_NOTIFICATION_SERVICE_REQUEST_SEND_SMS";
    public static final String REQUEST_DESTINATION_NAME = "DISTANCE_NOTIFICATION_SERVICE_REQUEST_DESTINATION_NAME";
    public static final String DESTINATION_NAME_REQUEST_NAME_EXTRA = "destinationNameRequestNameExtra";
    public static final String SERVICE_ENCOUNTERED_ERROR = "DISTANCE_NOTIFICATION_SERVICE_ENCOUNTERED_ERROR";
    public static final String SERVICE_ERROR_REASON_EXTRA = "errorExtra";
    public static boolean IsServiceRunning = false;

    private static final int NOTIFICATION_ID = 1;
    private static final long MAX_API_RETRY_INTERVAL_IN_SECONDS = 5*60; // upper limit for the location update interval if the api encountered an error and tries again
    private static final long TARGET_DESTINATION_RADIUS_IN_METERS = 75; // "size" of the target. used to check if the user is at his destination
    private static final long TARGET_DURATION_LOWER_LIMIT_IN_SECONDS = 25;         // maximum duration to decide whether the user has reached his destination or not
    private static final long ALMOST_THERE_DURATION_IN_SECONDS = 3*60;

    private boolean almostThereMessageSent = false;
    private boolean sendInitialMessage;
    private boolean sendContinuousMessage;
    private boolean sendAlmostThereMessage;
    private boolean sendArrivalMessage;
    private String phoneNumber;
    private String destination;
    private GoogleApiClient apiClient;
    private GeoApiContext geoApiContext;
    private Notification.Builder notificationBuilder;
    private ArrayList<TripSnapshot> tripSnapshots;
    private int currentReferenceSnapshotIndex = 0;
    private Converter converter;
    private long currentUpdateInterval;
    private long remainingDistanceInMeters;
    private long remainingDurationInSeconds;
    private long lastUpdateCheckTicks = -1;
    private boolean isFirstRequest = true;
    private boolean isInitialLocationFix = true;
    // If you change the update interval of the location updates you will get one immediate location fix and the next ones will be in the requested interval.
    // This can cause a delay message to be triggered when it should not happen, see: https://github.com/b0wter/ETA/issues/48
    private boolean isFirstFixAfterUpdateIntervalChanged = false;
    private BroadcastReceiver updateRequestBroadcastReceiver;
    private BroadcastReceiver sendSmsBroadcastReceiver;
    private IntervalManager intervalManager;

    public DistanceNotificationService(){
        Log.d(TAG, "Instantiating new DistanceNotificationService");
        tripSnapshots = new ArrayList<>();
        intervalManager = new IntervalManager();
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
                setExtras(extras);
                String longitude = null;
                String latitude = null;
                if(extras.containsKey(LATITUDE_EXTRA) && extras.containsKey(LONGITUDE_EXTRA)){
                    longitude = extras.getString(LONGITUDE_EXTRA);
                    latitude = extras.getString(LATITUDE_EXTRA);
                }
                start(extras.getString(PHONE_EXTRA), extras.getString(DESTINATION_EXTRA), longitude, latitude);
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

        Logger.writeToLogFile = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("enable_logging", false);
    }

    private void start(String phone, String destination, String longitude, String latitude){
        Logger.getInstance().d(TAG, "Service received COMMAND_START");
        IsServiceRunning = true;
        converter = new Converter(this);
        this.phoneNumber = phone;
        this.destination = destination;
        initService();
        showNotification();
        registerBroadcastReceivers();
    }

    private void setExtras(Bundle bundle){
        sendInitialMessage = bundle.getBoolean(SEND_INITIAL_MESSAGE_EXTRA);
        sendContinuousMessage = bundle.getBoolean(SEND_CONTINUOUS_UPDATES_EXTRA);
        sendAlmostThereMessage = bundle.getBoolean(SEND_ALMOST_THERE_MESSAGE_EXTRA);
        sendArrivalMessage = bundle.getBoolean(SEND_ARRIVAL_MESSAGE_EXTRA);
    }

    private void stop(){
        stop(false);
    }

    private void stop(boolean destinationReached){
        Logger.getInstance().d(TAG, "Service received COMMAND_STOP");
        if(apiClient != null)
            apiClient.disconnect();
        removeNotification();
        unregisterBroadcastReceivers();
        sendServiceStoppedBroadcast(destinationReached);
        Logger.getInstance().close();
        IsServiceRunning = false;
    }

    // GoogleAPIClient callback
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Logger.getInstance().d(TAG, "Connection to Google Api Client established.");
        setNewLocationListener(10);
    }

    // GoogleAPIClient callback
    @Override
    public void onConnectionSuspended(int i) {
        Logger.getInstance().i(TAG, "GoogleApiClient connection suspended.");
    }

    // GoogleAPIClient callback
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Logger.getInstance().e(TAG, "Connection to the Google Api Client failed.");
        stop();
    }

    private void registerBroadcastReceivers(){
        registerUpdateRequestBroadcastReceiver();
        registerSmsUpdateRequestBroadcastReceiver();
    }

    private void registerSmsUpdateRequestBroadcastReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(REQUEST_SEND_SMS_UPDATE);
        sendSmsBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                sendTripSms();
            }
        };
        registerReceiver(sendSmsBroadcastReceiver, filter);
    }

    private void registerUpdateRequestBroadcastReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(REQUEST_STATUS_BROADCAST);
        updateRequestBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                sendUpdateBroadcast(remainingDurationInSeconds, remainingDistanceInMeters);
            }
        };
        registerReceiver(updateRequestBroadcastReceiver, filter);
    }

    private void unregisterBroadcastReceivers(){
        unregisterSmsUpdateRequestBroadcastReceiver();
        unregisterUpdateRequestBroadcastReceiver();
    }
    private void unregisterSmsUpdateRequestBroadcastReceiver(){
        if(sendSmsBroadcastReceiver != null){
            unregisterReceiver(sendSmsBroadcastReceiver);
            sendSmsBroadcastReceiver = null;
        }
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
        Logger.getInstance().d(TAG, "setNewLocationListener");
        currentUpdateInterval = interval;
        interval *= 1000;
        LocationRequest request = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(interval).setFastestInterval(interval - 5);
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Logger.getInstance().i(TAG, "Setting new LocationListener with an interval of " + interval + "ms.");
            LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, request, this);
            isFirstFixAfterUpdateIntervalChanged = true;
        }
        else
            Logger.getInstance().e(TAG, "Cannot use the FusedLocationApi because the permission was not granted!");
    }

    /**
     * Callback function for the FusedLocationService.
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {
        Logger.getInstance().i(TAG, "A location update has been received.");
        if(isFirstFixAfterUpdateIntervalChanged && isFirstRequest == false)
            isFirstFixAfterUpdateIntervalChanged = false;
        else
            computeRemainingDistanceAndTime(location);
    }

    /**
     * Sends a distance matrix api request to get the remaining distance and duration.
     * @param location
     */
    private void computeRemainingDistanceAndTime(Location location){
        Logger.getInstance().d(TAG, "computeRemainingDistanceAndTime");
        Logger.getInstance().i(TAG, "location (lat/long): " + location.getLatitude() + "-" + location.getLongitude() + "; accuracy: " + location.getAccuracy());
        DistanceMatrixApiRequest request = DistanceMatrixApi.newRequest(geoApiContext);
        request.origins(convertLocationToLatLng(location));
        request.departureTime(new org.joda.time.DateTime());
        request.trafficModel(TrafficModel.BEST_GUESS);
        request.destinations(destination);
        try{
            Logger.getInstance().d(TAG, "Trying to send DistanceMatrixApi-request.");
            DistanceMatrix result = request.await();
            Logger.getInstance().d(TAG, "Received result from DistanceMatrixApi");
            onResult(result);
        } catch(Exception ex){
            Logger.getInstance().e(TAG, "Unable to send DistanceMatrixApi-request, error:");
            Logger.getInstance().e(TAG, ex.getMessage());

            // in case this request was the first cancel the service
            if(isFirstRequest)
                sendErrorBroadcast("Distance matrix API request failed.");
        }
    }

    /**
     * Callback for the distance matrix api requests.
     * @param result
     */
    @Override
    public void onResult(DistanceMatrix result) {
        Logger.getInstance().i(TAG, "Received distance matrix api result.");
        DistanceMatrixElement element = result.rows[0].elements[0];
        long duration = element.durationInTraffic == null ? element.duration.inSeconds : element.durationInTraffic.inSeconds;
        updateRemainingDistanceAndTime(duration, element.distance.inMeters, result.originAddresses[0], result.destinationAddresses[0]);

        // replace the given destination with the destination given by the api since that is where we are actually going
        if(!destination.equalsIgnoreCase(result.destinationAddresses[0])) {
            destination = result.destinationAddresses[0];
            sendDestinationNameBroadcast(destination);
        }

        Logger.getInstance().i(TAG, "Current location: " + result.originAddresses[0] + "; destination: " + result.destinationAddresses[0]);

        sendTripStartSmsIfNeeded();
        sendTripSmsIfNeeded();
        sendAlmostThereSmsIfNeeded();

        if(hasReachedDestination()) {
            Logger.getInstance().i(TAG, "Destination has been reached.");
            onReachedDestination();
        }
        else {
            Logger.getInstance().i(TAG, "Destination has not yet been reached.");
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
        Logger.getInstance().d(TAG, "sendTripSmsIfNeeded");
        if(tripSnapshots.size() < 2) {
            Logger.getInstance().i(TAG, "there is only a single tripLocationSnapshot available, quitting function");
            return;
        }

        if(hasArrivalTimeChanged()) {
            sendArrivalTimeChangedSms();
            currentReferenceSnapshotIndex = tripSnapshots.size()-1;
        }
    }

    private void sendAlmostThereSmsIfNeeded(){
        Logger.getInstance().d(TAG, "sendAlmostThereSmsIfNeeded");
        if(tripSnapshots.size()>0){
            Logger.getInstance().d(TAG, "remaining distance: " + tripSnapshots.get(tripSnapshots.size()-1).getRemainingDurationInSeconds() + "; almost there distance: " + ALMOST_THERE_DURATION_IN_SECONDS);
            if(tripSnapshots.get(tripSnapshots.size()-1).getRemainingDurationInSeconds() < ALMOST_THERE_DURATION_IN_SECONDS)
                if(!almostThereMessageSent) {
                    sendAlmostThereSms();
                    almostThereMessageSent = true;
                }
        }
    }

    private boolean hasArrivalTimeChanged(){
        Logger.getInstance().d(TAG, "hasArrivalTimeChanged");

        TripSnapshot reference = tripSnapshots.get(currentReferenceSnapshotIndex);
        TripSnapshot last = tripSnapshots.get(tripSnapshots.size()-1);
        TripSnapshot secondToLast = tripSnapshots.get(tripSnapshots.size()-2);

        boolean lastSnapshotWithinBounds = isWithinBounds(reference, last);
        Logger.getInstance().i(TAG, "lastSnapshotWithinBounds: " + ((Boolean)lastSnapshotWithinBounds).toString());
        boolean secondToLastSnapshotWithinBounds = isWithinBounds(reference, secondToLast);
        Logger.getInstance().i(TAG, "secondToLastSnapshotWithinBounds: " + ((Boolean)secondToLastSnapshotWithinBounds).toString());
        boolean timeChanged = (!lastSnapshotWithinBounds) && (!secondToLastSnapshotWithinBounds);
        Logger.getInstance().i(TAG, "timechanged: " + ((Boolean)timeChanged).toString());

        return timeChanged;
    }

    private boolean isWithinBounds(TripSnapshot reference, TripSnapshot snapshot){
        Logger.getInstance().d(TAG, "isWithinBounds");
        Logger.getInstance().i(TAG, "reference.estimatedArrivalTime: " + new Date(reference.getEstimatedArrivalTime()).toString() + "; raw: " + reference.getEstimatedArrivalTime());
        Logger.getInstance().i(TAG, "snapshot.estimatedArrivalTime: " + new Date(snapshot.getEstimatedArrivalTime()).toString() + "; raw: " + snapshot.getEstimatedArrivalTime());

        long arrivalTimeDifference = (Math.abs(reference.getEstimatedArrivalTime() - snapshot.getEstimatedArrivalTime())/1000);
        long remainingTime = snapshot.getRemainingDurationInSeconds();

        Logger.getInstance().i(TAG, "snapshot.remainingDurationInSeconds: " + snapshot.getRemainingDurationInSeconds());

        long maxDifference = intervalManager.getIntervalForRemainingTime(remainingTime).getMaxDelay();

        Logger.getInstance().i(TAG, "Comparing arrival times: arrivalTimeDifference=" + arrivalTimeDifference + ", maxDifference=" + maxDifference);
        return arrivalTimeDifference < maxDifference;
    }

    /**
     * Checks if the update interval needs to change and triggers the setting of a new LocationListener.
     * Does not set an interval that is larger than it previously was.
     */
    private void updateLocationListener(){
        long neededInterval = computeUpdateInterval(remainingDurationInSeconds);
        Logger.getInstance().d(TAG, "Required update interval: " + neededInterval + "; current interval: " + currentUpdateInterval);

        if(isInitialLocationFix){
            Logger.getInstance().d(TAG, "Service is handling its first requests and is allowed to set higher intervals for the location updates");
            currentUpdateInterval = Long.MAX_VALUE;
            isInitialLocationFix = false;
        }

        if(currentUpdateInterval > neededInterval){
            Logger.getInstance().d(TAG, "Updating LocationListener interval");
            LocationServices.FusedLocationApi.removeLocationUpdates(apiClient, this);
            setNewLocationListener(neededInterval);
        }
    }

    /**
     * Gets triggered once the destination reached requirements are met. Is responsible for stopping the service,
     * clearing the notification, etc..
     * @return true, if destination was reached, false if not
     */
    private boolean hasReachedDestination(){
        boolean distanceCheck = remainingDistanceInMeters < TARGET_DESTINATION_RADIUS_IN_METERS;
        boolean durationCheck = remainingDurationInSeconds < TARGET_DURATION_LOWER_LIMIT_IN_SECONDS;
        return distanceCheck || durationCheck;
    }

    /**
     * Callback for the distance matrix api requests.
     * @param e
     */
    @Override
    public void onFailure(Throwable e) {
        Logger.getInstance().e(TAG, "A distance matrix api call has failed. Reason: " + e.getMessage());
        setRetryLocationListener();
    }

    /**
     * Sets the location listener again in case it failed.
     */
    private void setRetryLocationListener(){
        long defaultUpdateInterval = computeUpdateInterval(remainingDurationInSeconds);
        long interval = Math.min(defaultUpdateInterval, MAX_API_RETRY_INTERVAL_IN_SECONDS);
        Logger.getInstance().i(TAG, "Scheduling a new LocationListener with an interval of " + String.valueOf(interval) + " seconds.");
        setNewLocationListener(interval);
    }

    private void sendStartTripSms(){
        if(!sendInitialMessage)
            return;

        Logger.getInstance().d(TAG, "sendStartTripSms");
        String text = getString(R.string.startTripSms);
        sendSms(text);
    }

    private void sendTripSms(){
        Logger.getInstance().d(TAG, "sendTripSms");
        String text = getString(R.string.duringTripSms);
        sendSms(text);
    }

    private void sendArrivalTimeChangedSms(){
        if(!sendContinuousMessage)
            return;

        Logger.getInstance().d(TAG, "sendArrivalTimeChangedSms");
        String text = getString(R.string.arrivalTimeChangedSms);
        sendSms(text);
    }

    private void sendAlmostThereSms(){
        Logger.getInstance().d(TAG, "sendAlmostThereMessage sent? " + String.valueOf(sendAlmostThereMessage));
        if(!sendAlmostThereMessage)
            return;

        Logger.getInstance().d(TAG, "sendAlmostThereSms");
        String text = getString(R.string.almostThereSms);
        sendSms(text);
    }

    private void sendArrivalSms(){
        if(!sendArrivalMessage)
            return;

        Logger.getInstance().d(TAG, "sendArrivalSms");
        String text = getString(R.string.arrivalSms);
        sendSms(text);
    }

    private void sendSms(String text){
        Logger.getInstance().d(TAG, "sendSms");
        text = fillSmsTemplate(text);
        SmsManager sms = SmsManager.getDefault();
        if(text.length() <= 160)
            sms.sendTextMessage(phoneNumber, null, text, null, null);
        else{
            ArrayList<String> parts = sms.divideMessage(text);
            sms.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
        }
    }

    private String fillSmsTemplate(String text){
        text = text.replace("%%DESTINATION%%", destination);
        text = text.replace("%%DURATION%%", converter.formatDuration(remainingDurationInSeconds));
        text = text.replace("%%ARRIVAL%%", converter.formatArrivalTime(remainingDurationInSeconds));
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

        Intent pendingAppIntent = new Intent(this, MainActivity.class);
        pendingAppIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent appIntent = PendingIntent.getActivity(this, 0, pendingAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder = new Notification.Builder(getApplicationContext())
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.distancenotificationservice_is_initializing_notification_message))
                .setSmallIcon(R.drawable.ic_directions_car_white_24dp)
                .setWhen(System.currentTimeMillis())
                .addAction(R.drawable.ic_stop_white_24dp, getString(R.string.stopCapital), serviceIntent)
                .setContentIntent(appIntent)
                .setOngoing(true);

        startForeground(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void updateNotification(){
        String distance = converter.formatDistance(remainingDistanceInMeters);
        String duration = converter.formatDuration(remainingDurationInSeconds);

        Date date = new Date(lastUpdateCheckTicks);
        String lastCheck = SimpleDateFormat.getTimeInstance().format(date);

        Date arrivalTime = new Date(System.currentTimeMillis() + remainingDurationInSeconds *1000);
        String arrivalTimeString = SimpleDateFormat.getTimeInstance().format(arrivalTime);

        String content = getString(R.string.distancenotificationservice_is_running_notification_message);
        String longContent = getString(R.string.distancenotificationservice_is_running_notification_message_details);

        content = content.replace("%%DISTANCE%%", distance).replace("%%DURATION%%", duration).replace("%%LASTCHECK%%", lastCheck).replace("%%ARRIVAL%%", arrivalTimeString);
        longContent = longContent.replace("%%DISTANCE%%", distance).replace("%%DURATION%%", duration).replace("%%LASTCHECK%%", lastCheck).replace("%%ARRIVAL%%", arrivalTimeString);

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
        return intervalManager.getIntervalForRemainingTime(secondsRemaining).getLocationUpdates();
    }

    private void updateRemainingDistanceAndTime(long durationInSeconds, long distanceInMeters, String position, String destination){
        this.remainingDurationInSeconds = durationInSeconds;
        this.remainingDistanceInMeters = distanceInMeters;
        this.lastUpdateCheckTicks = System.currentTimeMillis();
        updateNotification();
        TripSnapshot newSnapshot = new TripSnapshot(System.currentTimeMillis(), remainingDistanceInMeters, remainingDurationInSeconds, position, destination);
        tripSnapshots.add(newSnapshot);
        sendUpdateBroadcast(durationInSeconds, distanceInMeters);
    }

    private void sendUpdateBroadcast(long durationInSeconds, long distanceInMeters){
        Logger.getInstance().d(TAG, "Sending broadcast: " + TripActivity.SERVICE_BROADCAST_ACTION);
        Intent intent = new Intent(TripActivity.SERVICE_BROADCAST_ACTION);
        intent.putExtra(TripActivity.SERVICE_UPDATE_TIME_KEY, durationInSeconds);
        intent.putExtra(TripActivity.SERVICE_UPDATE_DISTANCE_KEY, distanceInMeters);
        sendBroadcast(intent);
    }

    private void sendDestinationNameBroadcast(String destination){
        Logger.getInstance().d(TAG, "Sending broadcast: " + REQUEST_DESTINATION_NAME);
        Intent intent = new Intent(REQUEST_DESTINATION_NAME);
        intent.putExtra(DESTINATION_NAME_REQUEST_NAME_EXTRA, destination);
        sendBroadcast(intent);
    }

    private void sendServiceStoppedBroadcast(boolean destinationReached){
        Logger.getInstance().d(TAG, "Sending broadcast: " + SERVICE_STOPPED_BROADCAST);
        Intent intent = new Intent(SERVICE_STOPPED_BROADCAST);
        intent.putExtra(SERVICE_STOPPED_BROADCAST_SUCCESS_EXTRA, destinationReached);
        sendBroadcast(intent);
    }

    private void sendErrorBroadcast(String message){
        Logger.getInstance().d(TAG, "Sending broadcast: " + SERVICE_ENCOUNTERED_ERROR);
        Intent intent = new Intent(SERVICE_ENCOUNTERED_ERROR);
        intent.putExtra(SERVICE_ERROR_REASON_EXTRA, message);
        sendBroadcast(intent);
    }
}
