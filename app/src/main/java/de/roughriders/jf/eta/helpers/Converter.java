package de.roughriders.jf.eta.helpers;

import android.content.Context;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.roughriders.jf.eta.R;

/**
 * Created by evil- on 6/25/2016.
 */
public class Converter {

    private Context context;
    private String hourString , hoursString , minuteString , minutesString;
    private String minAbbreviationString, hourAbbreviationString;
    private String metersString , kmString ;

    public Converter(Context context){
        this.context = context;
        hourString = context.getString(R.string.hour);
        hoursString = context.getString(R.string.hours);
        minuteString = context.getString(R.string.minute);
        minutesString = context.getString(R.string.minutes);
        minAbbreviationString = context.getString(R.string.min_abbreviation);
        hourAbbreviationString = context.getString(R.string.hour_abbreviation);
        metersString = context.getString(R.string.meters);
        kmString = context.getString(R.string.km);
    }

    /**
     * Transforms a distance into a string formatted in the current locale.
     * @param distanceInMeters
     * @return
     */
    public String formatDistance(long distanceInMeters){
        String distance;
        if(distanceInMeters <= 5000)
            distance = String.valueOf(distanceInMeters) + " " + metersString ;
        else if( distanceInMeters <= 15000)
            distance = String.valueOf((int)(distanceInMeters/1000)) + kmString ;
        else
            distance = String.valueOf((int)(distanceInMeters/1000)) + kmString ;
        return distance;
    }

    /**
     * Transforms a duration into a string formatted in the current locale.
     * @param durationInSeconds
     * @return
     */
    public String formatDuration(long durationInSeconds){
        if(durationInSeconds <= 90*60 && durationInSeconds > 120){
            long duration = durationInSeconds/60;
            return String.valueOf(duration) + " " + (duration == 1 ? minuteString  : minutesString );
        }
        else if(durationInSeconds <= 120)
        {
            float duration = ((float)durationInSeconds)/60;
            return String.format(Locale.getDefault(), "%.1f", duration) + " " + minutesString;
        }
        else {
            return String.format(Locale.getDefault(), "%.1f", ((float)durationInSeconds) / 60 / 60) + " " + hoursString;
        }
    }

    public String formatDurationWithAbbreviatedUnits(long durationInSeconds){
        if(durationInSeconds < 120){
            float minutes = ((float)durationInSeconds) / 60;
            return String.format(Locale.getDefault(), "%.1f", minutes) + " " + minAbbreviationString;
        }
        else if(durationInSeconds <= 90*60){
            int minutes = (int)(durationInSeconds/60);
            return String.valueOf(minutes) + " " + minAbbreviationString;
        }
        else{
            float hours = ((float)durationInSeconds/60/60);
            return String.format(Locale.getDefault(), "%.2f", hours) + " " + hourAbbreviationString;
        }
    }

    /**
     * Creates a String that represents an arrival time in the current locale.
     * @param remainingDurationInSeconds
     * @return
     */
    public String formatArrivalTime(long remainingDurationInSeconds){
        SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Date arrivalTime = new Date(System.currentTimeMillis() + remainingDurationInSeconds*1000);
        String arrivalTimeString = format.format(arrivalTime);
        return arrivalTimeString;
    }
}
