package net.yourhome.server.net.rest;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import net.yourhome.common.net.messagestructures.JSONMessage;
import net.yourhome.server.net.Server;

@Path("/messagehandler")
public class MessageHandler {

	private Server netWebSocketServer = Server.getInstance();

	@Produces({ MediaType.APPLICATION_JSON })
	@POST
	public String processMessage(@Context final UriInfo uriInfo, String bodyContent) {
		JSONMessage returnMessage = netWebSocketServer.processIncomingMessage(bodyContent);
		String returnMessageString = "{}";
		if (returnMessage != null) {
			returnMessageString = returnMessage.serialize().toString();
		}
		return returnMessageString;
	}
}
