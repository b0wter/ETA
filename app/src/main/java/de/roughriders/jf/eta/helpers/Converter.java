package de.roughriders.jf.eta.helpers;

/**
 * Created by evil- on 6/25/2016.
 */
public class Converter {

    /*
    private static long shortDistanceInMeters = 5000;
    public static void setShortDistanceInMeters(long value){
        shortDistanceInMeters = value;
    }
    private static String shortDistanceFormat = "#.0";
    public static void setShortDistanceFormat(String format){
        shortDistanceFormat = format;
    }

    private static long mediumDistanceInMeters = 1500;
    public static void setMediumDistanceInMeters(long value){
        mediumDistanceInMeters = value;
    }
    private static String mediumDistanceFormat = "#";
    public static void setMediumDistanceFormat(String format){
        mediumDistanceFormat = format;
    }

    private static String longDistanceFormat = "#";
    public static void setLongDistanceFormat(String format){
        longDistanceFormat = format;
    }
    */

    private Converter(){}

    public static String formatDistance(long distanceInMeters){
        String distance;
        if(distanceInMeters <= 5000)
            distance = String.valueOf(distanceInMeters) + " meters";
        else if( distanceInMeters <= 15000)
            distance = String.valueOf((int)(distanceInMeters/1000)) + "km";
        else
            distance = String.valueOf((int)(distanceInMeters/1000)) + "km";
        return distance;
    }

    public static String formatDuration(long durationInSeconds){
        if(durationInSeconds <= 90*60)
            return String.valueOf(durationInSeconds/60) + " minutes";
        else
            return String.format("%.2f", (float)(durationInSeconds)/60/60) + " hours";
    }
}
