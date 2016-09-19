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
package net.yourhome.server.base.rules.scenes.actions.notifications;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.yourhome.common.net.messagestructures.general.ClientNotificationMessage;
import net.yourhome.common.net.messagestructures.http.HttpCommand;
import net.yourhome.common.net.model.Device;
import net.yourhome.server.base.BuildConfig;
import net.yourhome.server.base.DatabaseConnector;
import net.yourhome.server.http.HttpCommandController;

public class GoogleCloudMessagingService {
	public static final String GOOGLE_CLOUD_MESSAGING = "https://android.googleapis.com/gcm/send";

	private Map<String, Device> registeredDevices = new HashMap<String, Device>();
	private static Logger log = Logger.getLogger(GoogleCloudMessagingService.class);
	private static volatile GoogleCloudMessagingService instance;
	private static Object lock = new Object();

	private GoogleCloudMessagingService() {

		// Read registration ID's from database
		String dbSelect = "SELECT * from main.Notification_GCM";
		ResultSet result = null;
		try {
			result = DatabaseConnector.getInstance().executeSelect(dbSelect);
			while (result.next()) {
				Device newDevice = new Device(result.getString("registration_id"), result.getString("name"), result.getInt("width"), result.getInt("height"));
				this.registeredDevices.put(newDevice.getRegistrationId(), newDevice);
			}
		} catch (SQLException e) {
			GoogleCloudMessagingService.log.error("Exception occured: ", e);
		} finally {
			try {
				if (result != null) {
					result.getStatement().close();
					result.close();
				}
			} catch (SQLException e) {
				GoogleCloudMessagingService.log.error("Exception occured: ", e);
			}
		}

	}

	public static GoogleCloudMessagingService getInstance() {
		GoogleCloudMessagingService r = GoogleCloudMessagingService.instance;
		if (r == null) {
			synchronized (GoogleCloudMessagingService.lock) { // while we were
																// waiting for
																// the lock,
																// another
				r = GoogleCloudMessagingService.instance; // thread may have
															// instantiated the
															// object
				if (r == null) {
					r = new GoogleCloudMessagingService();
					GoogleCloudMessagingService.instance = r;
				}
			}
		}
		return GoogleCloudMessagingService.instance;
	}

	public void registerClient(Device device) throws SQLException {
		if (this.registeredDevices.get(device.getRegistrationId()) == null) {
			this.registeredDevices.put(device.getRegistrationId(), device);
			String dbSaveQuery = "INSERT into main.Notification_GCM ('registration_id', 'name', 'width', 'height') VALUES ('" + device.getRegistrationId() + "', '" + device.getName() + "', '" + device.getWidth() + "','" + device.getHeight() + "')";
			DatabaseConnector.getInstance().executeQuery(dbSaveQuery);

			GoogleCloudMessagingService.log.info("Successfully registered device " + device.toString());
		}
	}

	public void unregisterClient(String deviceId) throws SQLException {
		this.registeredDevices.remove(deviceId);
		String sql = "DELETE FROM main.Notification_GCM WHERE registration_id = ?";
		PreparedStatement stm = DatabaseConnector.getInstance().prepareStatement(sql);
		stm.setString(1, deviceId);
		DatabaseConnector.getInstance().executePreparedUpdate(stm);
	}

	public void sendMessage(ClientNotificationMessage message) {
		this.sendMessage(message.getMessageMap());
	}

	private void sendMessage(Map<String, String> messageVariables) {

		HttpCommandController controller = HttpCommandController.getInstance();
		HttpCommand command = new HttpCommand();

		command.setHttpMethod("POST");
		command.setMessageType("application/json");
		command.setMessageBody(this.getMessageBody(messageVariables).toString());
		command.setUrl(GoogleCloudMessagingService.GOOGLE_CLOUD_MESSAGING);
		command.addHeader("Authorization", "key=" + BuildConfig.GCM_API_CODE);

		try {
			controller.sendHttpCommand(command);
		} catch (Exception e) {
			GoogleCloudMessagingService.log.error("Exception occured: ", e);
		}

	}

	/**
	 * @return the registeredDevices
	 */
	public Map<String, Device> getRegisteredDevices() {
		return this.registeredDevices;
	}

	private JSONObject getMessageBody(Map<String, String> dataVariables) {
		JSONArray registrationIDs = new JSONArray();
		JSONObject resultObj = new JSONObject();
		JSONObject dataObj = new JSONObject();

		try {

			// Parse data fields
			for (Entry<String, String> mapEntry : dataVariables.entrySet()) {
				try {
					dataObj.put(mapEntry.getKey(), mapEntry.getValue());
				} catch (JSONException e) {
					GoogleCloudMessagingService.log.error("Exception occured: ", e);
				}
			}
			resultObj.put("data", dataObj);

			// Parse registration strings
			for (String registrationId : this.registeredDevices.keySet()) {
				registrationIDs.put(registrationId);
			}

			resultObj.put("registration_ids", registrationIDs);

		} catch (JSONException e) {
			GoogleCloudMessagingService.log.error("Exception occured: ", e);
		}

		return resultObj;
	}

}
