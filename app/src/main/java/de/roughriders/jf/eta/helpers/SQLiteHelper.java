package de.roughriders.jf.eta.helpers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by evil- on 28-Jul-16.
 */
public class SQLiteHelper extends SQLiteOpenHelper{

    private static final String DATABASE_NAME = "eta.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_TRIP_SNAPSHOTS = "tripSnapshots";
    public static final String SNAPSHOT_COLUMN_ID = "_id";
    public static final String SNAPSHOT_COLUMN_DESTINATION = "destination";
    public static final String SNAPSHOT_COLUMN_POSITION = "position";
    public static final String SNAPSHOT_COLUMN_TIMESTAMP = "time";
    public static final String SNAPSHOT_COLUMN_REMAINING_TIME = "timeRemaining";
    public static final String SNAPSHOT_COLUMN_REMAINING_DISTANCE = "distanceRemaining";
    public static final String SNAPSHOT_COLUMN_TRIP_ID = "tripId";

    public static final String TABLE_TRIPS = "trips";
    public static final String TRIPS_COLUMN_ID = "_id";
    public static final String TRIPS_COLUMN_TIMESTAMP = "time";

    private static final String CREATE_SNAPSHOT_TABLE = "create table " + TABLE_TRIP_SNAPSHOTS + "( "
            + SNAPSHOT_COLUMN_ID + " integer primary key autoincrement, "
            + SNAPSHOT_COLUMN_DESTINATION + " text not null, "
            + SNAPSHOT_COLUMN_POSITION + " text not null, "
            + SNAPSHOT_COLUMN_TIMESTAMP + " integer not null, "
            + SNAPSHOT_COLUMN_REMAINING_TIME + " integer not null, "
            + SNAPSHOT_COLUMN_REMAINING_DISTANCE + " integer not null, "
            + SNAPSHOT_COLUMN_TRIP_ID + " integer not null);";

    private static final String CREATE_TRIPS_TABLE = "create table " + TABLE_TRIPS + "( "
            + TRIPS_COLUMN_ID + " integer primary key autoincrement, "
            + TRIPS_COLUMN_TIMESTAMP + " integer not null);";

    public SQLiteHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database){
        database.execSQL(CREATE_SNAPSHOT_TABLE);
        database.execSQL(CREATE_TRIPS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion){
        Log.w(SQLiteHelper.class.getName(), "Upgrading database from version " + oldVersion + " to version " + newVersion + ". This will destroy all data.");
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_TRIP_SNAPSHOTS);
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_TRIPS);
        onCreate(database);
    }
}
