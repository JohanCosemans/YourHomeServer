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

import net.yourhome.server.radio.RadioController;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.sql.SQLException;

@Path("/Radio")
public class Radio {

	private static Logger log = Logger.getLogger(Radio.class);
	private RadioController controller;

	// The initialize method will only be called when the controllers are needed
	// (in this way, the controllers are not initialized during the network
	// startup)
	private void initialize() {
		this.controller = RadioController.getInstance();
	}

	// POST api/RadioCommands/
	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	public String createNewCommand(@Context final UriInfo uriInfo, String bodyContent) throws SQLException {
		if (this.controller == null) {
			this.initialize();
		}
		try {
			JSONObject json = new JSONObject(bodyContent);
			RadioController.RadioChannel newRadioChannel = this.controller.new RadioChannel(json.getString("channelName"), json.getString("channelUrl"));

			return RadioController.getInstance().createRadioChannel(newRadioChannel);

		} catch (JSONException e) {
			Radio.log.error(e);
			return "";
		}
	}

	// DELETE api/RadioCommands/
	@DELETE
	@Produces({ MediaType.TEXT_HTML })
	@Path("{radioId}")
	public String deleteRadio(@Context final UriInfo uriInfo, @PathParam("radioId") final int radioId, String bodyContent) throws SQLException {
		if (this.controller == null) {
			this.initialize();
		}
		this.controller.deleteRadioChannel(radioId);

		return "";
	}

}
