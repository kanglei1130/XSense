package wisc.drivesense.utility;

import java.io.Serializable;

/**
 * Created by lkang on 12/14/16.
 */

public class Message implements Serializable {

    public String type = "none";
    public String value = "";

    private static String TAG = "Message";

    public static String ORIENTATION_CHANGE = "orientation";
    public static String STABILITY = "stability";


    public Message(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public Message() {

    }


}
