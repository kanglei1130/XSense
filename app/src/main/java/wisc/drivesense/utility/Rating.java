package wisc.drivesense.utility;

import java.io.Serializable;

/**
 * Created by lkang on 4/20/16.
 */
public class Rating implements Serializable {
    private Trip trip_;
    private int counter_;
    private Trace lastTrace_;
    private double lastSpeed_;
    private double score_ = 10.0;

    private static String TAG = "Rating";

    public Rating(Trip trip) {
        this.trip_ = trip;
        lastSpeed_ = -1.0;
        lastTrace_ = null;
        counter_ = 0;
    }

    public double readingData(Trace trace) {
        if(lastTrace_ == null) {
            lastTrace_ = trace;
            return 0.0;
        }
        double time = trace.time - lastTrace_.time;
        double curSpeed = Trip.distance(lastTrace_, trace)/(time/1000.0);
        if(lastSpeed_ == -1.0) {
            lastSpeed_ = curSpeed;
            return 0;
        } else if(curSpeed == 0.0) {
            return 0;
        } else {
            counter_++;
        }
        double a = (curSpeed - lastSpeed_)/(time/1000.0);

        lastSpeed_ = curSpeed;
        lastTrace_ = trace;
        if(a < -2.5) {
            double curscore = 3.0 - Math.min(3.0, Math.abs(a));
            score_ = (score_ * (counter_ - 1) + curscore * 10.0)/counter_;
            trip_.setScore(score_);
        } else {
            score_ = (score_ * (counter_ - 1) + 10.0)/counter_;
            trip_.setScore(score_);
        }
        return a;

    }



}
