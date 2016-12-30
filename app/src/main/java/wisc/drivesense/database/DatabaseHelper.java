package wisc.drivesense.database;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.Trace;
import wisc.drivesense.utility.Trip;
import wisc.drivesense.utility.User;


public class DatabaseHelper {

    // Logcat tag
    private static final String TAG = "DatabaseHelper";

    private SQLiteDatabase meta_ = null;
    private SQLiteDatabase db_ = null;


    // Database Version
    private static final String DATABASE_NAME = "summary.db";

    private static final String TABLE_META = "meta";
    private static final String CREATE_TABLE_META = "CREATE TABLE IF NOT EXISTS "
            + TABLE_META + "(starttime INTEGER, endtime INTEGER, distance REAL, score REAL, deleted INTEGER, uploaded INTEGER, email TEXT, orientation_change INTEGER, stability REAL);";

    private static final String TABLE_USER = "user";
    private static final String CREATE_TABLE_USER = "CREATE TABLE IF NOT EXISTS "
            + TABLE_USER + "(email TEXT, firstname TEXT, lastname TEXT, password TEXT, loginstatus INTEGER);";

    /*
    private static final String TABLE_BEHAVIOR = "behavior";
    private static final String CREATE_TABLE_BEHAVIOR = "CREATE TABLE IF NOT EXISTS "
            + TABLE_BEHAVIOR + "(triptime INTEGER, begintime INTEGER, finishtime INTEGER, behavior TEXT, sensor INTEGER);";
    */

    // Table Names
    private static final String TABLE_ACCELEROMETER = "accelerometer";
    private static final String TABLE_GYROSCOPE = "gyroscope";
    private static final String TABLE_MAGNETOMETER = "magnetometer";
    private static final String TABLE_ROTATION_MATRIX = "rotation_matrix";
    private static final String TABLE_GPS = "gps";


    private static final String KEY_TIME = "time";

    /*rotation matrix*/
    private static final String KEY_VALUES[] = {"x0", "x1", "x2", "x3", "x4", "x5", "x6", "x7", "x8"};


    // Table Create Statements

    private static final String CREATE_TABLE_GPS = "CREATE TABLE IF NOT EXISTS "
            + TABLE_GPS + "(" + KEY_TIME + " INTEGER PRIMARY KEY," + KEY_VALUES[0] + " REAL,"
            + KEY_VALUES[1] + " REAL," +  KEY_VALUES[2] + " REAL," + KEY_VALUES[3] + " REAL,"
            + KEY_VALUES[4] + " REAL," + KEY_VALUES[5] + " REAL, " + KEY_VALUES[6] + " REAL"
            + ");";
    private static final String CREATE_TABLE_ACCELEROMETER = "CREATE TABLE IF NOT EXISTS "
            + TABLE_ACCELEROMETER + "(" + KEY_TIME + " INTEGER PRIMARY KEY," + KEY_VALUES[0]
            + " REAL," + KEY_VALUES[1] + " REAL," +  KEY_VALUES[2] + " REAL" + ");";
    private static final String CREATE_TABLE_GYROSCOPE = "CREATE TABLE IF NOT EXISTS "
            + TABLE_GYROSCOPE + "(" + KEY_TIME + " INTEGER PRIMARY KEY," + KEY_VALUES[0]
            + " REAL," + KEY_VALUES[1] + " REAL," +  KEY_VALUES[2] + " REAL" + ");";
    private static final String CREATE_TABLE_MAGNETOMETER = "CREATE TABLE IF NOT EXISTS "
            + TABLE_MAGNETOMETER + "(" + KEY_TIME + " INTEGER PRIMARY KEY," + KEY_VALUES[0]
            + " REAL," + KEY_VALUES[1] + " REAL," +  KEY_VALUES[2] + " REAL" + ");";


    private static final String CREATE_TABLE_ROTATION_MATRIX = "CREATE TABLE IF NOT EXISTS "
            + TABLE_ROTATION_MATRIX + "(" + KEY_TIME + " INTEGER PRIMARY KEY,"
            + KEY_VALUES[0] + " REAL," + KEY_VALUES[1] + " REAL," +  KEY_VALUES[2] + " REAL,"
            + KEY_VALUES[3] + " REAL," + KEY_VALUES[4] + " REAL," +  KEY_VALUES[5] + " REAL,"
            + KEY_VALUES[6] + " REAL," + KEY_VALUES[7] + " REAL," +  KEY_VALUES[8] + " REAL"
            + ")";



    private boolean opened = false;
    // public interfaces
    public DatabaseHelper() {
        this.opened = true;
        //this.context = cont;
        //openOrCreateDatabase(DATABASE_NAME, SQLiteDatabase.CREATE_IF_NECESSARY, null);
        //File dir = this.context.getFilesDir();
        meta_ = SQLiteDatabase.openOrCreateDatabase(Constants.kDBFolder + DATABASE_NAME, null, null);
        meta_.execSQL(CREATE_TABLE_META);
        //we never close meta_ explicitly, maybe

        //create user table
        meta_.execSQL(CREATE_TABLE_USER);

    }


    //open and close for each trip
    public void createDatabase(long t) {
        this.opened = true;
        db_ = SQLiteDatabase.openOrCreateDatabase(Constants.kDBFolder + String.valueOf(t).concat(".db"), null, null);
        db_.execSQL(CREATE_TABLE_ACCELEROMETER);
        db_.execSQL(CREATE_TABLE_GYROSCOPE);
        db_.execSQL(CREATE_TABLE_MAGNETOMETER);
        db_.execSQL(CREATE_TABLE_GPS);
        db_.execSQL(CREATE_TABLE_ROTATION_MATRIX);
    }


    public void closeDatabase() {
        this.opened = false;
        if(meta_ != null && meta_.isOpen()) {
            meta_.close();
        }
        if(db_ != null && db_.isOpen()) {
            db_.close();
        }
    }
    public boolean isOpen() {
        return this.opened;
    }

    public void insertTrip(Trip trip) {
        Gson gson = new Gson();
        Log.d(TAG, "insertTrip" + gson.toJson(trip));
        ContentValues values = new ContentValues();
        values.put("starttime", trip.getStartTime());
        values.put("endtime", trip.getEndTime());
        values.put("distance", trip.getDistance());
        values.put("score", trip.getScore());
        values.put("deleted", 0);
        values.put("uploaded", 0);
        //assign to current user
        values.put("email", "");
        values.put("orientation_change", trip.numberOfOrientationChanges_);
        values.put("stability", trip.mountingStability_);

        meta_.insert(TABLE_META, null, values);
    }


    public void insertSensorData(Trace trace) {
        String type = trace.type;
        ContentValues values = new ContentValues();
        values.put(KEY_TIME, trace.time);
        for(int i = 0; i < trace.dim; ++i) {
            values.put(KEY_VALUES[i], trace.values[i]);
        }
        if (type.equals(Trace.ROTATION_MATRIX)) {
            db_.insert(TABLE_ROTATION_MATRIX, null, values);
        } else if (type.equals(Trace.ACCELEROMETER)) {
            db_.insert(TABLE_ACCELEROMETER, null, values);
        } else if (type.equals(Trace.GYROSCOPE)) {
            db_.insert(TABLE_GYROSCOPE, null, values);
        } else if (type.equals(Trace.MAGNETOMETER)) {
            db_.insert(TABLE_MAGNETOMETER, null, values);
        } else if (type.equals(Trace.GPS)) {
            db_.insert(TABLE_GPS, null, values);
        } else {
            assert 0 == 1;
        }
    }



    /**
     * @brief get the gps points of a trip, which is identified by the start time (the name of the database)
     * @param time the start time of a trip (also the name of the database)
     * @return a list of trace, or gps points
     */
    public List<Trace> getGPSPoints(long time) {
        SQLiteDatabase tmpdb = SQLiteDatabase.openDatabase(Constants.kDBFolder + String.valueOf(time).concat(".db"), null, SQLiteDatabase.OPEN_READONLY);
        List<Trace> res = new ArrayList<Trace>();
        String selectQuery = "SELECT  * FROM " + TABLE_GPS;
        Cursor cursor = tmpdb.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        do {
            if(cursor.getCount() == 0) {
                break;
            }
            Trace trace = new Trace(6);
            trace.time = cursor.getLong(0);
            for(int i = 0; i < 6; ++i) {
                trace.values[i] = cursor.getFloat(i + 1);
            }
            trace.type = Trace.GPS;
            res.add(trace);
        } while (cursor.moveToNext());
        tmpdb.close();
        return res;
    }

    private Trip constructTripByCursor(Cursor cursor, boolean withgps) {
        long stime = cursor.getLong(0);
        long etime = cursor.getLong(1);
        double dist = cursor.getDouble(2);
        double score = cursor.getDouble(3);
        int deleted = cursor.getInt(4);

        int orientation_changed = cursor.getInt(7);
        double stability = cursor.getDouble(8);
        Trip trip = new Trip(stime);
        trip.setScore(score);
        trip.setStatus(deleted == 1? 0 : 1);
        trip.setEndTime(etime);
        trip.setDistance(dist);

        trip.numberOfOrientationChanges_ = orientation_changed;
        trip.mountingStability_ = stability;
        if(withgps) {
            trip.setGPSPoints(this.getGPSPoints(stime));
        }

        return trip;
    }

    public Trip getTrip(long time) {
        String selectQuery = "SELECT  * FROM " + TABLE_META + " WHERE starttime = " + time + ";";
        Cursor cursor = meta_.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        Trip trip = null;
        do {
            if(cursor.getCount() == 0) {
                break;
            }
            trip = constructTripByCursor(cursor, true);
        } while (cursor.moveToNext());
        return trip;
    }

    /**
     * @brief remove the record of the table, so that the user cannot see it
     * but the file is still in the database
     * @param time
     */
    public void removeTrip(long time) {
        ContentValues data = new ContentValues();
        data.put("deleted", 1);
        String where = "starttime = ? ";
        String[] whereArgs = {String.valueOf(time)};
        meta_.update(TABLE_META, data, where, whereArgs);
    }

    /**
     * @  only if the trip is imcomplete
     * @param time
     */

    public void deleteTrip(long time) {
        Log.d(TAG, "deleteTrip:" + time);
        SQLiteDatabase.deleteDatabase(new File(Constants.kDBFolder + String.valueOf(time).concat(".db")));
    }


    public List<Trip> loadTrips() {
        List<Trip> trips = new ArrayList<Trip>();
        String selectQuery = "SELECT  * FROM " + TABLE_META + " WHERE email = '"+"' order by starttime desc;";
        Cursor cursor = meta_.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        if(cursor.getCount() == 0) {
            return trips;
        }
        do {
            int deleted = cursor.getInt(4);
            if(deleted >= 1) {
                continue;
            }
            Trip trip = constructTripByCursor(cursor, false);
            trips.add(trip);
            if(trips.size() >= Constants.kNumberOfTripsDisplay) {
                break;
            }
        } while (cursor.moveToNext());
        return trips;
    }


    public long[] tripsToSynchronize () {
        String selectQuery = "SELECT  * FROM " + TABLE_META + " WHERE uploaded = 1 and deleted = 1;";
        Cursor cursor = meta_.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        if(cursor.getCount() == 0) {
            return null;
        }
        long [] stime = new long[cursor.getCount()];
        int i = 0;
        do {
            stime[i++] = cursor.getLong(0);
        } while (cursor.moveToNext());
        return stime;
    }
    public int tripSynchronizeDone(long time) {
        ContentValues data = new ContentValues();
        data.put("deleted", 2);
        String where = "starttime = ? ";
        String[] whereArgs = {String.valueOf(time)};
        return meta_.update(TABLE_META, data, where, whereArgs);
    }
    /**
     * all about uploading
     */
    public long nextTripToUpload() {
        String selectQuery = "SELECT  * FROM " + TABLE_META + " WHERE uploaded = 0;";
        Cursor cursor = meta_.rawQuery(selectQuery, null);
        cursor.moveToFirst();
        long stime = -1;
        do {
            if(cursor.getCount() == 0) {
                break;
            }
            stime = cursor.getLong(0);
            break;
        } while (cursor.moveToNext());
        return stime;
    }


    public void tripRemoveSensorData(long time) {
        String [] tables = {TABLE_ACCELEROMETER, TABLE_GYROSCOPE, TABLE_MAGNETOMETER, TABLE_ROTATION_MATRIX};
        SQLiteDatabase tmpdb = SQLiteDatabase.openOrCreateDatabase(Constants.kDBFolder + String.valueOf(time).concat(".db"), null, null);
        for(int i = 0; i < tables.length; ++i) {
            String dropsql = "DROP TABLE IF EXISTS " + tables[i] + ";";
            tmpdb.execSQL(dropsql);
        }
        tmpdb.close();
    }
    /**
     * label the meta table that the trip has been uploaded, and remove all the sensor tables, leave gps table
     * @param time
     */
    public int tripUploadDone(long time) {
        Log.d(TAG, "tripUploadDone");

        //drop the sensor tables to avoid space waste
        tripRemoveSensorData(time);

        //update information in meta table
        ContentValues data = new ContentValues();
        data.put("uploaded", 1);
        String where = "starttime = ? ";
        String[] whereArgs = {String.valueOf(time)};
        return meta_.update(TABLE_META, data, where, whereArgs);
    }

}
