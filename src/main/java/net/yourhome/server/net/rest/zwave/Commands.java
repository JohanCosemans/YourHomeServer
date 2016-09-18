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
package net.yourhome.server.net.rest.zwave;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.yourhome.server.zwave.ZWaveController;

@Path("/ZWave/Commands")
public class Commands {

	private ZWaveController controller;
	private static Logger log = Logger.getLogger(Commands.class);

	// The initialize method will only be called when the controllers are needed
	// (in this way, the controllers are not initialized during the network
	// startup)
	private void initialize() {
		this.controller = ZWaveController.getInstance();
	}

	@Produces({ MediaType.APPLICATION_JSON })
	@GET
	public String getCommands() {
		if (this.controller == null) {
			this.initialize();
		}
		JSONArray jsonArray = new JSONArray();
		for (ZWaveCommand command : this.controller.getCommandList(this)) {
			JSONObject jsonObject = new JSONObject();
			try {
				jsonObject.put("name", command.name);
				jsonObject.put("command", command.command);
			} catch (JSONException e) {
				Commands.log.error("Exception occured: ", e);
			}
			jsonArray.put(jsonObject);
		}

		return jsonArray.toString();
	}

	public class ZWaveCommand {
		public String name;
		public String command;

		public ZWaveCommand(String name, String command) {
			this.name = name;
			this.command = command;
		}
	}
}
