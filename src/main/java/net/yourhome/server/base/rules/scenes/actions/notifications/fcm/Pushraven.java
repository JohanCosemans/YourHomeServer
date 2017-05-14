package net.yourhome.server.base.rules.scenes.actions.notifications.fcm;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This method allows for simple and modular Notification creation. Notifications can then be pushed to clients
 * over FCM using the push() method.
 * @author Raudius
 *
 */
public class Pushraven {


    private static final Logger LOG = Logger.getLogger(Pushraven.class.getName());

    private final static String API_URL = "https://fcm.googleapis.com/fcm/send";
    private static String FIREBASE_SERVER_KEY;
    public static Notification notification;

    // static initialization
    static {
        notification = new Notification();
    }


    /**
     * Set the API Server Key.
     */
    public static void setKey(String key){
        FIREBASE_SERVER_KEY = key;
        LOG.info("FirebasePush Setting api key to "+key.substring(0,6)+"...");
    }


    /**
     * Set new Notification object
     */
    public static void setNotification(Notification notification){
        Pushraven.notification = notification;
    }


    /**
     * Messages sent to targets.
     * This class interfaces with the FCM server by sending the Notification over HTTP-POST JSON.
     * @return FcmResponse object containing HTTP response info.
     */
    public static FcmResponse push(Notification notification) {

        if(FIREBASE_SERVER_KEY == null){
            LOG.warning("No Server-Key has been defined for Pushraven.");
            return null;
        }

        HttpURLConnection connection = null;
        try{
            URL url = new URL(API_URL);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");

            // Set POST headers
            connection.setRequestProperty("Authorization", "key="+FIREBASE_SERVER_KEY);
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

            // Send POST body
            connection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(wr, "UTF-8"));
            writer.write(notification.toJSON());
            writer.close();

            return new FcmResponse(connection);
        }
        catch(Exception e){
            LOG.log(Level.SEVERE, "Could not send notification",e);
        }
        return null;
    }


    /**
     * Messages sent to targets.
     * This class interfaces with the FCM server by sending the Notification over HTTP-POST JSON.
     * @return FcmResponse object containing HTTP response info.
     */
    public static FcmResponse push() {
        return push(notification);
    }

}