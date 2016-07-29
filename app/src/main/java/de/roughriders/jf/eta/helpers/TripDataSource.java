package de.roughriders.jf.eta.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import de.roughriders.jf.eta.models.Trip;

/**
 * Created by evil- on 28-Jul-16.
 */
public class TripDataSource {

    private SQLiteDatabase database;
    private SQLiteHelper dbHelper;
    private String[] allColumns = { SQLiteHelper.TRIPS_COLUMN_ID, SQLiteHelper.TRIPS_COLUMN_TIMESTAMP };

    public TripDataSource(Context context){
        dbHelper = new SQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close(){
        database.close();
    }

    public Trip createTrip(){
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.TRIPS_COLUMN_TIMESTAMP, System.currentTimeMillis());
        long insertId = database.insert(SQLiteHelper.TABLE_TRIPS, null, values);
        Cursor cursor = database.query(SQLiteHelper.TABLE_TRIPS, allColumns, SQLiteHelper.TRIPS_COLUMN_ID + " = " + insertId, null, null, null, null);
        cursor.moveToFirst();
        Trip trip = cursorToTrip(cursor);
        return trip;
    }

    private Trip cursorToTrip(Cursor cursor){
        long id = cursor.getLong(0);
        long time = cursor.getLong(1);
        Trip trip = new Trip(id, time);
        return trip;
    }
}
