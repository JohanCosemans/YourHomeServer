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
package net.yourhome.server.net.rest.rules;

import net.yourhome.server.base.rules.scenes.Scene;
import net.yourhome.server.base.rules.scenes.SceneManager;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.sql.SQLException;
import java.util.List;

@Path("/Scenes")
public class Scenes {

	private static Logger log = Logger.getLogger(Scenes.class);

	// POST api/Scenes
	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	public String Post(@Context final UriInfo uriInfo, String bodyContent) throws SQLException, JSONException {
		// A. Parse body
		JSONObject sceneObject = new JSONObject(bodyContent);
		Scene scene = new Scene(sceneObject);
		int sceneId = SceneManager.save(scene);
		return "{ \"id\" : " + sceneId + " }";
	}

	@PUT
	@Path("/{sceneId}")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response Put(@Context final UriInfo uriInfo, @PathParam("sceneId") final int sceneId, String bodyContent) throws SQLException {

		try {
			JSONObject sceneObject = new JSONObject(bodyContent);
			Scene sceneToUpdate = new Scene(sceneObject);
			sceneToUpdate.setId(sceneId);
			SceneManager.save(sceneToUpdate);

		} catch (JSONException e) {
			Scenes.log.error("Exception occured: ", e);
			return Response.serverError().build();
		}

		return Response.ok().build();
	}

	// GET api/Scenes
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public String Get(@Context final UriInfo uriInfo, String bodyContent) throws SQLException {
		JSONArray resultArray = new JSONArray();
		List<Scene> allScenes = SceneManager.getAllScenes();
		for (Scene scene : allScenes) {
			resultArray.put(scene.getSourceJsonObject());
		}
		return resultArray.toString();

	}

	// GET api/Scenes/12
	@GET
	@Path("/{id}")
	@Produces({ MediaType.APPLICATION_JSON })
	public String Get(@Context final UriInfo uriInfo, @PathParam("id") final int id, @PathParam("function") final String function, String bodyContent) {

		return "";
	}

	// DELETE api/Scenes/12
	@DELETE
	@Path("/{id}")
	public Response Delete(@Context final UriInfo uriInfo, @PathParam("id") final int scheduleId, @PathParam("function") final String function, String bodyContent) throws SQLException, JSONException {
		Scene scene = SceneManager.getScene(scheduleId);
		SceneManager.delete(scene);
		return Response.ok().build();
	}

	// GET api/Scenes/Activate/{sceneId}
	@GET
	@Path("/Activate/{sceneId}")
	@Produces({ MediaType.APPLICATION_JSON })
	public String GetSimulation(@Context final UriInfo uriInfo, @PathParam("sceneId") final int sceneId, String bodyContent) throws SQLException, JSONException {
		if (SceneManager.getScene(sceneId).activate()) {
			return "{ \"result\": \"Scene activated\"}";
		} else {
			return "{ \"result\": \"No scene activated\"}";
		}
	}

}
