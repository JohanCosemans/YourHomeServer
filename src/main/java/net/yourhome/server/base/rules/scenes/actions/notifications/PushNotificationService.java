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

import net.yourhome.common.net.messagestructures.general.ClientNotificationMessage;
import net.yourhome.common.net.messagestructures.http.HttpCommand;
import net.yourhome.common.net.messagestructures.http.HttpCommandMessage;
import net.yourhome.common.net.model.Device;
import net.yourhome.server.base.BuildConfig;
import net.yourhome.server.base.DatabaseConnector;
import net.yourhome.server.base.rules.scenes.actions.notifications.fcm.FcmResponse;
import net.yourhome.server.base.rules.scenes.actions.notifications.fcm.Notification;
import net.yourhome.server.base.rules.scenes.actions.notifications.fcm.Pushraven;
import net.yourhome.server.http.HttpCommandController;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class PushNotificationService {

	private Map<String, Device> registeredDevices = new HashMap<String, Device>();
	private static Logger log = Logger.getLogger(PushNotificationService.class);
	private static volatile PushNotificationService instance;
	private static Object lock = new Object();

	private PushNotificationService() {

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
			PushNotificationService.log.error("Exception occured: ", e);
		} finally {
			try {
				if (result != null) {
					result.getStatement().close();
					result.close();
				}
			} catch (SQLException e) {
				PushNotificationService.log.error("Exception occured: ", e);
			}
		}

	}

	public static PushNotificationService getInstance() {
		PushNotificationService r = PushNotificationService.instance;
		if (r == null) {
			synchronized (PushNotificationService.lock) { // while we were
																// waiting for
																// the lock,
																// another
				r = PushNotificationService.instance; // thread may have
															// instantiated the
															// object
				if (r == null) {
					r = new PushNotificationService();
					PushNotificationService.instance = r;
				}
			}
		}
		return PushNotificationService.instance;
	}

	public void registerClient(Device device) throws SQLException {
		if (this.registeredDevices.get(device.getRegistrationId()) == null) {
			this.registeredDevices.put(device.getRegistrationId(), device);
			String dbSaveQuery = "INSERT into main.Notification_GCM ('registration_id', 'name', 'width', 'height') VALUES ('" + device.getRegistrationId() + "', '" + device.getName() + "', '" + device.getWidth() + "','" + device.getHeight() + "')";
			DatabaseConnector.getInstance().executeQuery(dbSaveQuery);

			PushNotificationService.log.info("Successfully registered device " + device.toString());
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

	private void sendMessage(Map<String, Object> messageVariables) {

        Pushraven.setKey(BuildConfig.GCM_API_CODE);
        Notification notification = new Notification();
        notification.data(messageVariables);
        notification.registration_ids(this.registeredDevices.keySet());
        FcmResponse response = Pushraven.push(notification);
        log.debug(response);

	}

	/**
	 * @return the registeredDevices
	 */
	public Map<String, Device> getRegisteredDevices() {
		return this.registeredDevices;
	}

}
