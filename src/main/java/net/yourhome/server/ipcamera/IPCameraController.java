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
package net.yourhome.server.ipcamera;

import net.yourhome.common.base.enums.ControllerTypes;
import net.yourhome.common.base.enums.ValueTypes;
import net.yourhome.common.net.messagestructures.JSONMessage;
import net.yourhome.common.net.messagestructures.ipcamera.IPCameraMessage;
import net.yourhome.common.net.messagestructures.ipcamera.SnapshotMessage;
import net.yourhome.common.net.messagestructures.ipcamera.SnapshotRequestMessage;
import net.yourhome.common.net.model.binding.ControlIdentifiers;
import net.yourhome.server.AbstractController;
import net.yourhome.server.ControllerNode;
import net.yourhome.server.ControllerValue;
import net.yourhome.server.base.DatabaseConnector;
import net.yourhome.server.base.Setting;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class IPCameraController extends AbstractController {
	private static volatile IPCameraController ipCameraController;
	private static Object lock = new Object();

	private IPCameraController() {
		this.log = Logger.getLogger("net.yourhome.server.ipcamera.IPCamera");
	}

	public static IPCameraController getInstance() {
		IPCameraController r = net.yourhome.server.ipcamera.IPCameraController.ipCameraController;
		if (r == null) {
			synchronized (net.yourhome.server.ipcamera.IPCameraController.lock) { // while we were waiting
														// for the lock, another
				r = net.yourhome.server.ipcamera.IPCameraController.ipCameraController; // thread may have
															// instantiated the
				// object
				if (r == null) {
					r = new IPCameraController();
					net.yourhome.server.ipcamera.IPCameraController.ipCameraController = r;
				}
			}
		}

		return net.yourhome.server.ipcamera.IPCameraController.ipCameraController;
	}

	@Override
	public JSONMessage parseNetMessage(JSONMessage message) {
		if (message instanceof SnapshotRequestMessage) {
			// Build original message
			SnapshotRequestMessage requestSnapshotMessage = (SnapshotRequestMessage) message;

			// Process message: take screenshot
			IPCamera camera = net.yourhome.server.ipcamera.IPCameraController.ipCameraController.getIPCamera(Integer.parseInt(requestSnapshotMessage.controlIdentifiers.getValueIdentifier()));
			if (camera != null) {
				File cameraSnapshot = camera.saveAndGetSnapshot(false);

				// Build result message
				SnapshotMessage snapshotMessage = new SnapshotMessage(message);
				if (cameraSnapshot != null) {
					String relativeSnapshotPath = net.yourhome.server.net.rest.IPCameraController.getSnapshotUrl(camera.getId(), cameraSnapshot);
					snapshotMessage.snapshotUrl = relativeSnapshotPath;
				}
				snapshotMessage.videoPath = camera.getVideoUrl();
				snapshotMessage.broadcast = false;
				return snapshotMessage;
			}
		}

		return null;
	}

	public JSONObject getCameraList() {
		JSONObject completeObject = new JSONObject();
		try {
			completeObject.put("cameras", new JSONArray(this.getAllIPCameras()));
		} catch (JSONException e) {
		}
		return completeObject;
	}

	public String saveIPCamera(IPCameraMessage message) throws SQLException {
		String sql = "insert into main.IP_Cameras ('name', 'snapshotUrl', 'videoUrl')" + " VALUES ('" + message.name + "','" + message.snapshotUrl + "','" + message.videoUrl + "')";
		int id = DatabaseConnector.getInstance().insertConfigurationValue(sql);
		return "{ \"id\" : " + id + " }";
	}

	public String changeIPCamera(IPCameraMessage message) throws SQLException {
		String sql = "UPDATE main.IP_Cameras SET   'name'='" + message.name + "', " + "'snapshotUrl'='" + message.snapshotUrl + "', " + "'videoUrl'='" + message.videoUrl + "' " + " WHERE id = '" + message.id + "'";

		boolean result = DatabaseConnector.getInstance().executeQuery(sql);
		return "{ \"result\" : " + result + " }";
	}

	public boolean deleteIPCamera(int cameraId) throws SQLException {
		String sql = "delete from main.IP_Cameras where id = '" + cameraId + "'";
		return DatabaseConnector.getInstance().executeQuery(sql);
	}

	public IPCamera getIPCamera(int cameraId) {
		IPCamera returnCamera = null;

		String sql = "SELECT * from main.IP_Cameras where id = '" + cameraId + "'";
		ResultSet camerasResult = null;
		try {
			camerasResult = DatabaseConnector.getInstance().executeSelect(sql);
			returnCamera = null;
			while (camerasResult.next()) {
				returnCamera = new IPCamera(camerasResult);
			}
		} catch (SQLException e) {
			this.log.error("Exception occured: ", e);
		} finally {
			try {
				if (camerasResult != null) {
					camerasResult.getStatement().close();
					camerasResult.close();
				}
			} catch (SQLException e) {
				this.log.error("Exception occured: ", e);
			}
		}

		return returnCamera;
	}

	public List<IPCamera> getAllIPCameras() {

		String sql = "SELECT * from main.IP_Cameras";
		ResultSet ipCamerasResult = null;
		List<IPCamera> cameras = new ArrayList<IPCamera>();
		try {
			ipCamerasResult = DatabaseConnector.getInstance().executeSelect(sql);

			while (ipCamerasResult.next()) {
				try {
					cameras.add(new IPCamera(ipCamerasResult));
				} catch (SQLException e) {
					this.log.error("Exception occured: ", e);
				}
			}

		} catch (SQLException e) {
			this.log.error("Exception occured: ", e);
		} finally {
			try {
				if (ipCamerasResult != null) {
					ipCamerasResult.getStatement().close();
					ipCamerasResult.close();
				}
			} catch (SQLException e) {
				this.log.error("Exception occured: ", e);
			}
		}
		return cameras;
	}

	@Override
	public String getIdentifier() {
		return ControllerTypes.IPCAMERA.convert();
	}

	@Override
	public String getName() {
		return "IP Cameras";
	}

	@Override
	public void init() {
		super.init();

		this.log.info("Initialized");
	}

	@Override
	public List<JSONMessage> initClient() {
		return null;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public List<ControllerNode> getNodes() {
		List<ControllerNode> returnList = new ArrayList<ControllerNode>();
		ControllerNode cameraNode = new ControllerNode(this, "Cameras", "Cameras", "");
		for (IPCamera c : this.getAllIPCameras()) {
			cameraNode.addValue(new ControllerValue("" + c.getId(), c.getName(), ValueTypes.IP_CAMERA));
		}
		returnList.add(cameraNode);

		return returnList;
	}

	@Override
	public String getValueName(ControlIdentifiers valueIdentifier) {
		try {
			IPCamera camera = this.getIPCamera(Integer.parseInt(valueIdentifier.getValueIdentifier()));
			if (camera != null) {
				return camera.getName();
			}
		} catch (NumberFormatException e) {
		}

		return "Unkown";
	}

	@Override
	public List<ControllerNode> getTriggers() {
		return null;
	}

	@Override
	public String getValue(ControlIdentifiers valueIdentifiers) {
		return null;
	}

	@Override
	public boolean isInitialized() {
		return true;
	}

	@Override
	public List<Setting> getSettings() {
		// TODO Auto-generated method stub
		return null;
	}
}
