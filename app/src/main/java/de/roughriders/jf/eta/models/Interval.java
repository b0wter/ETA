package de.roughriders.jf.eta.models;

/**
 * Created by evil- on 31-Jul-16.
 */
public class Interval {
    public int min;
    public int max;
    public int locationUpdates;
    public int maxDelay;

    public Interval(int min, int max, int locationUpdates, int maxDelay){
        this.min = min;
        this.max = max;
        this.locationUpdates = locationUpdates;
        this.maxDelay = maxDelay;
    }
}
