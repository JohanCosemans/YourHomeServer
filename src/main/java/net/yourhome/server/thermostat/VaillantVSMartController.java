package net.yourhome.server.thermostat;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import net.yourhome.common.base.enums.ControllerTypes;
import net.yourhome.common.base.enums.MobileNotificationTypes;
import net.yourhome.common.base.enums.ValueTypes;
import net.yourhome.common.net.messagestructures.JSONMessage;
import net.yourhome.common.net.messagestructures.general.ClientMessageMessage;
import net.yourhome.common.net.messagestructures.general.ClientNotificationMessage;
import net.yourhome.common.net.messagestructures.general.SetValueMessage;
import net.yourhome.common.net.messagestructures.general.ValueChangedMessage;
import net.yourhome.common.net.messagestructures.general.ValueHistoryRequest;
import net.yourhome.common.net.messagestructures.http.HttpCommand;
import net.yourhome.common.net.messagestructures.http.HttpCommandMessage;
import net.yourhome.common.net.messagestructures.thermostat.SetAwayMessage;
import net.yourhome.common.net.model.binding.ControlIdentifiers;
import net.yourhome.server.AbstractController;
import net.yourhome.server.ControllerNode;
import net.yourhome.server.ControllerValue;
import net.yourhome.server.IController;
import net.yourhome.server.base.BuildConfig;
import net.yourhome.server.base.DatabaseConnector;
import net.yourhome.server.base.Scheduler;
import net.yourhome.server.base.Setting;
import net.yourhome.server.base.SettingsManager;
import net.yourhome.server.base.rules.scenes.actions.notifications.GoogleCloudMessagingService;
import net.yourhome.server.http.HttpCommandController;
import net.yourhome.server.net.Server;

public class VaillantVSMartController extends AbstractController {
	public enum Settings {

		THERMOSTAT_USERNAME(new Setting("THERMOSTAT_USERNAME", "VSMart Thermostat Username", "my@email.address")), 
		THERMOSTAT_PASSWORD(new Setting("THERMOSTAT_PASSWORD", "VSMart Thermostat Password"));

		private Setting setting;

		private Settings(Setting setting) {
			this.setting = setting;
		}

		public Setting get() {
			return setting;
		}
	}

	private HttpCommandController httpController;
	private boolean enabled;

	// Thermostat API
	private final String BASE_URL = "https://app.netatmo.net";
	private final String TOKEN_URL = BASE_URL + "/oauth2/token";
	private final String GET_THERMOSTAT_DATA = BASE_URL + "/api/getthermostatsdata";
	private final String SET_MINOR_MODE = BASE_URL + "/api/setminormode";

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

	private Map<String,ControllerNode> controllerMap = new LinkedHashMap<String,ControllerNode>();
	private Map<String, ValueChangedMessage> values = new HashMap<String, ValueChangedMessage>();

	private static volatile VaillantVSMartController instance;
	private static Object lock = new Object();
	private VaillantVSMartController() {
		log = Logger.getLogger("net.yourhome.server.thermostat.Thermostat-Vaillant");
	}
	public static VaillantVSMartController getInstance() {
		VaillantVSMartController r = instance;
		if (r == null) {
			synchronized (lock) { // while we were waiting for the lock, another
				r = instance; // thread may have instantiated
								// the object
				if (r == null) {
					r = new VaillantVSMartController();
					instance = r;
				}
			}
		}
		return instance;
	}

	private boolean getLoginToken() throws Exception {

		HttpCommand command = new HttpCommand();
		command.setHttpMethod("POST");

		String messageBody = "";
		Map<String, String> messageBodyMap = new HashMap<String, String>();
		messageBodyMap.put("client_id", URLEncoder.encode(CLIENT_ID, "UTF-8"));
		messageBodyMap.put("client_secret", URLEncoder.encode(BuildConfig.VAILLANT_CLIENT_SECRET, "UTF-8"));
		messageBodyMap.put("app_version", URLEncoder.encode(APP_VERSION, "UTF-8"));
		messageBodyMap.put("username", URLEncoder.encode(username, "UTF-8"));
		messageBodyMap.put("password", URLEncoder.encode(password, "UTF-8"));
		messageBodyMap.put("grant_type", URLEncoder.encode("password", "UTF-8"));
		messageBodyMap.put("user_prefix", URLEncoder.encode(USER_PREFIX, "UTF-8"));

		for (Map.Entry<String, String> param : messageBodyMap.entrySet()) {
			messageBody += param.getKey() + "=" + param.getValue() + "&";
		}

		command.setMessageBody(messageBody);
		command.setUrl(TOKEN_URL);
		command.setMessageType("application/x-www-form-urlencoded; charset=UTF-8");
		HttpCommandMessage returnMessage = httpController.sendHttpCommand(command);

		// Parse result
		JSONObject json = new JSONObject(returnMessage.response);

		// Read tokens
		this.authToken = json.getString("access_token");
		this.refreshToken = json.getString("refresh_token");

		if (authToken != null) {
			return true;
		}
		return false;
	}

	private void refreshToken() throws Exception {

		HttpCommand command = new HttpCommand();
		command.setHttpMethod("POST");

		String messageBody = "";
		Map<String, String> messageBodyMap = new HashMap<String, String>();
		messageBodyMap.put("client_id", URLEncoder.encode(CLIENT_ID, "UTF-8"));
		messageBodyMap.put("client_secret", URLEncoder.encode(BuildConfig.VAILLANT_CLIENT_SECRET, "UTF-8"));
		messageBodyMap.put("app_version", URLEncoder.encode(APP_VERSION, "UTF-8"));
		messageBodyMap.put("grant_type", URLEncoder.encode("refresh_token", "UTF-8"));
		messageBodyMap.put("user_prefix", URLEncoder.encode(USER_PREFIX, "UTF-8"));
		messageBodyMap.put("refresh_token", URLEncoder.encode(this.refreshToken, "UTF-8"));

		for (Map.Entry<String, String> param : messageBodyMap.entrySet()) {
			messageBody += param.getKey() + "=" + param.getValue() + "&";
		}

		command.setMessageBody(messageBody);
		command.setUrl(TOKEN_URL);
		command.addHeader("Content Type", "application/x-www-form-urlencoded; charset=UTF-8");
		HttpCommandMessage returnMessage = httpController.sendHttpCommand(command);

		// Parse result
		JSONObject json = new JSONObject(returnMessage.response);

		// Read tokens
		this.authToken = json.getString("access_token");
		this.refreshToken = json.getString("refresh_token");
	}

	private void updateThermostatDetails() throws Exception {
		HttpCommand command = new HttpCommand();
		Map<String, String> messageBodyMap = new HashMap<String, String>();

		messageBodyMap.put("app_version", URLEncoder.encode(APP_VERSION, "UTF-8"));
		messageBodyMap.put("sync_device_id", URLEncoder.encode("all", "UTF-8"));
		messageBodyMap.put("device_type", URLEncoder.encode("NAVaillant", "UTF-8"));
		messageBodyMap.put("access_token", URLEncoder.encode(authToken, "UTF-8"));

		String messageBody = "";
		for (Map.Entry<String, String> param : messageBodyMap.entrySet()) {
			messageBody += param.getKey() + "=" + param.getValue() + "&";
		}

		command.setHttpMethod("POST");
		command.setMessageType("application/x-www-form-urlencoded; charset=UTF-8");
		command.setUrl(GET_THERMOSTAT_DATA);
		command.setMessageBody(messageBody);

		// Parse result

		// Measures
		Object apiResponse = requestApi(command);
		try {
			Double outdoorTemperature = Double.valueOf("" + JsonPath.read(apiResponse, "$.body.devices[0].outdoor_temperature.te"));
			String formattedValue = String.format("%.2f", outdoorTemperature).replace(",", ".");
			updateTemperatureValue(OUTDOOR_TEMPERATURE, outdoorTemperature, formattedValue);

		} catch (Exception e) {
			log.error("Exception occured: ", e);
		}

		try {
			Double indoorTemperature = Double.valueOf("" + JsonPath.read(apiResponse, "$.body.devices[0].modules[0].measured.temperature"));
			String formattedValue = String.format("%.2f", indoorTemperature).replace(",", ".");
			updateTemperatureValue(INDOOR_TEMPERATURE, indoorTemperature, formattedValue);
		} catch (Exception e) {
			log.error("Exception occured: ", e);
		}

		try {
			Double setpointTemperature = Double.valueOf("" + JsonPath.read(apiResponse, "$.body.devices[0].modules[0].measured.setpoint_temp"));
			String formattedValue = String.format("%.1f", setpointTemperature).replace(",", ".");
			updateTemperatureValue(SETPOINT_TEMPERATURE, setpointTemperature, formattedValue);
		} catch (Exception e) {
			log.error("Exception occured: ", e);
		}

		try {
			deviceId = JsonPath.read(apiResponse, "$.body.devices[0]._id");
			moduleId = JsonPath.read(apiResponse, "$.body.devices[0].modules[0]._id");
		} catch (Exception e) {
		}

		// States
		try {
			Boolean awayActive = Boolean.valueOf("" + JsonPath.read(apiResponse, "$.body.devices[0].modules[0].setpoint_away.setpoint_activate"));
			updateBooleanValue(AWAY_ACTIVE, awayActive);
			if (awayActive) {
				Long awayActiveUntil = Long.valueOf("" + JsonPath.read(apiResponse, "$.body.devices[0].modules[0].setpoint_away.setpoint_endtime"));
				if (awayActiveUntil == 0) {
					updateAwayTxt(awayActive, null);
				} else {
					updateAwayTxt(awayActive, new Date(awayActiveUntil * 1000L));
				}
			} else {
				updateAwayTxt(awayActive, null);
			}
		} catch (Exception e) {
			log.error("Exception occured: ", e);
		}
		try {
			Boolean manualOverride = Boolean.valueOf("" + JsonPath.read(apiResponse, "$.body.devices[0].modules[0].setpoint_manual.setpoint_activate"));
			updateBooleanValue(MANUAL_OVERRIDE_ACTIVE, manualOverride);
		} catch (Exception e) {
			log.error("Exception occured: ", e);
		}

	}

	private void updateAwayTxt(Boolean away, Date until) {
		if (!away) {
			updateTextValue(AWAY_ACTIVE_UNTIL_TXT, "At home");
		} else {
			if (until == null) {
				updateTextValue(AWAY_ACTIVE_UNTIL_TXT, "Away until changed");
			} else {
				updateTextValue(AWAY_ACTIVE_UNTIL_TXT, "Away until " + new SimpleDateFormat("dd/MM HH:mm").format(until));
			}
		}
	}

	/*
	 * Set away without parameters will send a mobile notification to confirm
	 * until when the away mode has to remain active It will default the away
	 * time on 3 hours if no mobile action is taken.
	 */
	private boolean setAway(boolean on) throws Exception {

		Calendar endTime = Calendar.getInstance();
		endTime.setTime(new Date());
		endTime.add(Calendar.HOUR_OF_DAY, 3);

		boolean returnBoolean = setAway(on, endTime.getTime());

		if (returnBoolean && on) {
			ClientNotificationMessage notificationMessage = new ClientNotificationMessage();
			notificationMessage.controlIdentifiers = this.values.get(AWAY_ACTIVE).controlIdentifiers;
			notificationMessage.notificationType = MobileNotificationTypes.DATE_TIME_PICKER;
			notificationMessage.subtitle = "When will you be back home?";
			notificationMessage.title = "Away mode activated";
			notificationMessage.message = "Until " + new SimpleDateFormat("HH:mm, dd/MM").format(endTime.getTime()) + ". Click to change";
			notificationMessage.startDate = endTime.getTime().getTime();
			GoogleCloudMessagingService.getInstance().sendMessage(notificationMessage);
		}else if(returnBoolean && !on) {
			ClientNotificationMessage notificationMessage = new ClientNotificationMessage();
			notificationMessage.controlIdentifiers = this.values.get(AWAY_ACTIVE).controlIdentifiers;
			notificationMessage.notificationType = MobileNotificationTypes.DATE_TIME_PICKER;
			notificationMessage.cancel = true;
			GoogleCloudMessagingService.getInstance().sendMessage(notificationMessage);
		}
		return returnBoolean;
	}

	private boolean setAway(boolean on, Date endTime) throws Exception {
		HttpCommand command = new HttpCommand();
		Map<String, String> messageBodyMap = new HashMap<String, String>();

		messageBodyMap.put("app_version", URLEncoder.encode(APP_VERSION, "UTF-8"));
		messageBodyMap.put("activate", URLEncoder.encode(on ? "true" : "false", "UTF-8"));
		messageBodyMap.put("access_token", URLEncoder.encode(authToken, "UTF-8"));
		messageBodyMap.put("setpoint_mode", URLEncoder.encode("away", "UTF-8"));
		messageBodyMap.put("module_id", URLEncoder.encode(moduleId, "UTF-8"));
		messageBodyMap.put("device_id", URLEncoder.encode(deviceId, "UTF-8"));
		messageBodyMap.put("setpoint_endtime", endTime.getTime() / 1000L + "");

		String messageBody = "";
		for (Map.Entry<String, String> param : messageBodyMap.entrySet()) {
			messageBody += param.getKey() + "=" + param.getValue() + "&";
		}

		command.setHttpMethod("POST");
		command.setMessageType("application/x-www-form-urlencoded; charset=UTF-8");
		command.setUrl(SET_MINOR_MODE);
		command.setMessageBody(messageBody);

		// Parse result
		Object apiResponse = requestApi(command);
		try {
			String status = JsonPath.read(apiResponse, "$.status");
			if (status.equals("ok")) {
				updateBooleanValue(AWAY_ACTIVE, on);
				updateAwayTxt(on, endTime);
				// Cancel pending notifications as an end time has been set
				if(!on) {
					ClientNotificationMessage notificationMessage = new ClientNotificationMessage();
					notificationMessage.controlIdentifiers = this.values.get(AWAY_ACTIVE).controlIdentifiers;
					notificationMessage.notificationType = MobileNotificationTypes.DATE_TIME_PICKER;
					notificationMessage.cancel = true;
					GoogleCloudMessagingService.getInstance().sendMessage(notificationMessage);
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
			valueChanged.controlIdentifiers = new ControlIdentifiers(getIdentifier(), "Measurements", valueIdentifier);
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

			log.debug("Value change: " + valueIdentifier + ": " + formattedValue);
		}
	}

	private void updateTextValue(String valueIdentifier, String textValue) {

		ValueChangedMessage valueChanged = this.values.get(valueIdentifier);
		boolean changed = false;
		if (valueChanged == null) {
			valueChanged = new ValueChangedMessage();
			valueChanged.broadcast = true;
			valueChanged.controlIdentifiers = new ControlIdentifiers(getIdentifier(), "States", valueIdentifier);
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
			log.debug("Value change: " + valueIdentifier + ": " + textValue);
		}
	}

	private void updateBooleanValue(String valueIdentifier, Boolean booleanValue) {

		ValueChangedMessage valueChanged = this.values.get(valueIdentifier);
		String stringValue = booleanValue ? "true" : "false";
		boolean changed = false;
		if (valueChanged == null) {
			valueChanged = new ValueChangedMessage();
			valueChanged.broadcast = true;
			valueChanged.controlIdentifiers = new ControlIdentifiers(getIdentifier(), "States", valueIdentifier);
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

			log.debug("Value change: " + valueIdentifier + ": " + stringValue);
		}
	}

	private boolean setManualSetpoint(Double setpointTemperature, Date endTime, boolean on) throws Exception {
		HttpCommand command = new HttpCommand();
		Map<String, String> messageBodyMap = new HashMap<String, String>();

		messageBodyMap.put("app_version", URLEncoder.encode(APP_VERSION, "UTF-8"));
		messageBodyMap.put("activate", URLEncoder.encode(on ? "true" : "false", "UTF-8"));
		messageBodyMap.put("access_token", URLEncoder.encode(authToken, "UTF-8"));
		messageBodyMap.put("setpoint_mode", URLEncoder.encode("manual", "UTF-8"));
		messageBodyMap.put("module_id", URLEncoder.encode(moduleId, "UTF-8"));
		messageBodyMap.put("device_id", URLEncoder.encode(deviceId, "UTF-8"));
		messageBodyMap.put("setpoint_temp", URLEncoder.encode(String.format(Locale.US, "%.2f", setpointTemperature), "UTF-8"));
		messageBodyMap.put("setpoint_endtime", endTime.getTime() / 1000L + "");

		String messageBody = "";
		for (Map.Entry<String, String> param : messageBodyMap.entrySet()) {
			messageBody += param.getKey() + "=" + param.getValue() + "&";
		}

		command.setHttpMethod("POST");
		command.setMessageType("application/x-www-form-urlencoded; charset=UTF-8");
		command.setUrl(SET_MINOR_MODE);
		command.setMessageBody(messageBody);

		// Parse result
		Object apiResponse = requestApi(command);
		try {
			String status = JsonPath.read(apiResponse, "$.status");
			if (status.equals("ok")) {
				updateTemperatureValue(SETPOINT_TEMPERATURE, setpointTemperature, String.format("%.1f", setpointTemperature).replace(",", "."));
				return true;
			} else {
				return false;
			}

		} catch (PathNotFoundException e) {
			log.error("Exception occured: ", e);
			return false;
		}
	}

	private Object requestApi(HttpCommand command) throws Exception {
		HttpCommandMessage returnMessage = httpController.sendHttpCommand(command);
		log.debug("API Request: " + command.getUrl() + ", body: " + command.getMessageBody());
		Object document = Configuration.defaultConfiguration().jsonProvider().parse(returnMessage.response);
		log.debug("API Response: " + returnMessage.response);
		try {
			Integer error = JsonPath.read(document, "$.error.code");
			// {error={code=3, message=Access token expired}}
			if (error == 2 || error == 3) {
				// Token needs to be refreshed
				// refreshToken();
				getLoginToken();
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
				if (setAway(onOff, ((SetAwayMessage) message).until)) {
					returnMessage.messageContent = "Away mode activated until " + new SimpleDateFormat("dd/MM HH:mm").format(((SetAwayMessage) message).until);
					return returnMessage;
				} else {
					returnMessage.messageContent = "Failed to change away mode";
					return returnMessage;
				}
			} catch (Exception e) {
				log.error("Exception occured: ", e);
			}
		} else if (message instanceof SetValueMessage) {
			if (message.controlIdentifiers.getValueIdentifier().equals(SETPOINT_TEMPERATURE)) {
				try {
					Double value = Double.parseDouble(((SetValueMessage) message).value);
					Calendar c = Calendar.getInstance();
					c.setTime(new Date());
					c.add(Calendar.HOUR_OF_DAY, 3);

					ClientMessageMessage returnMessage = new ClientMessageMessage();
					message.broadcast = true;
					message.controlIdentifiers = new ControlIdentifiers(ControllerTypes.GENERAL.convert());
					if (this.setManualSetpoint(value, c.getTime(), true)) {
						returnMessage.messageContent = "Manual temperature set to " + value + "° until " + new SimpleDateFormat("HH:mm").format(c.getTime());
						return returnMessage;
					} else {
						returnMessage.messageContent = "Failed to change temperature";
						return returnMessage;
					}
				} catch (NumberFormatException e) {
					log.error("Exception occured: ", e);
				} catch (Exception e) {
					log.error("Exception occured: ", e);
				}
			} else if (message.controlIdentifiers.getValueIdentifier().equals(AWAY_ACTIVE)) {
				Boolean value = Boolean.parseBoolean(((SetValueMessage) message).value);
				try {
					setAway(value);
				} catch (Exception e) {
					log.error("Exception occured: ", e);
				}
			} else if (message.controlIdentifiers.getValueIdentifier().equals(MANUAL_OVERRIDE_ACTIVE)) {
				Boolean value = Boolean.parseBoolean(((SetValueMessage) message).value);
				try {
					if (value == false) {
						this.setManualSetpoint(5.0, new Date(), false);
					}
				} catch (Exception e) {
					log.error("Exception occured: ", e);
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
		
		username = SettingsManager.getStringValue(getIdentifier(), Settings.THERMOSTAT_USERNAME.get());
		password = SettingsManager.getStringValue(getIdentifier(), Settings.THERMOSTAT_PASSWORD.get());
		
		if (username == null || username.equals("") || password == null || password.equals("")) {
			log.info("Could not find VSMart settings. Disabling VSMart.");
			enabled = false;
		}else {
			enabled = true;
			if (this.httpController == null) {
				this.httpController = HttpCommandController.getInstance();
			}

			// Login
			try {
				getLoginToken();
				updateThermostatDetails();
			} catch (Exception e) {
				e.getStackTrace();
				log.error("Could not start : " + e.getMessage());
			}
			
			// Poll for updates each 5 minutes
			Scheduler.getInstance().scheduleCron(new TimerTask() {
				@Override
				public void run() {
					try {
						updateThermostatDetails();
					} catch (Exception e) {

					}
				}
			}, "0,5,10,15,20,25,30,35,40,45,50,55 * * * *");
			
			// Initialize node structure
			getNodes();
			
			log.info("Initialized");
		}
	}

	@Override
	public List<JSONMessage> initClient() {
		return new ArrayList<JSONMessage>(this.values.values());
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public String getName() {
		return "Vaillant VSmart Thermostat";
	}
	

	@Override
	public Collection<ControllerNode> getNodes() {

		if(controllerMap.size()==0) {
			ControllerNode measurementsNode = new ControllerNode(this, "Measurements", "Measurements", "");
			measurementsNode.addValue(new ControllerValue(SETPOINT_TEMPERATURE, "Setpoint Temperature", ValueTypes.HEATING));
			measurementsNode.addValue(new ControllerValue(INDOOR_TEMPERATURE, "Indoor Temperature", ValueTypes.SENSOR_TEMPERATURE));
			measurementsNode.addValue(new ControllerValue(OUTDOOR_TEMPERATURE, "Outdoor Temperature", ValueTypes.SENSOR_TEMPERATURE));
			controllerMap.put(measurementsNode.getIdentifier(),measurementsNode);
	
			ControllerNode statesNode = new ControllerNode(this, "States", "States", "");
			statesNode.addValue(new ControllerValue(MANUAL_OVERRIDE_ACTIVE, "Manual Override Active", ValueTypes.SWITCH_BINARY));
			statesNode.addValue(new ControllerValue(AWAY_ACTIVE, "Away schema active", ValueTypes.SWITCH_BINARY));
			statesNode.addValue(new ControllerValue(AWAY_ACTIVE_UNTIL_TXT, "Away until text", ValueTypes.TEXT));
			controllerMap.put(statesNode.getIdentifier(),statesNode);
		}

		return controllerMap.values();
	}

	@Override
	public String getValueName(ControlIdentifiers valueIdentifier) {
		ControllerNode node = controllerMap.get(valueIdentifier.getControllerIdentifier());
		if(node != null) {
			ControllerValue value = node.getValue(valueIdentifier.getValueIdentifier());
			if(value != null) {
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
