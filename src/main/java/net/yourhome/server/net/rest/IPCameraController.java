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

import net.yourhome.common.net.messagestructures.ipcamera.IPCameraMessage;
import net.yourhome.server.ipcamera.IPCamera;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.sql.SQLException;

@Path("/api/IPCameras")
public class IPCameraController {
	private net.yourhome.server.ipcamera.IPCameraController controller;
	private static Logger log = Logger.getLogger(IPCameraController.class);

	// The initialize method will only be called when the controllers are needed
	// (in this way, the controllers are not initialized during the network
	// startup)
	private void initialize() {
		this.controller = net.yourhome.server.ipcamera.IPCameraController.getInstance();
	}

	// GET api/IPCameras/
	@Produces({ MediaType.APPLICATION_JSON })
	@GET
	public String getIPCameras() {
		if (this.controller == null) {
			this.initialize();
		}

		return this.controller.getCameraList().toString();
	}

	// GET api/IPCameras/1
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("{cameraId}")
	@GET
	public String getIPCamera(@Context final UriInfo uriInfo, String bodyContent, @PathParam("cameraId") final int cameraId) {
		if (this.controller == null) {
			this.initialize();
		}

		return new JSONObject(this.controller.getIPCamera(cameraId)).toString();
	}

	// PUT api/IPCameras/1
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("{cameraId}")
	@PUT
	public String editIPCamera(@Context final UriInfo uriInfo, String bodyContent, @PathParam("cameraId") final int cameraId) throws SQLException {
		if (this.controller == null) {
			this.initialize();
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
				return this.controller.changeIPCamera(ipCameraMessage);
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
		if (this.controller == null) {
			this.initialize();
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

				return this.controller.saveIPCamera(ipCameraMessage);
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
		IPCamera camera = net.yourhome.server.ipcamera.IPCameraController.getInstance().getIPCamera(cameraId);
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
		IPCamera camera = net.yourhome.server.ipcamera.IPCameraController.getInstance().getIPCamera(cameraId);
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
		if (this.controller == null) {
			this.initialize();
		}
		this.controller.deleteIPCamera(cameraId);

		return "";
	}

}