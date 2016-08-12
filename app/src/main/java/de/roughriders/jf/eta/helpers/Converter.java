package de.roughriders.jf.eta.helpers;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.roughriders.jf.eta.R;

/**
 * Created by evil- on 6/25/2016.
 */
public class Converter {

    private Context context;
    private String hour, hours, minute, minutes;
    private String meters, km;

    public Converter(Context context){
        hour = context.getString(R.string.hour);
        hours = context.getString(R.string.hours);
        minute = context.getString(R.string.minute);
        minutes = context.getString(R.string.minutes);
        meters = context.getString(R.string.meters);
        km = context.getString(R.string.km);
    }

    /**
     * Transforms a distance into a string formatted in the current locale.
     * @param distanceInMeters
     * @return
     */
    public String formatDistance(long distanceInMeters){
        String distance;
        if(distanceInMeters <= 5000)
            distance = String.valueOf(distanceInMeters) + " " + meters;
        else if( distanceInMeters <= 15000)
            distance = String.valueOf((int)(distanceInMeters/1000)) + km;
        else
            distance = String.valueOf((int)(distanceInMeters/1000)) + km;
        return distance;
    }

    /**
     * Transforms a duration into a string formatted in the current locale.
     * @param durationInSeconds
     * @return
     */
    public String formatDuration(long durationInSeconds){
        if(durationInSeconds <= 90*60){
            long duration = durationInSeconds/60;
            return String.valueOf(duration) + " " + (duration == 1 ? minute : minutes);
        }
        else {
            return String.format("%.2f", (float) (durationInSeconds) / 60 / 60) + " " + hours;
        }
    }

    /**
     * Creates a String that represents an arrival time in the current locale.
     * @param remainingDuractionInSeconds
     * @return
     */
    public String formatArrivalTime(long remainingDuractionInSeconds){
        Date arrivalTime = new Date(System.currentTimeMillis() + remainingDuractionInSeconds*1000);
        String arrivalTimeString = SimpleDateFormat.getTimeInstance().format(arrivalTime);
        return arrivalTimeString;
    }
}
