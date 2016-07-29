package de.roughriders.jf.eta.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import de.roughriders.jf.eta.models.TripSnapshot;

/**
 * Created by evil- on 28-Jul-16.
 */
public class TripSnapshotDataSource {

    private SQLiteDatabase database;
    private SQLiteHelper dbHelper;
    private String[] allColumns = { SQLiteHelper.SNAPSHOT_COLUMN_ID, SQLiteHelper.SNAPSHOT_COLUMN_DESTINATION, SQLiteHelper.SNAPSHOT_COLUMN_POSITION, SQLiteHelper.SNAPSHOT_COLUMN_TIMESTAMP, SQLiteHelper.SNAPSHOT_COLUMN_REMAINING_TIME, SQLiteHelper.SNAPSHOT_COLUMN_REMAINING_TIME, SQLiteHelper.SNAPSHOT_COLUMN_TRIP_ID};

    public TripSnapshotDataSource(Context context){
        dbHelper = new SQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public TripSnapshot createTripSnapshot(long time, long remainingDistanceInMeters, long remainingDurationInSeconds, String position, String destination, long tripId){
        ContentValues values = new ContentValues();
        values.put(SQLiteHelper.SNAPSHOT_COLUMN_DESTINATION, destination);
        values.put(SQLiteHelper.SNAPSHOT_COLUMN_POSITION, position);
        values.put(SQLiteHelper.SNAPSHOT_COLUMN_REMAINING_DISTANCE, remainingDistanceInMeters);
        values.put(SQLiteHelper.SNAPSHOT_COLUMN_REMAINING_TIME, remainingDurationInSeconds);
        values.put(SQLiteHelper.SNAPSHOT_COLUMN_TIMESTAMP, time);
        values.put(SQLiteHelper.SNAPSHOT_COLUMN_TRIP_ID, tripId);
        long insertId = database.insert(SQLiteHelper.TABLE_TRIP_SNAPSHOTS, null, values);
        Cursor cursor = database.query(SQLiteHelper.TABLE_TRIP_SNAPSHOTS, allColumns, SQLiteHelper.SNAPSHOT_COLUMN_ID + " = " + insertId, null, null, null, null);
        cursor.moveToFirst();
        TripSnapshot newSnapshot = cursorToSnapshot(cursor);
        return newSnapshot;
    }

    private TripSnapshot cursorToSnapshot(Cursor cursor){
        long id = cursor.getLong(0);
        String destination = cursor.getString(1);
        String position = cursor.getString(2);
        long timestamp = cursor.getLong(3);
        long remainingTime = cursor.getLong(4);
        long remainingDistance = cursor.getLong(5);
        long tripId = cursor.getLong(6);
        TripSnapshot snapshot = new TripSnapshot(id, timestamp, remainingTime, remainingDistance, position, destination, tripId);
        return snapshot;
    }
}
