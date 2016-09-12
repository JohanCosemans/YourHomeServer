package net.yourhome.server.net.rest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import net.minidev.json.JSONArray;
import net.yourhome.common.base.enums.ControllerTypes;
import net.yourhome.common.net.model.ServerInfo;
import net.yourhome.server.IController;
import net.yourhome.server.base.Setting;
import net.yourhome.server.base.SettingsManager;
import net.yourhome.server.base.Util;
import net.yourhome.server.base.rules.scenes.actions.notifications.GoogleCloudMessagingService;
import net.yourhome.server.http.HttpCommandController;
import net.yourhome.server.net.Server;

@Path("/Info")
public class Info {
	private static Logger log = Logger.getLogger(Info.class);
	public static final String SERVER_INFO_FILE = "/serverinfo.json";

	// The initialize method will only be called when the controllers are needed
	// (in this way, the controllers are not initialized during the network
	// startup)
	private void initialize() {
	}

	// GET api/Info/
	@Produces({ MediaType.APPLICATION_JSON })
	@GET
	public ServerInfo getInfo() throws IOException, JSONException {
		return getServerInfo();
	}

	// GET api/Info/Settings
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("/Settings")
	@GET
	public String getSettings() throws IOException, JSONException {
		Map<String, Map<Setting, String>> allSettings = SettingsManager.getAllSettings();
		JSONArray returnArray = new JSONArray();

		for (String controllerIdentifier : allSettings.keySet()) {
			if(controllerIdentifier != ControllerTypes.DEMO.convert()) {
				JSONObject controllerInfo = new JSONObject();
				IController controller = Server.getInstance().getControllers().get(controllerIdentifier);
				controllerInfo.put("controllerIdentifier", controllerIdentifier);
				controllerInfo.put("controllerName", controller.getName());
	
				JSONArray settingsArray = new JSONArray();
				Map<Setting, String> settingsMap = allSettings.get(controllerIdentifier);
				for (Entry<Setting, String> settingsEntry : settingsMap.entrySet()) {
					JSONObject entryObject = new JSONObject();
					entryObject.put("setting", new JSONObject(settingsEntry.getKey()));
					entryObject.put("value", settingsEntry.getValue());
					settingsArray.add(entryObject);
				}
	
				controllerInfo.put("settings", settingsArray);
				returnArray.add(controllerInfo);
			}
		}
		JSONObject returnObject = new JSONObject();
		returnObject.put("controllers", returnArray);
		return returnObject.toString();
	}

	// PUT api/Info/Settings
	@Path("/Settings")
	@PUT
	public Response updateSettings(@Context final UriInfo uriInfo, String bodyContent) throws IOException {
		JSONObject updateObject = new JSONObject(bodyContent);
		String[] controllers = JSONObject.getNames(updateObject);
		if (controllers != null) {
			for (String controller : controllers) {
				JSONObject controllerSettings = updateObject.getJSONObject(controller);
				String[] settings = JSONObject.getNames(controllerSettings);
				if (settings != null) {
					for (String setting : settings) {
						String settingValue = controllerSettings.get(setting).toString();
						SettingsManager.setStringValue(controller, setting, settingValue);
					}
				}
			}
		}
		SettingsManager.storeSettingsAndRestart();
		return Response.ok().build();
	}
	
	// GET api/Info/Devices
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("/Devices")
	@GET
	public String getDevices() throws IOException, JSONException {
		return new JSONObject(GoogleCloudMessagingService.getInstance().getRegisteredDevices()).toString();
	}
	// DELETE api/Info/Devices/{deviceId}
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("/Devices/{deviceId}")
	@DELETE
	public Response deleteDevice(@PathParam("deviceId") final String deviceId) throws SQLException {
		GoogleCloudMessagingService.getInstance().unregisterClient(deviceId);
		return Response.ok().build();
	}

	public static ServerInfo getServerInfo() throws IOException, JSONException {
		File serverInfoFile = new File(SettingsManager.getBasePath(), Server.FILESERVER_PATH + SERVER_INFO_FILE);
		if(serverInfoFile.exists()) {
			String serverInfoString = Util.readFile(serverInfoFile);
			JSONObject serverInfoObject = new JSONObject(serverInfoString);
			return new ServerInfo(serverInfoObject);
		}else {
			return new ServerInfo();
		}
	}

	public static boolean writeServerInfo(ServerInfo info) throws IOException, JSONException {
		File serverInfoFile = new File(SettingsManager.getBasePath(), Server.FILESERVER_PATH + SERVER_INFO_FILE);
		String serverInfoString = info.serialize().toString();
		Util.writeToFile(new ByteArrayInputStream(serverInfoString.getBytes()), serverInfoFile);
		return true;
	}

}