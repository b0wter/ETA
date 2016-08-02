package de.roughriders.jf.eta.models;

import de.roughriders.jf.eta.helpers.Logger;

/**
 * Created by evil- on 05-Jul-16.
 */
public class TripSnapshot {

    private static final String TAG  = "TripSnapshot";

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        Logger.getInstance().i(TAG, "Changing time to " + time);
        this.time = time;
    }

    public long getRemainingDistanceInMeters() {
        return remainingDistanceInMeters;
    }

    public void setRemainingDistanceInMeters(long remainingDistanceInMeters) {
        Logger.getInstance().i(TAG, "Changing remainingDistanceInMeters to " + remainingDistanceInMeters);
        this.remainingDistanceInMeters = remainingDistanceInMeters;
    }

    public long getRemainingDurationInSeconds() {
        return remainingDurationInSeconds;
    }

    public void setRemainingDurationInSeconds(long remainingDurationInSeconds) {
        Logger.getInstance().i(TAG, "Changing remainingDurationInSeconds to " + remainingDurationInSeconds);
        this.remainingDurationInSeconds = remainingDurationInSeconds;
    }

    public long getEstimatedArrivalTime() {
        return estimatedArrivalTime;
    }

    public void setEstimatedArrivalTime(long estimatedArrivalTime) {
        Logger.getInstance().i(TAG, "Changing estimatedArrivalTime to " + estimatedArrivalTime);
        this.estimatedArrivalTime = estimatedArrivalTime;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        Logger.getInstance().i(TAG, "Changing position to " + position);
        this.position = position;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        Logger.getInstance().i(TAG, "Changing destination to " + destination);
        this.destination = destination;
    }

    private long time;
    private long remainingDistanceInMeters;
    private long remainingDurationInSeconds;
    private long estimatedArrivalTime;
    private String position;
    private String destination;

    public TripSnapshot(long time, long remainingDistanceInMeters, long remainingDurationInSeconds, String position, String destination){
        this.time = time;
        this.remainingDistanceInMeters = remainingDistanceInMeters;
        this.remainingDurationInSeconds = remainingDurationInSeconds;
        this.position = position;
        this.destination = destination;
        this.estimatedArrivalTime = time + remainingDurationInSeconds*1000;
    }
}
