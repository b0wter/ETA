package de.roughriders.jf.eta.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.roughriders.jf.eta.helpers.Logger;

/**
 * Created by b0wter on 6/11/16.
 */
public class RecentTrip {

    private static final String SERIALIZATION_FIELD_DELIMITER = "#";
    private static final String SHARED_PREFERENCES_KEY = "recentTrips";

    public Date date;
    public RecentDestination destination;
    public Contact contact;

    public RecentTrip(long date, RecentDestination destination, Contact contact){
        this.date = new Date(date);
        this.destination = destination;
        this.contact = contact;
    }

    /**
     * Creates a RecentTrip instance from a string.
     * @param s Serialized RecentTrip, needs to be of the format: $DATE_AS_UNIX_TIMESTAMP#$SERIALIZED_DESTINATION#$SERILIZED_CONTACT
     * @return
     */
    public static RecentTrip fromString(String s){
        String[] parts = s.split(SERIALIZATION_FIELD_DELIMITER);
        if(parts.length != 3)
            throw new IllegalArgumentException("The string does not conform to the definition of a serialized RecentTrip.");
        long epoch = Long.parseLong(parts[0]);
        RecentDestination destination = RecentDestination.fromString(parts[1]);
        Contact contact = Contact.fromString(parts[2]);
        return new RecentTrip(epoch, destination, contact);
    }

    public static ArrayList<RecentTrip> getFromSharedPreferences(Context context){
        try {
            final Gson gson = new Gson();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            ArrayList<RecentTrip> recentTrips = new ArrayList<RecentTrip>();
            if (prefs.contains(SHARED_PREFERENCES_KEY)) {
                Set<String> set = prefs.getStringSet(SHARED_PREFERENCES_KEY, new HashSet<String>());
                for (String element : set)
                    recentTrips.add(gson.fromJson(element, RecentTrip.class));
                return recentTrips;
            } else
                return recentTrips;
        } catch(JsonSyntaxException ex){
            Logger.getInstance().w("RecentTrip", "There was a problem deserializing data stored in the shared preferences. Will remove all.\r\n" + ex.getMessage());
            clearFromSharedPreferences(context);
            return new ArrayList<>();
        }
    }

    public static void saveToSharedPreferences(List<RecentTrip> trips, Context context){
        final Gson gson = new Gson();
        HashSet<String> set = new HashSet<>(trips.size());
        for(RecentTrip trip : trips)
            set.add(gson.toJson(trip));
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putStringSet(SHARED_PREFERENCES_KEY, set);
        editor.apply();
    }

    public static void clearFromSharedPreferences(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(prefs.contains(SHARED_PREFERENCES_KEY)){
            prefs.edit().remove(SHARED_PREFERENCES_KEY).apply();
        }
    }

    @Override
    public String toString(){
        if(date == null)
            date = new Date(0);
        if(destination == null)
            destination = new RecentDestination("unknown", "unknown");
        if(contact == null)
            contact = new Contact("unknown", "unknown");
        return String.valueOf(date.getTime()) + SERIALIZATION_FIELD_DELIMITER + destination.toString() + SERIALIZATION_FIELD_DELIMITER + contact.toString();
    }
}
