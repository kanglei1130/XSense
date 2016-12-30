package wisc.drivesense.triprecorder;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import wisc.drivesense.database.DatabaseHelper;
import wisc.drivesense.dataprocessing.RealTimeSensorProcessing;
import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.Message;
import wisc.drivesense.utility.Rating;
import wisc.drivesense.utility.Trace;
import wisc.drivesense.utility.Trip;

public class TripService extends Service {

    private DatabaseHelper dbHelper_ = null;
    public Trip curtrip_ = null;
    public Rating rating_ = null;
    public RealTimeSensorProcessing processing_ = null;

    public Binder _binder = new TripServiceBinder();
    private AtomicBoolean _isRunning = new AtomicBoolean(false);

    private final String TAG = "Trip Service";


    private static Intent mSensor = null;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return _binder;
    }

    public class TripServiceBinder extends Binder {
        public TripService getService() {return TripService.this;}
        public Trip getTrip() {
            return curtrip_;
        }
        public boolean isRunning() {
            return _isRunning.get();
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        startService();
        return START_STICKY;
    }

    public void onDestroy() {
        Log.d(TAG, "stop driving detection service");
        _isRunning.set(false);
        stopService(mSensor);
        mSensor = null;

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

        //validate the trip based on distance and travel time

        if(curtrip_.getDistance() >= Constants.kTripMinimumDistance && curtrip_.getDuration() >= Constants.kTripMinimumDuration) {
            Toast.makeText(this, "Saving trip in background!", Toast.LENGTH_SHORT).show();
            recordingDrivingBehaviors();
            dbHelper_.insertTrip(curtrip_);
        } else {
            Toast.makeText(this, "Trip too short, not saved!", Toast.LENGTH_SHORT).show();
            dbHelper_.deleteTrip(curtrip_.getStartTime());
        }
        dbHelper_.closeDatabase();

        stopSelf();
    }

    private void recordingDrivingBehaviors() {
        int numberofoc = processing_.number_of_orientation_changed;
        double mv = (1.0 - Math.min(processing_.avg_mv_, 1.0))/0.01;
        if(mv == 100.0) {
            mv = 0.0;
        }
        double stability = mv;
        curtrip_.numberOfOrientationChanges_ = numberofoc;
        curtrip_.mountingStability_ = stability;
    }


    private void startService() {
        _isRunning.set(true);
        Log.d(TAG, "start driving detection service");

        mSensor = new Intent(this, SensorService.class);
        startService(mSensor);

        //start recording
        File dbDir = new File(Constants.kDBFolder);
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }

        Toast.makeText(this, "Start trip in background!", Toast.LENGTH_SHORT).show();
        dbHelper_ = new DatabaseHelper();

        long time = System.currentTimeMillis();
        dbHelper_.createDatabase(time);
        curtrip_ = new Trip(time);
        rating_ = new Rating(curtrip_);

        processing_ = new RealTimeSensorProcessing();

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("sensor"));
    }

    /**
     * where we get the sensor data
     */
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("trace");
            Trace trace = new Trace();
            trace.fromJson(message);

            if(trace.type.compareTo(Trace.GPS) == 0) {
                Log.d(TAG, "Got message: " + trace.toJson());

                trace = calculateTraceByGPS(trace);
                curtrip_.addGPS(trace);
                sendTrip(trace);
            } else {

            }

            if(dbHelper_.isOpen()) {
                dbHelper_.insertSensorData(trace);
            }
            processSensorData(trace);
        }

        private void processSensorData(Trace trace) {
            processing_.processTrace(trace);
            if(processing_.orientation_changing_) {
                Message msg = new Message(Message.ORIENTATION_CHANGE, "Orientation Changing: YES");
                sendMessage(msg);
            } else {
                Message msg = new Message(Message.ORIENTATION_CHANGE, "Orientation Changing: NO");
                sendMessage(msg);
            }

            double mv = (1.0 - Math.min(processing_.avg_mv_, 1.0))/0.01;
            if(mv == 100.0) {
                mv = 0.0;
            }

            Message stability = null;
            if(mv == 0.0) {
                stability = new Message(Message.STABILITY, "Mounting Stability: Training");
            } else {
                stability = new Message(Message.STABILITY, "Mounting Stability:" + String.format("%.0f", mv) + "%");
            }

            sendMessage(stability);
        }

        private Trace calculateTraceByGPS(Trace trace) {
            double brake = rating_.readingData(trace);
            double sign = brake >= 0.0? 1.0 : -1.0;
            double acc_y = processing_.acceleration_ * sign;
            //create a new trace for GPS, since we use GPS to capture driving behaviors
            Trace ntrace = new Trace(6);
            ntrace.type = trace.type;
            ntrace.time = trace.time;
            System.arraycopy(trace.values, 0, ntrace.values, 0, trace.values.length);
            ntrace.values[4] = brake;
            ntrace.values[5] = acc_y;
            return ntrace;
        }
    };

    private void sendTrip(Trace trace) {
        Intent intent = new Intent("driving");
        intent.putExtra("trip", trace.toJson());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendMessage(Message message) {
        Intent intent = new Intent("sensormessage");
        intent.putExtra("message", new Gson().toJson(message));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }




}
