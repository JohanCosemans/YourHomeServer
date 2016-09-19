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

import java.util.Collection;
import java.util.Map.Entry;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.yourhome.common.base.enums.ControllerTypes;
import net.yourhome.common.base.enums.ValueTypes;
import net.yourhome.server.ControllerNode;
import net.yourhome.server.IController;
import net.yourhome.server.net.Server;

@Path("/Controllers")
public class Controllers {

	private static Logger log = Logger.getLogger(Controllers.class);
	private Server server;

	// The initialize method will only be called when the controllers are needed
	// (in this way, the controllers are not initialized during the network
	// startup)
	private void initialize() {
		this.server = Server.getInstance();
	}

	@Produces({ MediaType.APPLICATION_JSON })
	@GET
	public String get(@Context final UriInfo uriInfo, String bodyContent) throws JSONException {

		JSONArray allControllers = this.getValuesForAllControllers();

		// Remove "Commands" node from general controller (can be added again
		// later?)
		for (int i = allControllers.length() - 1; i >= 0; i--) {
			JSONObject controller = allControllers.getJSONObject(i);
			if (allControllers.getJSONObject(i).getString("identifier").equals(ControllerTypes.GENERAL.convert())) {
				JSONArray allNodes = controller.getJSONArray("nodes");
				for (int j = allNodes.length() - 1; j >= 0; j--) {
					JSONObject node = allNodes.getJSONObject(j);
					if (node.getString("identifier").equals("Commands")) {
						allNodes.remove(j);
					}
				}
			}
		}
		return allControllers.toString();
	}

	@Produces({ MediaType.APPLICATION_JSON })
	@Path("/Values")
	@GET
	public String getControllableValues(@Context final UriInfo uriInfo, String bodyContent) throws JSONException {
		JSONArray allControllers = this.getValuesForAllControllers();

		// Go through the list and filter out all non-controllable values
		for (int i = allControllers.length() - 1; i >= 0; i--) {
			JSONObject controller = allControllers.getJSONObject(i);

			JSONArray allNodes = controller.getJSONArray("nodes");
			for (int j = allNodes.length() - 1; j >= 0; j--) {
				JSONObject node = allNodes.getJSONObject(j);

				JSONArray allValues = node.getJSONArray("values");
				for (int k = allValues.length() - 1; k >= 0; k--) {
					JSONObject value = allValues.getJSONObject(k);
					if (!this.isControllableValue(ValueTypes.convert(value.getString("valueType")))) {
						allValues.remove(k);
					}
				}
				if (allValues.length() == 0) {
					allNodes.remove(j);
				}
			}
			if (allNodes.length() == 0) {
				allControllers.remove(i);
			}
		}

		return allControllers.toString();
	}

	@Produces({ MediaType.APPLICATION_JSON })
	@Path("/Triggers")
	@GET
	public String getTriggerValues(@Context final UriInfo uriInfo, String bodyContent) throws JSONException {
		if (this.server == null) {
			this.initialize();
		}

		JSONArray controllerArray = new JSONArray();
		for (Entry<String, IController> entry : this.server.getControllers().entrySet()) {

			if (entry.getValue().isEnabled() && entry.getValue().getIdentifier() != null) {

				String controllerName = entry.getValue().getName();
				if (controllerName != null && !controllerName.equals("")) {
					JSONObject controllerObject = new JSONObject();
					Collection<ControllerNode> triggerNodes = entry.getValue().getTriggers();
					JSONArray nodesArray = new JSONArray();
					if (triggerNodes != null) {
						for (ControllerNode node : triggerNodes) {
							if (node.getValues().size() > 0) {
								nodesArray.put(new JSONObject(node));
							}
						}
						if (triggerNodes.size() > 0) {
							controllerObject.put("name", controllerName);
							controllerObject.put("identifier", entry.getValue().getIdentifier());
							controllerObject.put("nodes", nodesArray);
							controllerArray.put(controllerObject);
						}
					}
				}
			}
		}
		return controllerArray.toString();
	}

	private JSONArray getValuesForAllControllers() throws JSONException {
		if (this.server == null) {
			this.initialize();
		}

		JSONArray controllerArray = new JSONArray();
		for (Entry<String, IController> entry : this.server.getControllers().entrySet()) {

			if (entry.getValue().isEnabled() && entry.getValue().getIdentifier() != null) {

				String controllerName = entry.getValue().getName();
				if (controllerName != null && !controllerName.equals("")) {
					JSONObject controllerObject = new JSONObject();
					Collection<ControllerNode> nodes = entry.getValue().getNodes();
					JSONArray nodesArray = new JSONArray();
					if (nodes != null) {
						for (ControllerNode node : nodes) {
							nodesArray.put(new JSONObject(node));
						}
					}
					controllerObject.put("name", controllerName);
					controllerObject.put("identifier", entry.getValue().getIdentifier());
					controllerObject.put("nodes", nodesArray);
					controllerArray.put(controllerObject);
				}
			}
		}
		return controllerArray;
	}

	private boolean isControllableValue(ValueTypes value) {
		switch (value) {
		case COLOR_BULB:
		case DIMMER:
		case GENERAL_COMMAND:
		case HEATING:
		case HTTP_COMMAND:
		case MUSIC_ACTION:
		case RADIO_STATION:
		case SWITCH_BINARY:
		case WAIT:
		case SEND_NOTIFICATION:
		case SOUND_NOTIFICATION:
		case SCENE_ACTIVATION:
		case SYSTEM_COMMAND:
			return true;
		default:
			return false;
		}
	}
}
