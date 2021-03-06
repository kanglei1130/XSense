
package wisc.drivesense.activity;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;

import wisc.drivesense.R;
import wisc.drivesense.triprecorder.TripService;
import wisc.drivesense.utility.Constants;
import wisc.drivesense.utility.Message;
import wisc.drivesense.utility.Trace;
import wisc.drivesense.utility.Trip;
import wisc.drivesense.vediorecorder.CameraFragment;


public class MainActivity extends AppCompatActivity {

    //for display usage only, all calculation is conducted in TripService

    private Trip curtrip_ = null;


    private static String TAG = "MainActivity";

    private TextView tvSpeed = null;
    private TextView tvMile = null;
    private Button btnStart = null;

    private TextView tvOrientation = null;
    private TextView tvStability = null;


    private boolean mIsRecordingVideo = false;

    private CameraFragment camerafragment = null;

    private static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //ask for permission first
        int permissionCheck = isAllPermissionGranted();
        if(permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, 1001);
        }

        // Initializing Facebook Integration

        setupViews();
        setupFolders();
        addListenerOnButton();

        //findViewById(R.id.textspeed).setVisibility(View.GONE);
        camerafragment = CameraFragment.newInstance();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, camerafragment)
                .commit();

    }


    private void setupViews() {
        android.support.v7.widget.Toolbar mToolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.maintoolbar);
        setSupportActionBar(mToolbar);

        tvSpeed = (TextView) findViewById(R.id.textspeed);
        tvMile = (TextView) findViewById(R.id.milesdriven);
        btnStart = (Button) findViewById(R.id.btnstart);

        //tvOrientation = (TextView) findViewById(R.id.orientation);
        //tvStability = (TextView) findViewById(R.id.stability);
    }

    //setup folders to store database files and video files
    private void setupFolders () {
        File dbDir = new File(Constants.kDBFolder);
        File videoDir = new File(Constants.kVideoFolder);
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }
        if(!videoDir.exists()) {
            videoDir.mkdir();
        }
    }


    private class TripServiceConnection implements ServiceConnection {
        private TripService.TripServiceBinder binder = null;

        public void onServiceConnected(ComponentName className, IBinder service) {
            binder = ((TripService.TripServiceBinder)service);
            curtrip_ = binder.getTrip();

            //start recording after the trip is started
            mIsRecordingVideo = true;
            camerafragment.setRecordingPath(Constants.kVideoFolder + curtrip_.getStartTime());
            camerafragment.startRecordingVideo();
        }
        public void onServiceDisconnected(ComponentName className) {
            binder = null;

        }
    };
    private Intent mTripServiceIntent = null;
    private ServiceConnection mTripConnection = null;

    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onSTop");

    }

    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPuase");
        if(mTripConnection != null) {
            unbindService(mTripConnection);
        }
    }
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");


        if (MainActivity.isServiceRunning(this, TripService.class) == true) {
            btnStart.setBackgroundResource(R.drawable.stop_button);
            btnStart.setText(R.string.stop_button);
            //if the service is running, then start the connnection
            mTripServiceIntent = new Intent(this, TripService.class);
            mTripConnection = new TripServiceConnection();
            bindService(mTripServiceIntent, mTripConnection, Context.BIND_AUTO_CREATE);
        } else {
            btnStart.setBackgroundResource(R.drawable.start_button);
            btnStart.setText(R.string.start_button);
        }
    }


    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        if(SettingActivity.isAutoMode(MainActivity.this)) {
            Toast.makeText(MainActivity.this, "Disable Auto Mode to Stop", Toast.LENGTH_SHORT).show();
            return;
        }
        mTripServiceIntent = new Intent(this, TripService.class);
        stopService(mTripServiceIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        return true;
    }

    //get the selected dropdown list value
    public void addListenerOnButton() {


        //final TextView txtView= (TextView) findViewById(R.id.textspeed);
        btnStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(SettingActivity.isAutoMode(MainActivity.this)) {
                    Toast.makeText(MainActivity.this, "Disable Auto Mode in Settings", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d(TAG, "start button clicked");
                if (MainActivity.isServiceRunning(MainActivity.this, TripService.class) == false) {
                    //Toast.makeText(MainActivity.this, "Service Started!", Toast.LENGTH_SHORT).show();
                    startRunning();
                    btnStart.setBackgroundResource(R.drawable.stop_button);
                    btnStart.setText(R.string.stop_button);
                } else {
                    //Toast.makeText(MainActivity.this, "Service Stopped!", Toast.LENGTH_SHORT).show();
                    stopRunning();
                    btnStart.setBackgroundResource(R.drawable.start_button);
                    btnStart.setText(R.string.start_button);
                    //showDriveRating();
                }
            }
        });
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.settings:
                showSettings();
                return true;

            case R.id.history:
                showHistory();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }
    private synchronized void startRunning() {
        Log.d(TAG, "start running");

        //curtrip_ = new Trip(System.currentTimeMillis());
        LocalBroadcastManager.getInstance(this).registerReceiver(mTraceReceiver, new IntentFilter("driving"));
       // LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("sensormessage"));


        mTripServiceIntent = new Intent(this, TripService.class);
        mTripConnection = new TripServiceConnection();
        if(MainActivity.isServiceRunning(this, TripService.class) == false) {
            Log.d(TAG, "Start driving detection service!!!");
            bindService(mTripServiceIntent, mTripConnection, Context.BIND_AUTO_CREATE);
            startService(mTripServiceIntent);
        }

    }

    private synchronized void stopRunning() {

        Log.d(TAG, "Stopping live data..");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mTraceReceiver);
       // LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);

        tvSpeed.setText("null");
        tvMile.setText("null");

        //tvOrientation.setText("Orientation Changing: N/A");
        //tvStability.setText("Mounting Stability: N/A");

        if(MainActivity.isServiceRunning(this, TripService.class) == true) {
            Log.d(TAG, "Stop driving detection service!!!");
            stopService(mTripServiceIntent);
            unbindService(mTripConnection);
            mTripConnection = null;
            mTripServiceIntent = null;
        }


        if(mIsRecordingVideo) {
            mIsRecordingVideo = false;
            camerafragment.stopRecordingVideo();
        }
    }

    //
    /**
     * where we get the sensor data
     */
    private BroadcastReceiver mTraceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("trip");
            Trace trace = new Trace();
            trace.fromJson(message);
            if(curtrip_ != null) {
                if(trace.type.equals(Trace.GPS)) {
                    Log.d(TAG, "Got message: " + message);
                    curtrip_.addGPS(trace);
                    tvSpeed.setText(String.format("%.1f", curtrip_.getSpeed()) + "mph");
                    tvMile.setText(String.format("%.2f", curtrip_.getDistance() * Constants.kMeterToMile) + "mi");
                }
            }
        }
    };


    /*
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String msg = intent.getStringExtra("message");
            Gson gson = new Gson();
            Message message = gson.fromJson(msg, Message.class);

            if(curtrip_ != null) {
                if(message.type.equals(Message.ORIENTATION_CHANGE)) {
                    tvOrientation.setText(message.value);
                } else if(message.type.equals(Message.STABILITY)) {
                    tvStability.setText(message.value);
                } else {

                }
            }
        }
    };
    */


    public void showSettings() {
        Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);
    }

    public void showHistory() {
        Intent intent = new Intent(this, HistoryActivity.class);
        startActivity(intent);
    }




    public static boolean isServiceRunning(Context context, Class running) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (running.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    /**
     * persmission related, we request all the permissions all for good, refactor required for production usage
     * @return
     */
    private int isAllPermissionGranted() {
        for(int i = 0; i < PERMISSIONS.length; ++i) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, PERMISSIONS[i]);
            if(permissionCheck == PackageManager.PERMISSION_DENIED) {
                return PackageManager.PERMISSION_DENIED;
            }
        }
        return PackageManager.PERMISSION_GRANTED;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1001: {
                if (grantResults.length == PERMISSIONS.length && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Got permission to use location");
                }
            }
        }
    }


}


