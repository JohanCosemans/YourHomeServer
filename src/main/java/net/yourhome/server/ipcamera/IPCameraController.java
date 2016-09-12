package net.yourhome.server.ipcamera;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import net.yourhome.server.net.rest.IPCameras;

public class IPCameraController extends AbstractController {
	private static volatile IPCameraController ipCameraController;
	private static Object lock = new Object();

	private IPCameraController() {
		log = Logger.getLogger("net.yourhome.server.ipcamera.IPCamera");
	}

	public static IPCameraController getInstance() {
		IPCameraController r = ipCameraController;
		if (r == null) {
			synchronized (lock) { // while we were waiting for the lock, another
				r = ipCameraController; // thread may have instantiated the
										// object
				if (r == null) {
					r = new IPCameraController();
					ipCameraController = r;
				}
			}
		}

		return ipCameraController;
	}

	@Override
	public JSONMessage parseNetMessage(JSONMessage message) {
		if (message instanceof SnapshotRequestMessage) {
			// Build original message
			SnapshotRequestMessage requestSnapshotMessage = (SnapshotRequestMessage) message;

			// Process message: take screenshot
			IPCamera camera = ipCameraController.getIPCamera(Integer.parseInt(requestSnapshotMessage.controlIdentifiers.getValueIdentifier()));
			if (camera != null) {
				File cameraSnapshot = camera.saveAndGetSnapshot(false);

				// Build result message
				SnapshotMessage snapshotMessage = new SnapshotMessage(message);
				if (cameraSnapshot != null) {
					String relativeSnapshotPath = IPCameras.getSnapshotUrl(camera.getId(), cameraSnapshot);
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
			log.error("Exception occured: ", e);
		} finally {
			try {
				if (camerasResult != null) {
					camerasResult.getStatement().close();
					camerasResult.close();
				}
			} catch (SQLException e) {
				log.error("Exception occured: ", e);
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
					log.error("Exception occured: ", e);
				}
			}

		} catch (SQLException e) {
			log.error("Exception occured: ", e);
		} finally {
			try {
				if (ipCamerasResult != null) {
					ipCamerasResult.getStatement().close();
					ipCamerasResult.close();
				}
			} catch (SQLException e) {
				log.error("Exception occured: ", e);
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

		log.info("Initialized");
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
