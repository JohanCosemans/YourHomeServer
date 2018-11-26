package net.yourhome.server.amazon.alexa;

import net.yourhome.common.base.enums.MessageLevels;
import net.yourhome.common.base.enums.MessageTypes;
import net.yourhome.common.net.messagestructures.JSONMessage;
import net.yourhome.common.net.messagestructures.general.ClientMessageMessage;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Johan on 9/01/2017.
 */
public class ApiCaller {

    private static final Logger log = LoggerFactory.getLogger(ApiCaller.class);

    public static final String YOURHOME_API = "YOURHOME_API";
    private final String YOURHOME_API_ADDRESS = System.getenv(YOURHOME_API);

    public JSONMessage callApi(JSONMessage jsonMessage) {
        log.info("Sending : " + jsonMessage.serialize().toString());
        log.info("To YourHome API : " + YOURHOME_API_ADDRESS);

        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        HttpPost httpPost = new HttpPost(YOURHOME_API_ADDRESS);
        try {
            httpPost.setEntity(new StringEntity(jsonMessage.serialize().toString()));
            response = httpclient.execute(httpPost);
            System.out.println(response.getStatusLine());
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject jsonObject = new JSONObject(responseBody);
            JSONMessage message = MessageTypes.getMessage(jsonObject);
            log.info("Response : " + message.serialize().toString());
            return message;
        } catch (Exception e) {
            log.error("Error",e);
        } finally {
            if(response != null) {
                try { response.close(); }catch(Exception e) {}
            }
        }
        ClientMessageMessage failedMessage = new ClientMessageMessage();
        failedMessage.messageContent = "Failed to activate scene";
        failedMessage.messageLevel = MessageLevels.ERROR;
        return failedMessage;
    }
}
