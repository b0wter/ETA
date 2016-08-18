package de.roughriders.jf.eta.models;

/**
 * Holds all the settings in information for a single time interval.
 */
public class Interval {
    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public int getLocationUpdates() {
        return locationUpdates;
    }

    public void setLocationUpdates(int locationUpdates) {
        this.locationUpdates = locationUpdates;
    }

    public int getMaxDelay() {
        return maxDelay;
    }

    public void setMaxDelay(int maxDelay) {
        this.maxDelay = maxDelay;
    }

    // lower limit of this interval's remaining duration
    private int min;
    // upper limit of this interval's remaining duration
    private int max;
    // interval for location updates
    private int locationUpdates;
    // maximum time difference for sending a delay sms
    private int maxDelay;

    public Interval(int min, int max, int locationUpdates, int maxDelay){
        this.min = min;
        this.max = max;
        this.locationUpdates = locationUpdates;
        this.maxDelay = maxDelay;
    }
}
