package de.roughriders.jf.eta.models;

/**
 * Created by evil- on 05-Jul-16.
 */
public class TripSnapshot {
    public long time;
    public long remainingDistanceInMeters;
    public long remainingDurationInSeconds;
    private float distanceTimeRatio;

    public TripSnapshot(long time, long remainingDistanceInMeters, long remainingDurationInSeconds){
        this.time = time;
        this.remainingDistanceInMeters = remainingDistanceInMeters;
        this.remainingDurationInSeconds = remainingDurationInSeconds;
        if(remainingDurationInSeconds != 0)
            this.distanceTimeRatio = remainingDistanceInMeters / remainingDurationInSeconds;
        else
            this.distanceTimeRatio = 0;
    }

    public float getDistanceTimeRatio(){
        return distanceTimeRatio;
    }

    /**
     * Returns the time (unix, millisecond epoch) of arrival estimated for this snapshot.
     * @return
     */
    public long getEstimatedArrivalTime(){
        return getEstimatedArrivalTime(1);
    }

    public  long getEstimatedArrivalTime(float timeMod){
        return time + (long)(timeMod*remainingDurationInSeconds*1000);
    }
}
