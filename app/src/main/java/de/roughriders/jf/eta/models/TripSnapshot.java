package de.roughriders.jf.eta.models;

/**
 * Created by evil- on 05-Jul-16.
 */
public class TripSnapshot {
    public long id;
    public long time;
    public long remainingDistanceInMeters;
    public long remainingDurationInSeconds;
    public String position;
    public String destination;
    private float distanceTimeRatio;
    public long tripId;

    public TripSnapshot(long id, long time, long remainingDistanceInMeters, long remainingDurationInSeconds, String position, String destination, long tripId){
        this.time = time;
        this.remainingDistanceInMeters = remainingDistanceInMeters;
        this.remainingDurationInSeconds = remainingDurationInSeconds;
        this.position = position;
        this.destination = destination;
        if(remainingDurationInSeconds != 0)
            this.distanceTimeRatio = remainingDistanceInMeters / remainingDurationInSeconds;
        else
            this.distanceTimeRatio = 0;
        this.tripId = tripId;
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
