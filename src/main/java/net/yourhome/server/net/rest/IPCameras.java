package net.yourhome.server.net.rest;

import java.io.File;
import java.sql.SQLException;

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
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import net.yourhome.common.net.messagestructures.ipcamera.IPCameraMessage;
import net.yourhome.server.http.HttpCommandController;
import net.yourhome.server.ipcamera.IPCamera;
import net.yourhome.server.ipcamera.IPCameraController;

@Path("/IPCameras")
public class IPCameras {
	private IPCameraController controller;
	private static Logger log = Logger.getLogger(IPCameras.class);

	// The initialize method will only be called when the controllers are needed
	// (in this way, the controllers are not initialized during the network
	// startup)
	private void initialize() {
		controller = IPCameraController.getInstance();
	}

	// GET api/IPCameras/
	@Produces({ MediaType.APPLICATION_JSON })
	@GET
	public String getIPCameras() {
		if (controller == null) {
			initialize();
		}

		return controller.getCameraList().toString();
	}

	// GET api/IPCameras/1
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("{cameraId}")
	@GET
	public String getIPCamera(@Context final UriInfo uriInfo, String bodyContent, @PathParam("cameraId") final int cameraId) {
		if (controller == null) {
			initialize();
		}

		return new JSONObject(controller.getIPCamera(cameraId)).toString();
	}

	// PUT api/IPCameras/1
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("{cameraId}")
	@PUT
	public String editIPCamera(@Context final UriInfo uriInfo, String bodyContent, @PathParam("cameraId") final int cameraId) throws SQLException {
		if (controller == null) {
			initialize();
		}

		try {
			JSONObject json = new JSONObject(bodyContent);
			String messageType = json.getString("type");

			if (messageType.equals("IPCamera")) {
				IPCameraMessage ipCameraMessage = new IPCameraMessage();
				ipCameraMessage.id = json.getInt("id");
				ipCameraMessage.name = json.getString("name");
				ipCameraMessage.videoUrl = json.getString("videoUrl");
				ipCameraMessage.snapshotUrl = json.getString("snapshotUrl");
				return controller.changeIPCamera(ipCameraMessage);
			}
		} catch (JSONException e) {
			return "";
		}
		return "";
	}

	// POST api/IPCameras/
	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	public String createNewCamera(@Context final UriInfo uriInfo, String bodyContent) throws SQLException {
		if (controller == null) {
			initialize();
		}

		try {
			JSONObject json = new JSONObject(bodyContent);
			String messageType = json.getString("type");

			if (messageType.equals("IPCamera")) {
				IPCameraMessage ipCameraMessage = new IPCameraMessage();
				try {
					ipCameraMessage.id = json.getInt("id");
				} catch (JSONException e) {
				} // Will be initial

				ipCameraMessage.name = json.getString("name");
				ipCameraMessage.videoUrl = json.getString("videoUrl");
				ipCameraMessage.snapshotUrl = json.getString("snapshotUrl");

				return controller.saveIPCamera(ipCameraMessage);
			}
		} catch (JSONException e) {
			return "";
		}
		return "";
	}

	// GET api/IPCameras/{cameraId}/Snapshot
	@Produces("image/jpg")
	@Path("{cameraId}/Snapshot")
	@GET
	public Response getCameraSnapshot(@Context final UriInfo uriInfo, String bodyContent, @PathParam("cameraId") final int cameraId) {
		IPCamera camera = IPCameraController.getInstance().getIPCamera(cameraId);
		if (camera != null) {
			File cameraSnapshot = camera.saveAndGetSnapshot(false);
			ResponseBuilder response = Response.ok(cameraSnapshot);
			response.header("Content-Disposition", "attachment; filename=" + cameraSnapshot.getName());
			return response.build();
		}
		return Response.serverError().build();
	}

	// GET api/IPCameras/3/Snapshot/file-01012013.113020
	@Produces("image/jpg")
	@Path("{cameraId}/Snapshot/{fileName}")
	@GET
	public Response getCameraSnapshotByFilename(@Context final UriInfo uriInfo, String bodyContent, @PathParam("cameraId") final int cameraId, @PathParam("fileName") final String fileName) {
		IPCamera camera = IPCameraController.getInstance().getIPCamera(cameraId);
		if (camera != null) {
			File cameraSnapshot = camera.getSnapshotByName(fileName);
			ResponseBuilder response = Response.ok(cameraSnapshot);
			response.header("Content-Disposition", "attachment; filename=" + cameraSnapshot.getName());
			return response.build();
		}
		return Response.serverError().build();
	}

	public static String getSnapshotUrl(int cameraId, File filename) {
		return "/api/IPCameras/" + cameraId + "/Snapshot/" + filename.getName();
	}

	// DELETE api/IPCameras/12
	@DELETE
	@Produces({ MediaType.TEXT_HTML })
	@Path("{cameraId}")
	public String deleteRadio(@Context final UriInfo uriInfo, @PathParam("cameraId") final int cameraId, String bodyContent) throws SQLException {
		if (controller == null) {
			initialize();
		}
		controller.deleteIPCamera(cameraId);

		return "";
	}

}