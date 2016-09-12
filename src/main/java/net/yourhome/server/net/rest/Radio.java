package net.yourhome.server.net.rest;

import java.sql.SQLException;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import net.yourhome.server.radio.RadioController;


@Path( "/Radio" ) 
public class Radio {

	private static Logger log = Logger.getLogger(Radio.class);
    private RadioController controller;
    
    // The initialize method will only be called when the controllers are needed (in this way, the controllers are not initialized during the network startup)
	private void initialize() {
		 controller = RadioController.getInstance();
	}
    
	// POST api/RadioCommands/
    @POST
	@Produces( { MediaType.APPLICATION_JSON  } )
    public String createNewCommand(@Context final UriInfo uriInfo, String bodyContent) throws SQLException
    {        
		if(controller == null) {
			initialize();
		}
    	try {
			JSONObject json = new JSONObject(bodyContent);
			RadioController.RadioChannel newRadioChannel = controller.new RadioChannel(json.getString("channelName"),json.getString("channelUrl"));
	    
			return RadioController.getInstance().createRadioChannel(newRadioChannel);
        
		} catch (JSONException e) {
			log.error(e);
	        return "";
		}
    }

    // DELETE api/RadioCommands/
    @DELETE
	@Produces( { MediaType.TEXT_HTML  } )
    @Path("{radioId}")
    public String deleteRadio(@Context final UriInfo uriInfo, @PathParam( "radioId" ) final int radioId, String bodyContent) throws SQLException
    {        
		if(controller == null) {
			initialize();
		}
		controller.deleteRadioChannel(radioId);
		
        return "";
    }
	

}
