package de.roughriders.jf.eta.helpers;

/**
 * Created by evil- on 05-Jul-16.
 */
public class TripSnapshot {
    public long time;
    public long remainingDistanceInMeters;
    public long remainingDurationInSeconds;

    public TripSnapshot(long time, long remainingDistanceInMeters, long remainingDurationInSeconds){
        this.time = time;
        this.remainingDistanceInMeters = remainingDistanceInMeters;
        this.remainingDurationInSeconds = remainingDurationInSeconds;
    }
}
