package wisc.drivesense.utility;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lkang on 3/29/16.
 */
public class Trip implements Serializable {

    private long startTime_ = 0;
    private long endTime_ = 0;
    private double distance_ = 0; // in meter
    private double speed_ = 0.0;
    private double score_ = 10.0;
    private int status_ = 1;
    private List<Trace> gps_;
    private Trace start_ = null;
    private Trace dest_ = new Trace();



    public int numberOfOrientationChanges_ = 0;
    public double mountingStability_ = 0.0;

    private String TAG = "Trip";


    public Trip (long time) {
        gps_ = new ArrayList<Trace>();
        this.startTime_ = time;
        //rating = new Rating(this);
    }

    public void setScore(double score) {this.score_ = score;}
    public void setStatus(int status) {this.status_ = status;}
    public void setEndTime(long time) {this.endTime_ = time;}
    public void setDistance(double dist) {this.distance_ = dist;}



    public long getStartTime() {
        return this.startTime_;
    }
    public long getEndTime() {
        return this.endTime_;
    }
    public double getDistance() {
        return this.distance_;
    }
    public double getScore() {return this.score_;}
    public long getDuration() {return this.endTime_ - this.startTime_;}
    public int getStatus() {return this.status_;}


    public Trace getStartPoint() {return start_;}
    public Trace getEndPoint() {return dest_;}



    public double getSpeed() {return speed_ * Constants.kMeterPSToMilePH;}


    /**
     * Add one GPS point in real time, do not keep the GPS array in memory
     * always read/write gps points from database
     *
     * @param trace
     */
    public void addGPS(Trace trace) {

        gps_.add(trace);
        if(start_ == null) {
            start_ = new Trace();
            start_.copyTrace(trace);
        }
        dest_.copyTrace(trace);
        speed_ = trace.values[3];
        this.endTime_ = trace.time;

        int sz = gps_.size();
        if(sz >= 2) {
            distance_ += distance(gps_.get(sz - 2), gps_.get(sz - 1));
            //keep it to be just last two locations
            gps_.remove(0);
        }
    }


    public void setGPSPoints(List<Trace> gps) {
        int sz = gps.size();
        if(sz == 0) {
            return;
        }
        this.gps_ = gps;
        this.start_ = gps.get(0);
        this.dest_ = gps.get(sz - 1);
    }

    public List<Trace> getGPSPoints() {
        return gps_;
    }


    public double gpspercent_ = 0.0;
    public double sensorpercent_ = 0.0;

    public int brakeByGPS_ = 0;
    public int brakeBySensor_ = 0;
    public int brakeByXSense_ = 0;
    public void calculateTripMeta() {
        if(this.gps_ == null || this.gps_.size() <= 2) {
            return;
        }
        int gpscount = 0;
        boolean usegps = false;
        for(int i = 10; i < this.gps_.size(); ++i) {
            Trace cur = this.gps_.get(i);
            double speed = cur.values[3];
            usegps = false;
            if(speed >= 10) {
                gpscount ++;
                usegps = true;
            } else {
                if(cur.values[5] == 0.0) {
                    gpscount ++;
                    usegps = true;
                }
            }

            if(cur.values[4] < Constants.kHardBrakeThreshold) {
                brakeByGPS_ ++;
            }
            if(cur.values[5] < Constants.kHardBrakeThreshold) {
                brakeBySensor_ ++;
            }
            if(usegps && cur.values[4] < Constants.kHardBrakeThreshold) {
                brakeByXSense_ ++;
            }
            if(!usegps && cur.values[5] < Constants.kHardBrakeThreshold) {
                brakeByXSense_ ++;
            }
        }
        this.gpspercent_ = (int)(gpscount * 100.0/(this.gps_.size() - 10));
        this.sensorpercent_ = 100 - this.gpspercent_;
    }



    public static double distance(Trace gps0, Trace gps1) {

        double lat1 = Math.toRadians(gps0.values[0]);
        double lng1 = Math.toRadians(gps0.values[1]);
        double lat2 = Math.toRadians(gps1.values[0]);
        double lng2 = Math.toRadians(gps1.values[1]);

        double p1 = Math.cos(lat1)*Math.cos(lat2)*Math.cos(lng1-lng2);
        double p2 = Math.sin(lat1)*Math.sin(lat2);

        double res = Math.acos(p1 + p2);
        if(res< Constants.kSmallEPSILON || res!=res) {
            res = 0.0;
        }
        //Log.log("dis:", res);
        return res * Constants.kEarthRadius;
    }

}
