package net.yourhome.server.net.rest.zwave;

import java.math.BigInteger;
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
import org.zwave4j.ValueId;

import net.yourhome.server.zwave.Value;
import net.yourhome.server.zwave.ZWaveController;
import net.yourhome.server.zwave.ZWaveController.ZWaveScene;

@Path("/ZWave/Scenes")
public class Scenes {
	private ZWaveController controller;
	private static Logger log = Logger.getLogger(Scenes.class);

	// The initialize method will only be called when the controllers are needed
	// (in this way, the controllers are not initialized during the network
	// startup)
	private void initialize() {
		controller = ZWaveController.getInstance();
	}

	@Produces({ MediaType.APPLICATION_JSON })
	@GET
	public String getAllScenes() {
		if (controller == null) {
			initialize();
		}
		List<ZWaveScene> allScenes = this.controller.getAllScenes();
		JSONArray convertedObject = new JSONArray(allScenes);
		return convertedObject.toString();
	}

	@Produces({ MediaType.APPLICATION_JSON })
	@GET
	@Path("{sceneId}")
	public String getScenes(@PathParam("sceneId") final short sceneId) throws Exception {
		if (controller == null) {
			initialize();
		}
		ZWaveScene selectedScene = this.controller.new ZWaveScene(sceneId, true);
		JSONObject convertedObject = new JSONObject(selectedScene);
		return convertedObject.toString();
	}

	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	public String createScene(String bodyContent) {
		if (controller == null) {
			initialize();
		}
		try {
			JSONObject bodyContentObj = new JSONObject(bodyContent);
			ZWaveScene newScene = controller.createScene();

			try {
				String sceneLabel = bodyContentObj.getString("label");
				newScene.setLabel(sceneLabel);
			} catch (JSONException e) {
				newScene.setLabel("Untitled");
			}

			try {
				String sceneIconUrl = bodyContentObj.getString("iconUrl");
				newScene.setIconUrl(sceneIconUrl);
			} catch (JSONException e) {
				newScene.setIconUrl("");
			}

			if (newScene != null) {
				JSONObject sceneObject = new JSONObject(newScene);
				return sceneObject.toString();
			} else {
				return "{ \"status\" : \"ERROR\"}";
			}
		} catch (JSONException e) {
			log.error("Exception occured: ", e);
		}

		return "{ \"status\" : \"ERROR\"}";

	}

	@DELETE
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("{sceneId}")
	public String deleteScene(@Context final UriInfo uriInfo, @PathParam("sceneId") final short sceneId) {

		if (controller == null) {
			initialize();
		}

		if (controller.removeScene(sceneId)) {
			return "{ \"status\" : \"OK\"}";
		} else {
			return "{ \"status\" : \"ERROR\"}";
		}
	}

	@PUT
	@Path("/{sceneId}")
	public Response changeScene(@Context final UriInfo uriInfo, @PathParam("sceneId") final short sceneId, String bodyContent) throws Exception {
		if (controller == null) {
			initialize();
		}
		boolean result = false;
		JSONObject changedScene;
		try {
			changedScene = new JSONObject(bodyContent);
			String newLabel = changedScene.getString("label");
			String sceneIconUrl = changedScene.getString("iconUrl");

			ZWaveScene scene = controller.new ZWaveScene(sceneId, false);
			if (scene != null) {
				scene.setLabel(newLabel);
				scene.setIconUrl(sceneIconUrl);
			}

			return Response.ok().build();

		} catch (JSONException e) {
			return Response.serverError().build();
		}
	}

	@DELETE
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("/{sceneId}/{homeId}/{nodeId}/{instance}/{valueId}")
	public Response deleteSceneValue(@Context final UriInfo uriInfo, @PathParam("sceneId") final short sceneId, @PathParam("homeId") final long homeId, @PathParam("nodeId") final short nodeId, @PathParam("instance") final short instance, @PathParam("valueId") final BigInteger valueId) throws Exception {
		if (controller == null) {
			initialize();
		}
		boolean result = false;

		ZWaveScene scene = controller.new ZWaveScene(sceneId, true);
		if (scene != null) {
			Value valueObj = controller.getValue(homeId, nodeId, instance, valueId);
			if(valueObj != null) {
				result = scene.removeSceneValue(valueObj.getOriginalValueId());
			}
		}
		if (result) {
			return Response.ok().build();
		} else {
			return Response.serverError().build();
		}
	}

	@PUT
	@Path("/{sceneId}/{homeId}/{nodeId}/{instance}/{valueId}/{value}")
	public Response changeSceneValue(@Context final UriInfo uriInfo, @PathParam("sceneId") final short sceneId, @PathParam("homeId") final long homeId, @PathParam("nodeId") final short nodeId, @PathParam("instance") final short instance, @PathParam("valueId") final BigInteger valueId, @PathParam("value") final String value, String bodyContent) throws Exception {
		if (controller == null) {
			initialize();
		}
		boolean result = false;
		ZWaveScene scene = controller.new ZWaveScene(sceneId, true);
		if (scene != null) {
			Value valueObj = controller.getValue(homeId, nodeId, instance, valueId);
			result = scene.setSceneValue(sceneId, valueObj.getOriginalValueId(), value);
		}
		if (result) {
			return Response.ok().build();
		} else {
			return Response.serverError().build();
		}
	}

	/*
	 * 
	 * { homeId nodeId instance valueId value }
	 * 
	 * 
	 */
	@POST
	@Path("/{sceneId}/")
	public Response addSceneValue(@Context final UriInfo uriInfo, @PathParam("sceneId") final short sceneId, String bodyContent) throws Exception {
		if (controller == null) {
			initialize();
		}
		boolean result = false;
		try {
			JSONObject bodyContentObj = new JSONObject(bodyContent);
			ZWaveScene scene = controller.new ZWaveScene(sceneId, true);
			short nodeId = (short) bodyContentObj.getInt("nodeId");
			long homeId = bodyContentObj.getLong("homeId");
			short instance = (short) bodyContentObj.getInt("instance");
			BigInteger valueId = BigInteger.valueOf(bodyContentObj.getLong("valueId"));
			String value = bodyContentObj.getString("value");
			Value valueObj = controller.getValue(homeId, nodeId, instance, valueId);
			if (valueObj != null) {
				if (!scene.hasSceneValue(valueObj.getOriginalValueId())) {
					result = scene.addSceneValue(sceneId, valueObj.getOriginalValueId(), value);
				} else {
					throw new Exception("Value already extists!");
				}
			}
		} catch (JSONException e) {
			log.error("Exception occured: ", e);
			return Response.serverError().build();
		}
		if (result) {
			return Response.ok().build();
		} else {
			return Response.serverError().build();
		}
	}
}
