package wisc.drivesense.triprecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import wisc.drivesense.activity.MainActivity;
import wisc.drivesense.activity.SettingActivity;
import wisc.drivesense.uploader.UploaderService;

public class ChargingStateReceiver extends BroadcastReceiver {

    private static String TAG = "ChargingStateReceiver";
    private static Intent mDrivingDetectionIntent = null;

    private static Intent mUploaderIntent = null;

    @Override
    public void onReceive(Context context, Intent intent) {


        if(SettingActivity.isAutoMode(context) == true) {
            autoStart(context, intent);
        }
        autoUpload(context, intent);

    }

    private void autoUpload(Context context, Intent intent) {
        // connectivity
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if(isConnected) {
            Log.d(TAG, "Internet is Connected!");
            if(MainActivity.isServiceRunning(context, UploaderService.class) == false) {
                Log.d(TAG, "Start upload service!!!");
                mUploaderIntent = new Intent(context, UploaderService.class);
                context.startService(mUploaderIntent);
            }
        } else {
            Log.d(TAG, "Internet is Closed!");
            //end uploading
            if(MainActivity.isServiceRunning(context, UploaderService.class) == true) {
                Log.d(TAG, "Stop upload service!!!");
                context.stopService(mUploaderIntent);
                mUploaderIntent = null;
            }
        }

    }

    private void autoStart(Context context, Intent intent) {
        //check charging status, and start sensor service automatically
        String action = intent.getAction();
        mDrivingDetectionIntent = new Intent(context, TripService.class);

        if(action.equals(Intent.ACTION_POWER_CONNECTED)) {
            // Do something when power connected
            Log.d(TAG, "plugged");
            if(MainActivity.isServiceRunning(context, TripService.class) == false) {
                Log.d(TAG, "Start driving detection service!!!");
                context.startService(mDrivingDetectionIntent);
            }
        } else if(action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
            // Do something when power disconnected
            Log.d(TAG, "unplugged");
            if(MainActivity.isServiceRunning(context, TripService.class) == true) {
                Log.d(TAG, "Stop driving detection service!!!");
                context.stopService(mDrivingDetectionIntent);
            }
        } else {

        }
    }



}
