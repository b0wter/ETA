package de.roughriders.jf.eta.models;

/**
 * Created by evil- on 28-Jul-16.
 */
public class Trip {
    public long id;
    public long timestamp;

    public Trip(long id){
        this.id = id;
        this.timestamp = System.currentTimeMillis();
    }

    public Trip(long id, long timestamp){
        this.id = id;
        this.timestamp = timestamp;
    }
}
