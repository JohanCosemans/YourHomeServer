/*-
 * Copyright (c) 2016 Coteq, Johan Cosemans
 * All rights reserved.
 *
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY COTEQ AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.yourhome.server.net.rest;

import net.yourhome.common.net.messagestructures.http.HttpCommand;
import net.yourhome.common.net.messagestructures.http.HttpCommandMessage;
import net.yourhome.common.net.messagestructures.http.HttpNodeMessage;
import net.yourhome.server.http.HttpCommandController;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.sql.SQLException;

@Path("/HttpCommands")
public class HttpCommands {
	private HttpCommandController controller;
	private static Logger log = Logger.getLogger(HttpCommands.class);

	// The initialize method will only be called when the controllers are needed
	// (in this way, the controllers are not initialized during the network
	// startup)
	private void initialize() {
		this.controller = HttpCommandController.getInstance();
	}

	// POST api/HttpCommands/Test

	@POST
	@Path("/Test")
	@Produces({ MediaType.TEXT_PLAIN })
	public String Post(@Context final UriInfo uriInfo, String bodyContent) {
		try {
			JSONObject jsonObject = new JSONObject(bodyContent);
			HttpCommand command = new HttpCommand(jsonObject);

			try {
				return HttpCommandController.getInstance().sendHttpCommand(command).toString();
			} catch (Exception e) {
				HttpCommands.log.error("Exception occured: ", e);
				return "Error: " + e.getCause().getMessage();
			}
		} catch (JSONException e) {
			return "";
		}
	}

	// POST api/HttpCommands/

	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	public String createNewCommandOrNode(@Context final UriInfo uriInfo, String bodyContent) throws SQLException {
		try {
			JSONObject json = new JSONObject(bodyContent);
			String messageType = json.getString("type");

			if (messageType.equals("HttpCommand")) {
				HttpCommandMessage httpCommandMessage = new HttpCommandMessage();
				httpCommandMessage.httpCommand = new HttpCommand(json);

				return HttpCommandController.getInstance().saveHttpCommand(httpCommandMessage);

			} else if (messageType.equals("HttpNode")) {
				HttpNodeMessage httpNodeMessage = new HttpNodeMessage();
				httpNodeMessage.name = json.getString("name");
				try {
					httpNodeMessage.parentId = json.getInt("parentId");
				} catch (JSONException e) {
				}

				return HttpCommandController.getInstance().saveHttpNode(httpNodeMessage);
			}
		} catch (JSONException e) {
			return "";
		}
		return "";
	}

	// GET api/HttpCommands/
	/*
	 * @Produces( { MediaType.APPLICATION_JSON } )
	 * 
	 * @GET public String getNodeAndCommandStructure() { if(controller == null)
	 * { initialize(); }
	 * 
	 * return controller.getCommandList().toString(); } // GET
	 * api/HttpCommands/Nodes /* @Produces( { MediaType.APPLICATION_JSON } )
	 * 
	 * @Path("/Nodes")
	 * 
	 * @GET public String getAllNodes() { if(controller == null) { initialize();
	 * }
	 * 
	 * return controller.getAllNodes().toString(); }
	 */

	// GET api/HttpCommands/Node/{nodeId}
	@GET
	@Path("/Node/{nodeId}")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getNode(@Context final UriInfo uriInfo, @PathParam("nodeId") final int nodeId, String bodyContent) {
		if (this.controller == null) {
			this.initialize();
		}
		return this.controller.getNode(nodeId).toString();
	}

	// GET api/HttpCommands/Command/{commandId}
	@GET
	@Path("Command/{commandId}")
	@Produces({ MediaType.APPLICATION_JSON })
	public String getCommand(@Context final UriInfo uriInfo, @PathParam("commandId") final int commandId, String bodyContent) throws SQLException {
		if (this.controller == null) {
			this.initialize();
		}
		return this.controller.getCommand(commandId).serialize().toString();
	}

	@PUT
	@Path("Command/{commandId}")
	@Produces({ MediaType.APPLICATION_JSON })
	public String changeCommand(@Context final UriInfo uriInfo, @PathParam("commandId") final int commandId, String bodyContent) throws SQLException {
		if (this.controller == null) {
			this.initialize();
		}
		JSONObject httpCommand;
		HttpCommandMessage changedHttpCommand;
		try {
			httpCommand = new JSONObject(bodyContent);
			changedHttpCommand = new HttpCommandMessage(httpCommand);
			return this.controller.changeHttpCommand(changedHttpCommand);
		} catch (JSONException e) {
			HttpCommands.log.error("Exception occured: ", e);
			return "{ \"result\" : \"ERROR\" }";
		}
	}

	// DELETE /api/HttpCommands/Command/{commandId}
	@DELETE
	@Path("Command/{commandId}")
	@Produces({ MediaType.TEXT_HTML })
	public String deleteCommand(@Context final UriInfo uriInfo, @PathParam("commandId") final int commandId, String bodyContent) throws SQLException {
		if (this.controller == null) {
			this.initialize();
		}
		this.controller.deleteCommand(commandId);
		return "";
	}

	// DELETE /api/HttpCommands/Node/{nodeId}
	@DELETE
	@Path("Node/{nodeId}")
	@Produces({ MediaType.TEXT_HTML })
	public String deleteNode(@Context final UriInfo uriInfo, @PathParam("nodeId") final int nodeId, String bodyContent) throws SQLException {
		if (this.controller == null) {
			this.initialize();
		}
		this.controller.deleteNode(nodeId);
		return "";
	}
}
