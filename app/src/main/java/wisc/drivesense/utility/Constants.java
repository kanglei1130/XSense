package wisc.drivesense.utility;

/**
 * Created by lkang on 3/29/16.
 */
public class Constants {
    public static final double kEarthGravity = 9.80665; /*m^2/s*/

    /*for GPS*/
    public static final double kSmallEPSILON = 1e-8;
    public static final double kEarthRadius = 6371 * 1000; /*m*/

    public static final double kMeterToMile = 0.000621371;
    public static final double kMeterPSToMilePH = 2.23694;
    public static final double kKmPHToMPH = 0.621371;
    public static final double kKmPHToMeterPS = 0.277778;

    public static final double kMileToMeters = 1609.34;

    public static final String kInputSeperator = "\t";
    public static final String kOutputSeperator = "\t";
    public static final String slash = "/";

    public static final double kSampleRate = 1.0;
    public static final double kRecordingInterval = 100;


    public static final String kUploadTripDBFile = "dbfile";
    public static final String kSychronizeTripDeletion = "delete";


    private static final String kDomain = "http://drivesense.wings.cs.wisc.edu:8000";
    public static final String kUploadURL = kDomain + "/upload";
    public static final String kSyncDeleteURL = kDomain + "/androidsync";


    public static final String kSignInURL = kDomain + "/androidsignin";
    public static final String kSignUpURL = kDomain + "/androidsignup";

    public static final String kPackageName = "wisc.drivesense";
    public static final String kDBFolder = "/data/data/" + kPackageName + "/databases/";
    public static final String kVideoFolder = "/data/data/" + kPackageName + "/videos/";

    public static final String kUserEmail = "lkang@cs.wisc.edu";

    public static final int kNumberOfTripsDisplay = 20;
    public static final double kTripMinimumDuration = 0.0; //1.0; //mins
    public static final double kTripMinimumDistance = 0; //1000; //meter


    public static final double kExponentialMovingAverageAlpha = 0.4;//0.3;

    public static final double kOrientationChangeVarianceThreshold = 1.2;

    public static final double kHardBrakeThreshold = -3.0;


}
