package wisc.drivesense.uploader;

/**
 * Created by lkang on 4/14/16.
 */

import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpClient {
    private String url;
    private HttpURLConnection con;
    private OutputStream os;

    private String delimiter = "--";
    private String boundary =  "SwA"+Long.toString(System.currentTimeMillis())+"SwA";
    private static String TAG = "HttpClient";

    public HttpClient(String url) {
        this.url = url;
    }



    public void connectForMultipart() throws Exception {
        con = (HttpURLConnection) ( new URL(url)).openConnection();
        con.setRequestMethod("POST");
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setConnectTimeout(100 * 60);
        con.setChunkedStreamingMode(1024);
        con.setRequestProperty("Connection", "Keep-Alive");
        con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        con.connect();
        os = con.getOutputStream();
    }

    public void finishMultipart() throws Exception {
        os.write( (delimiter + boundary + delimiter + "\r\n").getBytes());
        os.flush();
        os.close();
    }





    public boolean addFilePart(String paramName, String fileName, byte[] data) {
        try {
            os.write((delimiter + boundary + "\r\n").getBytes());
            os.write(("Content-Disposition: form-data; name=\"" + paramName + "\"; filename=\"" + fileName + "\"\r\n").getBytes());
            os.write(("Content-Type: application/octet-stream\r\n").getBytes());
            os.write(("Content-Transfer-Encoding: binary\r\n").getBytes());
            os.write("\r\n".getBytes());
            os.write(data);
            os.write("\r\n".getBytes());
            return false;
        } catch (Exception e) {
            Log.d(TAG, e.toString());
            return false;
        }
    }



    public boolean addFormPart(String paramName, String value)  {
        try {
            os.write((delimiter + boundary + "\r\n").getBytes());
            os.write("Content-Type: text/plain\r\n".getBytes());
            os.write(("Content-Disposition: form-data; name=\"" + paramName + "\"\r\n").getBytes());
            os.write(("\r\n" + value + "\r\n").getBytes());
            return true;
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            return false;
        }
    }

    public String getResponse() {
        try {
            InputStream is = con.getInputStream();
            byte[] b1 = new byte[1];
            StringBuffer buffer = new StringBuffer();
            while (is.read(b1) != -1) {
                buffer.append(new String(b1));
            }
            con.disconnect();
            return buffer.toString();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            return null;
        }
    }
}