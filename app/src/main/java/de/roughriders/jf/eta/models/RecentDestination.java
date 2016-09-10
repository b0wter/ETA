package de.roughriders.jf.eta.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.PreferencesFactory;

import de.roughriders.jf.eta.helpers.Logger;

/**
 * Model to store and retrieve a recent destination to the SharedPreferences
 */
public class RecentDestination {
    public String primaryText = "";
    public String secondaryText = "";
    public String latitude;
    public String longitude;

    public static final String SERIALIZATION_FIELD_DELIMITER = ";";
    public static final String SHARED_PREFERENCES_KEY = "recentDestinations";

    private static final String TAG = "RecentDestination";

    public RecentDestination(String primaryText){
        this.primaryText = primaryText.trim();
    }

    public RecentDestination(String primaryText, String secondaryText){
        this.primaryText = primaryText.trim();
        this.secondaryText = secondaryText.trim();
    }

    public RecentDestination(String primaryText, String secondaryText, double latitude, double longitude){
        this(primaryText, secondaryText);
        this.latitude = String.valueOf(latitude).replace(",", ".");
        this.longitude = String.valueOf(longitude).replace(",", ".");
    }

    public static RecentDestination fromPrediction(AutocompletePrediction prediction){
        return new RecentDestination(prediction.getPrimaryText(null).toString(), prediction.getSecondaryText(null).toString());
    }

    public static RecentDestination fromString(String s){
        String[] parts = s.split(SERIALIZATION_FIELD_DELIMITER);
        if(parts.length > 3)
            throw new IllegalArgumentException("The string contains more than three parts (delimiter: " + SERIALIZATION_FIELD_DELIMITER + ". String: " + s);
        else if(parts.length == 3){
            // this is maintained for legacy reasons, prior versions of this model included a Google Places ID
            return new RecentDestination(parts[0], parts[1]);
        }
        else if(parts.length == 2){
            return new RecentDestination(parts[0], parts[1]);
        }
        else if(parts.length == 1){
            return new RecentDestination(parts[0]);
        }
        else {
            throw new IllegalArgumentException("The string does not contain any text (besides delimiters). String: " + s);
        }
    }

    public static ArrayList<RecentDestination> getFromSharedPreferences(Context context){
        try {
            final Gson gson = new Gson();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (prefs.contains(SHARED_PREFERENCES_KEY)) {
                ArrayList<RecentDestination> list = new ArrayList<>();
                Set<String> set = prefs.getStringSet(SHARED_PREFERENCES_KEY, new HashSet<String>());
                for (String element : set) {
                    list.add(gson.fromJson(element, RecentDestination.class));
                }
                return list;
            } else
                return new ArrayList<>();
        } catch(JsonSyntaxException ex){
            Logger.getInstance().w("RecentDestination", "There was a problem deserializing data stored in the shared preferences. Will remove all.\r\n" + ex.getMessage());
            clearFromSharedPreferences(context);
            return new ArrayList<>();
        }
    }

    public static void saveToSharedPreferences(List<RecentDestination> destinations, Context context){
        final Gson gson = new Gson();
        HashSet<String> set = new HashSet<>(destinations.size());
        for(RecentDestination destination : destinations)
            set.add(gson.toJson(destination));
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putStringSet(SHARED_PREFERENCES_KEY, set);
        editor.apply();
        Log.i(TAG, "Recent destinations have been saved to the SharedPreferences");
    }

    public static void clearFromSharedPreferences(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(prefs.contains(SHARED_PREFERENCES_KEY)){
            prefs.edit().remove(SHARED_PREFERENCES_KEY).apply();
        }
    }

    public String toNiceString(){
        if(!primaryText.isEmpty() && !secondaryText.isEmpty())
            return primaryText + ", " + secondaryText;
        else if(!primaryText.isEmpty() && secondaryText.isEmpty())
            return primaryText.trim();
        else if(primaryText.isEmpty() && !secondaryText.isEmpty())
            return secondaryText.trim();
        else
            return "no text available";
    }

    @Override
    public String toString(){
        if(!primaryText.isEmpty() && !secondaryText.isEmpty())
            return primaryText + SERIALIZATION_FIELD_DELIMITER + secondaryText;
        else if(!primaryText.isEmpty())
            return primaryText;
        else if(!secondaryText.isEmpty())
            return secondaryText;
        else
            return "<unknown>";
    }
}
