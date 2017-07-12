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
package net.yourhome.server.thermostat;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import net.yourhome.common.base.enums.ControllerTypes;
import net.yourhome.common.base.enums.MobileNotificationTypes;
import net.yourhome.common.base.enums.ValueTypes;
import net.yourhome.common.net.messagestructures.JSONMessage;
import net.yourhome.common.net.messagestructures.general.*;
import net.yourhome.common.net.messagestructures.http.HttpCommand;
import net.yourhome.common.net.messagestructures.http.HttpCommandMessage;
import net.yourhome.common.net.messagestructures.thermostat.SetAwayMessage;
import net.yourhome.common.net.model.binding.ControlIdentifiers;
import net.yourhome.server.AbstractController;
import net.yourhome.server.ControllerNode;
import net.yourhome.server.ControllerValue;
import net.yourhome.server.IController;
import net.yourhome.server.base.*;
import net.yourhome.server.base.rules.scenes.actions.notifications.PushNotificationService;
import net.yourhome.server.http.HttpCommandController;
import net.yourhome.server.net.Server;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

public class VaillantVSMartController extends AbstractController {
	public enum Settings {

		THERMOSTAT_USERNAME(
				new Setting("THERMOSTAT_USERNAME", "VSMart Thermostat Username", "my@email.address")
		), THERMOSTAT_PASSWORD(
				new Setting("THERMOSTAT_PASSWORD", "VSMart Thermostat Password")
		), HOURS_AWAY(
				new Setting("HOURS_AWAY", "Hours away when away mode is activated (0 = no end)")
		);

		private Setting setting;

		private Settings(Setting setting) {
			this.setting = setting;
		}

		public Setting get() {
			return this.setting;
		}
	}

	private HttpCommandController httpController;
	private boolean enabled;

	// Thermostat API
	private final String BASE_URL = "https://app.netatmo.net";
	private final String TOKEN_URL = this.BASE_URL + "/oauth2/token";
	private final String GET_THERMOSTAT_DATA = this.BASE_URL + "/api/getthermostatsdata";
	private final String SET_MINOR_MODE = this.BASE_URL + "/api/setminormode";

	// Vaillant app specifics
	private final String CLIENT_ID = "na_client_android_vaillant";
	private final String APP_VERSION = "1.0.0.22";
	private final String USER_PREFIX = "vaillant";

	// HomeServer value IDs
	private final String SETPOINT_TEMPERATURE = "setpointTemperature";
	private final String INDOOR_TEMPERATURE = "indoorTemperature";
	private final String OUTDOOR_TEMPERATURE = "outdoorTemperature";
	private final String MANUAL_OVERRIDE_ACTIVE = "manualOverride";
	private final String AWAY_ACTIVE = "awayActive";
	private final String AWAY_ACTIVE_UNTIL_TXT = "awayActiveUntilTxt";

	private String username;
	private String password;
	private String authToken;
	private String refreshToken;
	private String deviceId;
	private String moduleId;

	private Map<String, ControllerNode> controllerMap = new LinkedHashMap<String, ControllerNode>();
	private Map<String, ValueChangedMessage> values = new HashMap<String, ValueChangedMessage>();

	private static volatile VaillantVSMartController instance;
	private static Object lock = new Object();

	private VaillantVSMartController() {
		this.log = Logger.getLogger("net.yourhome.server.thermostat.Thermostat-Vaillant");
	}

	public static VaillantVSMartController getInstance() {
		VaillantVSMartController r = VaillantVSMartController.instance;
		if (r == null) {
			synchronized (VaillantVSMartController.lock) { // while we were
															// waiting for the
															// lock, another
				r = VaillantVSMartController.instance; // thread may have
														// instantiated
				// the object
				if (r == null) {
					r = new VaillantVSMartController();
					VaillantVSMartController.instance = r;
				}
			}
		}
		return VaillantVSMartController.instance;
	}

	private boolean getLoginToken() throws Exception {

		HttpCommand command = new HttpCommand();
		command.setHttpMethod("POST");

		String messageBody = "";
		Map<String, String> messageBodyMap = new HashMap<String, String>();
		messageBodyMap.put("client_id", URLEncoder.encode(this.CLIENT_ID, "UTF-8"));
		messageBodyMap.put("client_secret", URLEncoder.encode(BuildConfig.VAILLANT_CLIENT_SECRET, "UTF-8"));
		messageBodyMap.put("app_version", URLEncoder.encode(this.APP_VERSION, "UTF-8"));
		messageBodyMap.put("username", URLEncoder.encode(this.username, "UTF-8"));
		messageBodyMap.put("password", URLEncoder.encode(this.password, "UTF-8"));
		messageBodyMap.put("grant_type", URLEncoder.encode("password", "UTF-8"));
		messageBodyMap.put("user_prefix", URLEncoder.encode(this.USER_PREFIX, "UTF-8"));

		for (Map.Entry<String, String> param : messageBodyMap.entrySet()) {
			messageBody += param.getKey() + "=" + param.getValue() + "&";
		}

		command.setMessageBody(messageBody);
		command.setUrl(this.TOKEN_URL);
		command.setMessageType("application/x-www-form-urlencoded; charset=UTF-8");
		HttpCommandMessage returnMessage = this.httpController.sendHttpCommand(command);

		// Parse result
		JSONObject json = new JSONObject(returnMessage.response);

		// Read tokens
		this.authToken = json.getString("access_token");
		this.refreshToken = json.getString("refresh_token");

		if (this.authToken != null) {
			return true;
		}
		return false;
	}

	private void refreshToken() throws Exception {

		HttpCommand command = new HttpCommand();
		command.setHttpMethod("POST");

		String messageBody = "";
		Map<String, String> messageBodyMap = new HashMap<String, String>();
		messageBodyMap.put("client_id", URLEncoder.encode(this.CLIENT_ID, "UTF-8"));
		messageBodyMap.put("client_secret", URLEncoder.encode(BuildConfig.VAILLANT_CLIENT_SECRET, "UTF-8"));
		messageBodyMap.put("app_version", URLEncoder.encode(this.APP_VERSION, "UTF-8"));
		messageBodyMap.put("grant_type", URLEncoder.encode("refresh_token", "UTF-8"));
		messageBodyMap.put("user_prefix", URLEncoder.encode(this.USER_PREFIX, "UTF-8"));
		messageBodyMap.put("refresh_token", URLEncoder.encode(this.refreshToken, "UTF-8"));

		for (Map.Entry<String, String> param : messageBodyMap.entrySet()) {
			messageBody += param.getKey() + "=" + param.getValue() + "&";
		}

		command.setMessageBody(messageBody);
		command.setUrl(this.TOKEN_URL);
		command.addHeader("Content Type", "application/x-www-form-urlencoded; charset=UTF-8");
		HttpCommandMessage returnMessage = this.httpController.sendHttpCommand(command);

		// Parse result
		JSONObject json = new JSONObject(returnMessage.response);

		// Read tokens
		this.authToken = json.getString("access_token");
		this.refreshToken = json.getString("refresh_token");
	}

	private void updateThermostatDetails() throws Exception {
		HttpCommand command = new HttpCommand();
		Map<String, String> messageBodyMap = new HashMap<String, String>();

		messageBodyMap.put("app_version", URLEncoder.encode(this.APP_VERSION, "UTF-8"));
		messageBodyMap.put("sync_device_id", URLEncoder.encode("all", "UTF-8"));
		messageBodyMap.put("device_type", URLEncoder.encode("NAVaillant", "UTF-8"));
		messageBodyMap.put("access_token", URLEncoder.encode(this.authToken, "UTF-8"));

		String messageBody = "";
		for (Map.Entry<String, String> param : messageBodyMap.entrySet()) {
			messageBody += param.getKey() + "=" + param.getValue() + "&";
		}

		command.setHttpMethod("POST");
		command.setMessageType("application/x-www-form-urlencoded; charset=UTF-8");
		command.setUrl(this.GET_THERMOSTAT_DATA);
		command.setMessageBody(messageBody);

		// Parse result

		// Measures
		Object apiResponse = this.requestApi(command);
		try {
			Double outdoorTemperature = Double.valueOf("" + JsonPath.read(apiResponse, "$.body.devices[0].outdoor_temperature.te"));
			String formattedValue = String.format("%.2f", outdoorTemperature).replace(",", ".");
			this.updateTemperatureValue(this.OUTDOOR_TEMPERATURE, outdoorTemperature, formattedValue);

		} catch (Exception e) {
			this.log.error("Exception occured: ", e);
		}

		try {
			Double indoorTemperature = Double.valueOf("" + JsonPath.read(apiResponse, "$.body.devices[0].modules[0].measured.temperature"));
			String formattedValue = String.format("%.2f", indoorTemperature).replace(",", ".");
			this.updateTemperatureValue(this.INDOOR_TEMPERATURE, indoorTemperature, formattedValue);
		} catch (Exception e) {
			this.log.error("Exception occured: ", e);
		}

		try {
			Double setpointTemperature = Double.valueOf("" + JsonPath.read(apiResponse, "$.body.devices[0].modules[0].measured.setpoint_temp"));
			String formattedValue = String.format("%.1f", setpointTemperature).replace(",", ".");
			this.updateTemperatureValue(this.SETPOINT_TEMPERATURE, setpointTemperature, formattedValue);
		} catch (Exception e) {
			this.log.error("Exception occured: ", e);
		}

		try {
			this.deviceId = JsonPath.read(apiResponse, "$.body.devices[0]._id");
			this.moduleId = JsonPath.read(apiResponse, "$.body.devices[0].modules[0]._id");
		} catch (Exception e) {
		}

		// States
		try {
			Boolean awayActive = Boolean.valueOf("" + JsonPath.read(apiResponse, "$.body.devices[0].modules[0].setpoint_away.setpoint_activate"));
			this.updateBooleanValue(this.AWAY_ACTIVE, awayActive);
			if (awayActive) {
				Long awayActiveUntil = Long.valueOf("" + JsonPath.read(apiResponse, "$.body.devices[0].modules[0].setpoint_away.setpoint_endtime"));
				if (awayActiveUntil == 0) {
					this.updateAwayTxt(awayActive, null);
				} else {
					this.updateAwayTxt(awayActive, new Date(awayActiveUntil * 1000L));
				}
			} else {
				this.updateAwayTxt(awayActive, null);
			}
		} catch (Exception e) {
			this.log.error("Exception occured: ", e);
		}
		try {
			Boolean manualOverride = Boolean.valueOf("" + JsonPath.read(apiResponse, "$.body.devices[0].modules[0].setpoint_manual.setpoint_activate"));
			this.updateBooleanValue(this.MANUAL_OVERRIDE_ACTIVE, manualOverride);
		} catch (Exception e) {
			this.log.error("Exception occured: ", e);
		}

	}

	private void updateAwayTxt(Boolean away, Date until) {
		if (!away) {
			this.updateTextValue(this.AWAY_ACTIVE_UNTIL_TXT, "At home");
		} else {
			if (until == null) {
				this.updateTextValue(this.AWAY_ACTIVE_UNTIL_TXT, "Away until changed");
			} else {
				this.updateTextValue(this.AWAY_ACTIVE_UNTIL_TXT, "Away until " + new SimpleDateFormat("dd/MM HH:mm").format(until));
			}
		}
	}

	/*
	 * Set away without parameters will send a mobile notification to confirm
	 * until when the away mode has to remain active It will default the away
	 * time on X hours if no mobile action is taken.
	 */
	private boolean setAway(boolean on) throws Exception {
		Integer hoursAway;

		try {
			hoursAway = Integer.parseInt(SettingsManager.getStringValue(this.getIdentifier(), Settings.HOURS_AWAY.get()));
		}catch (Exception e) {
			hoursAway = 0;
		}

		boolean returnBoolean;
		Calendar endTime = null;
		if (hoursAway > 0) {
			endTime = Calendar.getInstance();
			endTime.setTime(new Date());
			endTime.add(Calendar.HOUR_OF_DAY, hoursAway);
			returnBoolean = this.setAway(on, endTime.getTime());
		} else {
			returnBoolean = this.setAway(on, null);
		}

		if (returnBoolean && on) {
			ClientNotificationMessage notificationMessage = new ClientNotificationMessage();
			notificationMessage.controlIdentifiers = this.values.get(this.AWAY_ACTIVE).controlIdentifiers;
			notificationMessage.notificationType = MobileNotificationTypes.DATE_TIME_PICKER;
			notificationMessage.subtitle = "When will you be back home?";
			notificationMessage.title = "Away mode activated";
			if (endTime != null) {
				notificationMessage.message = "Until " + new SimpleDateFormat("HH:mm, dd/MM").format(endTime.getTime()) + ". Click to change";
				notificationMessage.startDate = new Date().getTime();
			}
			PushNotificationService.getInstance().sendMessage(notificationMessage);
		} else if (returnBoolean && !on) {
			ClientNotificationMessage notificationMessage = new ClientNotificationMessage();
			notificationMessage.controlIdentifiers = this.values.get(this.AWAY_ACTIVE).controlIdentifiers;
			notificationMessage.notificationType = MobileNotificationTypes.DATE_TIME_PICKER;
			notificationMessage.cancel = true;
			PushNotificationService.getInstance().sendMessage(notificationMessage);
		}
		return returnBoolean;
	}

	private boolean setAway(boolean on, Date endTime) throws Exception {
		HttpCommand command = new HttpCommand();
		Map<String, String> messageBodyMap = new HashMap<String, String>();

		messageBodyMap.put("app_version", URLEncoder.encode(this.APP_VERSION, "UTF-8"));
		messageBodyMap.put("activate", URLEncoder.encode(on ? "true" : "false", "UTF-8"));
		messageBodyMap.put("access_token", URLEncoder.encode(this.authToken, "UTF-8"));
		messageBodyMap.put("setpoint_mode", URLEncoder.encode("away", "UTF-8"));
		messageBodyMap.put("module_id", URLEncoder.encode(this.moduleId, "UTF-8"));
		messageBodyMap.put("device_id", URLEncoder.encode(this.deviceId, "UTF-8"));
		if (endTime != null) {
			messageBodyMap.put("setpoint_endtime", endTime.getTime() / 1000L + "");
		}
		String messageBody = "";
		for (Map.Entry<String, String> param : messageBodyMap.entrySet()) {
			messageBody += param.getKey() + "=" + param.getValue() + "&";
		}

		command.setHttpMethod("POST");
		command.setMessageType("application/x-www-form-urlencoded; charset=UTF-8");
		command.setUrl(this.SET_MINOR_MODE);
		command.setMessageBody(messageBody);

		// Parse result
		Object apiResponse = this.requestApi(command);
		try {
			String status = JsonPath.read(apiResponse, "$.status");
			if (status.equals("ok")) {
				this.updateBooleanValue(this.AWAY_ACTIVE, on);
				this.updateAwayTxt(on, endTime);
				// Cancel pending notifications as an end time has been set
				if (!on) {
					ClientNotificationMessage notificationMessage = new ClientNotificationMessage();
					notificationMessage.controlIdentifiers = this.values.get(this.AWAY_ACTIVE).controlIdentifiers;
					notificationMessage.notificationType = MobileNotificationTypes.DATE_TIME_PICKER;
					notificationMessage.cancel = true;
					PushNotificationService.getInstance().sendMessage(notificationMessage);
				}
			}
		} catch (Exception e) {
		}

		return true;
	}

	private void updateTemperatureValue(String valueIdentifier, Double doubleValue, String formattedValue) {
		ValueChangedMessage valueChanged = this.values.get(valueIdentifier);
		boolean changed = false;
		if (valueChanged == null) {
			valueChanged = new ValueChangedMessage();
			valueChanged.broadcast = true;
			valueChanged.controlIdentifiers = new ControlIdentifiers(this.getIdentifier(), "Measurements", valueIdentifier);
			valueChanged.unit = "°C";
			valueChanged.value = formattedValue;
			valueChanged.valueType = ValueTypes.SENSOR_TEMPERATURE;
			this.values.put(valueIdentifier, valueChanged);
			changed = true;
		} else {
			if (!formattedValue.equals(valueChanged.value)) {
				changed = true;
				valueChanged.value = formattedValue;
			}
		}

		if (changed) {
			valueChanged.value = formattedValue;

			DatabaseConnector.getInstance().insertValueChange(valueChanged.controlIdentifiers, valueChanged.unit, valueChanged.value, doubleValue);

			Server.getInstance().broadcast(valueChanged);

			this.triggerValueChanged(valueChanged.controlIdentifiers);

			this.log.debug("Value change: " + valueIdentifier + ": " + formattedValue);
		}
	}

	private void updateTextValue(String valueIdentifier, String textValue) {

		ValueChangedMessage valueChanged = this.values.get(valueIdentifier);
		boolean changed = false;
		if (valueChanged == null) {
			valueChanged = new ValueChangedMessage();
			valueChanged.broadcast = true;
			valueChanged.controlIdentifiers = new ControlIdentifiers(this.getIdentifier(), "States", valueIdentifier);
			valueChanged.unit = "";
			valueChanged.value = textValue;
			valueChanged.valueType = ValueTypes.TEXT;
			this.values.put(valueIdentifier, valueChanged);

			changed = true;
		} else {
			if (!textValue.equals(valueChanged.value)) {
				valueChanged.value = textValue;
				changed = true;
			}
		}

		if (changed) {
			Server.getInstance().broadcast(valueChanged);
			this.log.debug("Value change: " + valueIdentifier + ": " + textValue);
		}
	}

	private void updateBooleanValue(String valueIdentifier, Boolean booleanValue) {

		ValueChangedMessage valueChanged = this.values.get(valueIdentifier);
		String stringValue = booleanValue ? "true" : "false";
		boolean changed = false;
		if (valueChanged == null) {
			valueChanged = new ValueChangedMessage();
			valueChanged.broadcast = true;
			valueChanged.controlIdentifiers = new ControlIdentifiers(this.getIdentifier(), "States", valueIdentifier);
			valueChanged.unit = "";
			valueChanged.value = booleanValue ? "true" : "false";
			valueChanged.valueType = ValueTypes.SWITCH_BINARY;
			this.values.put(valueIdentifier, valueChanged);

			changed = true;
		} else {
			if (!stringValue.equals(valueChanged.value)) {
				changed = true;
				valueChanged.value = stringValue;
			}
		}

		if (changed) {
			DatabaseConnector.getInstance().insertValueChange(valueChanged.controlIdentifiers, valueChanged.unit, valueChanged.value, booleanValue ? 1.0 : 0.0);

			Server.getInstance().broadcast(valueChanged);

			this.triggerValueChanged(valueChanged.controlIdentifiers);

			this.log.debug("Value change: " + valueIdentifier + ": " + stringValue);
		}
	}

	private boolean setManualSetpoint(Double setpointTemperature, Date endTime, boolean on) throws Exception {
		HttpCommand command = new HttpCommand();
		Map<String, String> messageBodyMap = new HashMap<String, String>();

		messageBodyMap.put("app_version", URLEncoder.encode(this.APP_VERSION, "UTF-8"));
		messageBodyMap.put("activate", URLEncoder.encode(on ? "true" : "false", "UTF-8"));
		messageBodyMap.put("access_token", URLEncoder.encode(this.authToken, "UTF-8"));
		messageBodyMap.put("setpoint_mode", URLEncoder.encode("manual", "UTF-8"));
		messageBodyMap.put("module_id", URLEncoder.encode(this.moduleId, "UTF-8"));
		messageBodyMap.put("device_id", URLEncoder.encode(this.deviceId, "UTF-8"));
		messageBodyMap.put("setpoint_temp", URLEncoder.encode(String.format(Locale.US, "%.2f", setpointTemperature), "UTF-8"));
		messageBodyMap.put("setpoint_endtime", endTime.getTime() / 1000L + "");

		String messageBody = "";
		for (Map.Entry<String, String> param : messageBodyMap.entrySet()) {
			messageBody += param.getKey() + "=" + param.getValue() + "&";
		}

		command.setHttpMethod("POST");
		command.setMessageType("application/x-www-form-urlencoded; charset=UTF-8");
		command.setUrl(this.SET_MINOR_MODE);
		command.setMessageBody(messageBody);

		// Parse result
		Object apiResponse = this.requestApi(command);
		try {
			String status = JsonPath.read(apiResponse, "$.status");
			if (status.equals("ok")) {
				this.updateTemperatureValue(this.SETPOINT_TEMPERATURE, setpointTemperature, String.format("%.1f", setpointTemperature).replace(",", "."));
				return true;
			} else {
				return false;
			}

		} catch (PathNotFoundException e) {
			this.log.error("Exception occured: ", e);
			return false;
		}
	}

	private Object requestApi(HttpCommand command) throws Exception {
		HttpCommandMessage returnMessage = this.httpController.sendHttpCommand(command);
		this.log.debug("API Request: " + command.getUrl() + ", body: " + command.getMessageBody());
		Object document = Configuration.defaultConfiguration().jsonProvider().parse(returnMessage.response);
		this.log.debug("API Response: " + returnMessage.response);
		try {
			Integer error = JsonPath.read(document, "$.error.code");
			// {error={code=3, message=Access token expired}}
			if (error == 2 || error == 3) {
				// Token needs to be refreshed
				// refreshToken();
				this.getLoginToken();
				return Configuration.defaultConfiguration().jsonProvider().parse(returnMessage.response);
			}
		} catch (PathNotFoundException e) {
		}
		return document;
	}

	@Override
	public JSONMessage parseNetMessage(JSONMessage message) {

		if (message instanceof ValueHistoryRequest) {
			// Let the general controller handle this one
			IController generalController = Server.getInstance().getControllers().get(ControllerTypes.GENERAL.convert());
			if (generalController != null) {
				return generalController.parseNetMessage(message);
			}
		} else if (message instanceof SetAwayMessage) {
			Boolean onOff = Boolean.parseBoolean(((SetAwayMessage) message).value);
			try {

				ClientMessageMessage returnMessage = new ClientMessageMessage();
				message.broadcast = true;
				message.controlIdentifiers = new ControlIdentifiers(ControllerTypes.GENERAL.convert());
				if (this.setAway(onOff, ((SetAwayMessage) message).until)) {
					returnMessage.messageContent = "Away mode activated until " + new SimpleDateFormat("dd/MM HH:mm").format(((SetAwayMessage) message).until);
					return returnMessage;
				} else {
					returnMessage.messageContent = "Failed to change away mode";
					return returnMessage;
				}
			} catch (Exception e) {
				this.log.error("Exception occured: ", e);
			}
		} else if (message instanceof SetValueMessage) {
			if (message.controlIdentifiers.getValueIdentifier().equals(this.SETPOINT_TEMPERATURE)) {
				try {
					Double value = Double.parseDouble(((SetValueMessage) message).value);
					Calendar c = Calendar.getInstance();
					c.setTime(new Date());
					c.add(Calendar.HOUR_OF_DAY, 3);

					ClientMessageMessage returnMessage = new ClientMessageMessage();
					message.broadcast = true;
					message.controlIdentifiers = new ControlIdentifiers(ControllerTypes.GENERAL.convert());
					if (this.setManualSetpoint(value, c.getTime(), true)) {
						returnMessage.messageContent = "Manual temperature set to " + value + "� until " + new SimpleDateFormat("HH:mm").format(c.getTime());
						return returnMessage;
					} else {
						returnMessage.messageContent = "Failed to change temperature";
						return returnMessage;
					}
				} catch (NumberFormatException e) {
					this.log.error("Exception occured: ", e);
				} catch (Exception e) {
					this.log.error("Exception occured: ", e);
				}
			} else if (message.controlIdentifiers.getValueIdentifier().equals(this.AWAY_ACTIVE)) {
				Boolean value = Boolean.parseBoolean(((SetValueMessage) message).value);
				try {
					this.setAway(value);
				} catch (Exception e) {
					this.log.error("Exception occured: ", e);
				}
			} else if (message.controlIdentifiers.getValueIdentifier().equals(this.MANUAL_OVERRIDE_ACTIVE)) {
				Boolean value = Boolean.parseBoolean(((SetValueMessage) message).value);
				try {
					if (value == false) {
						this.setManualSetpoint(5.0, new Date(), false);
					}
				} catch (Exception e) {
					this.log.error("Exception occured: ", e);
				}
			}
		}

		return null;
	}

	@Override
	public String getIdentifier() {
		return ControllerTypes.THERMOSTAT.convert();
	}

	@Override
	public void init() {
		super.init();

		this.username = SettingsManager.getStringValue(this.getIdentifier(), Settings.THERMOSTAT_USERNAME.get());
		this.password = SettingsManager.getStringValue(this.getIdentifier(), Settings.THERMOSTAT_PASSWORD.get());

		if (this.username == null || this.username.equals("") || this.password == null || this.password.equals("")) {
			this.log.info("Could not find VSMart settings. Disabling VSMart.");
			this.enabled = false;
		} else {
			this.enabled = true;
			if (this.httpController == null) {
				this.httpController = HttpCommandController.getInstance();
			}

			// Login
			try {
				this.getLoginToken();
				this.updateThermostatDetails();
			} catch (Exception e) {
				e.getStackTrace();
				this.log.error("Could not start : " + e.getMessage());
			}

			// Poll for updates each 5 minutes
			Scheduler.getInstance().scheduleCron(new TimerTask() {
				@Override
				public void run() {
					try {
						VaillantVSMartController.this.updateThermostatDetails();
					} catch (Exception e) {

					}
				}
			}, "0,5,10,15,20,25,30,35,40,45,50,55 * * * *");

			// Initialize node structure
			this.getNodes();

			this.log.info("Initialized");
		}
	}

	@Override
	public List<JSONMessage> initClient() {
		return new ArrayList<JSONMessage>(this.values.values());
	}

	@Override
	public boolean isEnabled() {
		return this.enabled;
	}

	@Override
	public String getName() {
		return "Vaillant VSmart Thermostat";
	}

	@Override
	public Collection<ControllerNode> getNodes() {

		if (this.controllerMap.size() == 0) {
			ControllerNode measurementsNode = new ControllerNode(this, "Measurements", "Measurements", "");
			measurementsNode.addValue(new ControllerValue(this.SETPOINT_TEMPERATURE, "Setpoint Temperature", ValueTypes.HEATING));
			measurementsNode.addValue(new ControllerValue(this.INDOOR_TEMPERATURE, "Indoor Temperature", ValueTypes.SENSOR_TEMPERATURE));
			measurementsNode.addValue(new ControllerValue(this.OUTDOOR_TEMPERATURE, "Outdoor Temperature", ValueTypes.SENSOR_TEMPERATURE));
			this.controllerMap.put(measurementsNode.getIdentifier(), measurementsNode);

			ControllerNode statesNode = new ControllerNode(this, "States", "States", "");
			statesNode.addValue(new ControllerValue(this.MANUAL_OVERRIDE_ACTIVE, "Manual Override Active", ValueTypes.SWITCH_BINARY));
			statesNode.addValue(new ControllerValue(this.AWAY_ACTIVE, "Away schema active", ValueTypes.SWITCH_BINARY));
			statesNode.addValue(new ControllerValue(this.AWAY_ACTIVE_UNTIL_TXT, "Away until text", ValueTypes.TEXT));
			this.controllerMap.put(statesNode.getIdentifier(), statesNode);
		}

		return this.controllerMap.values();
	}

	@Override
	public String getValueName(ControlIdentifiers valueIdentifier) {
		ControllerNode node = this.controllerMap.get(valueIdentifier.getControllerIdentifier());
		if (node != null) {
			ControllerValue value = node.getValue(valueIdentifier.getValueIdentifier());
			if (value != null) {
				return value.getName();
			}
		}
		return "Unknown";
	}

	@Override
	public Collection<ControllerNode> getTriggers() {
		return this.getNodes();
	}

	@Override
	public String getValue(ControlIdentifiers valueIdentifiers) {
		return this.values.get(valueIdentifiers.getValueIdentifier()).value;
	}

	@Override
	public boolean isInitialized() {
		return this.values.size() >= 5;
	}

	@Override
	public List<Setting> getSettings() {
		List<Setting> returnList = new ArrayList<Setting>();
		for (Settings s : Settings.values()) {
			returnList.add(s.setting);
		}
		return returnList;
	}

}
