package net.yourhome.server.net.rest.rules;

import java.sql.SQLException;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.yourhome.server.base.rules.scenes.Scene;
import net.yourhome.server.base.rules.scenes.SceneManager;

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
			log.error("Exception occured: ", e);
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
