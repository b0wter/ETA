package de.roughriders.jf.eta.models;

import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by b0wter on 18-Aug-16.
 */
public class IntervalManager {

    private static String TAG = "IntervalManager";
    private ArrayList<Interval> intervals;

    public IntervalManager(){
        initializeIntervals();
    }

    private void initializeIntervals(){
        intervals = new ArrayList<>();
        // [0, 1.5[
        Interval i = new Interval(0, 89, 10, 60);
        intervals.add(i);
        // [1.5, 3[
        i = new Interval(90, 179, 20, 90);
        intervals.add(i);
        // [3, 5[
        i = new Interval(180, 299, 30, 90);
        intervals.add(i);
        // [5, 10[
        i = new Interval(300, 599, 45, 150);
        intervals.add(i);
        // [10, 30[
        i = new Interval(600, 1799, 60, 300);
        intervals.add(i);
        // [30, 60[
        i = new Interval(1800, 3599, 150, 459);
        intervals.add(i);
        // [60, 120[
        i = new Interval(3600, 7199, 300, 900);
        intervals.add(i);
        // [120, int.Max]
        i = new Interval(7200, Integer.MAX_VALUE, 600, 1800);
        intervals.add(i);
    }

    public Interval getIntervalForRemainingTime(long remainingTimeInSeconds){
        Log.d(TAG, "getIntervalForRemainingTime - " + String.valueOf(remainingTimeInSeconds) + " seconds remaining");
        for(int i = 0; i < intervals.size(); i++){
            if(remainingTimeInSeconds >= intervals.get(i).getMin() && remainingTimeInSeconds < intervals.get(i).getMax()) {
                Interval interval = intervals.get(i);
                Log.d(TAG, "Selected interval: [" + interval.getMin() + ", " + interval.getMax() + "]");
                Log.d(TAG, "Update times: " + interval.getLocationUpdates() + ", max delay: " + interval.getMaxDelay());
                return interval;
            }
        }
        return intervals.get(0);
    }
}
