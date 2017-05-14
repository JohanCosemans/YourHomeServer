package net.yourhome.server.base.rules.scenes.actions.notifications.fcm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FcmResponse {

    private static final Logger LOG = Logger.getLogger(FcmResponse.class.getName());

    private HttpURLConnection connection;
    private String success;
    private String error;
    private int responseCode;

    public FcmResponse(HttpURLConnection connection) throws IOException {
        this.connection = connection;
        responseCode = connection.getResponseCode();
        success = readResponseMessage(false);
        error = readResponseMessage(true);
    }

    public String toString(){
        return String.format("Response: %d\n Success Message: '%s'\nError Message: '%s'", responseCode, success, error);
    }

    private String readResponseMessage(boolean errorStream){
        InputStream in = null;
        try {
            if(errorStream) {
                in = connection.getErrorStream();
            }else {
                in = connection.getInputStream();
            }
        } catch (IOException e1) {
            LOG.log(Level.SEVERE,"Could not read result",e1);
        }

        if(in == null)
            return "";

        BufferedReader r = new BufferedReader(new InputStreamReader(in));

        StringBuilder total = new StringBuilder();

        String line = null;

        try {
            while ((line = r.readLine()) != null) {
                total.append(line);
            }
            r.close();
        } catch (IOException e) {
            LOG.log(Level.SEVERE,"Could not read success message",e);
        }
        return total.toString();
    }
}
