package de.roughriders.jf.eta.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.location.places.AutocompletePrediction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Model to store and retrieve a recent destination to the SharedPreferences
 */
public class RecentDestination {
    public String primaryText;
    public String secondaryText;
    public String placesId;

    public static final String SERIALIZATION_FIELD_DELIMITER = ";";
    public static final String SHARED_PREFERENCES_KEY = "recentDestinations";

    private static final String TAG = "RecentDestination";

    public RecentDestination(String primaryText, String secondaryText, String placesId){
        this.primaryText = primaryText;
        this.secondaryText = secondaryText;
        this.placesId = placesId;
    }

    public static RecentDestination fromPrediction(AutocompletePrediction prediction){
        return new RecentDestination(prediction.getPrimaryText(null).toString(), prediction.getSecondaryText(null).toString(), prediction.getPlaceId());
    }

    public static RecentDestination fromString(String s){
        String[] parts = s.split(SERIALIZATION_FIELD_DELIMITER);
        if(parts.length != 3)
            throw new IllegalArgumentException("The string contains more than three parts.");
        return new RecentDestination(parts[0], parts[1], parts[2]);
    }

    public static ArrayList<RecentDestination> getFromSharedPreferences(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(prefs.contains(SHARED_PREFERENCES_KEY)){
            ArrayList<RecentDestination> list = new ArrayList<>();
            Set<String> set = prefs.getStringSet(SHARED_PREFERENCES_KEY, new HashSet<String>());
            for(String element : set){
                list.add(RecentDestination.fromString(element));
            }
            return list;
        }
        else
            return new ArrayList<RecentDestination>();
    }

    public static void saveToSharedPreferences(List<RecentDestination> destinations, Context context){
        HashSet<String> set = new HashSet<>(destinations.size());
        for(RecentDestination destination : destinations)
            set.add(destination.toString());
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putStringSet(SHARED_PREFERENCES_KEY, set);
        editor.apply();
        Log.i(TAG, "Recent destinations have been saved to the SharedPreferences");
    }

    @Override
    public String toString(){
        return primaryText + SERIALIZATION_FIELD_DELIMITER + secondaryText + SERIALIZATION_FIELD_DELIMITER + placesId;
    }
}
