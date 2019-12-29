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
package net.yourhome.server.ikea;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import net.yourhome.common.base.enums.ControllerTypes;
import net.yourhome.common.base.enums.EnumConverter;
import net.yourhome.common.base.enums.ReverseEnumMap;
import net.yourhome.common.base.enums.ValueTypes;
import net.yourhome.common.net.messagestructures.JSONMessage;
import net.yourhome.common.net.messagestructures.general.SetValueMessage;
import net.yourhome.common.net.messagestructures.general.ValueChangedMessage;
import net.yourhome.common.net.messagestructures.general.ValueHistoryRequest;
import net.yourhome.common.net.messagestructures.http.HttpCommand;
import net.yourhome.common.net.messagestructures.http.HttpCommandMessage;
import net.yourhome.common.net.model.binding.ControlIdentifiers;
import net.yourhome.server.AbstractController;
import net.yourhome.server.ControllerNode;
import net.yourhome.server.ControllerValue;
import net.yourhome.server.IController;
import net.yourhome.server.base.Setting;
import net.yourhome.server.base.SettingsManager;
import net.yourhome.server.base.Util;
import net.yourhome.server.http.HttpCommandController;
import net.yourhome.server.net.Server;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.*;

public class IkeaNetController  {

	private final String LOGIN_URL = "/login";
	private final String GROUPS_URL = "/groups";
	private final String DEVICES_URL = "/devices";

	private String hubSecurityToken;
	private String hubIp;
	private String hubRestProtocol = "http";
	private String hubRestIp = "localhost";
	private String hubRestPort = "2080";

	private ObjectMapper objectMapper;
	private Map<Integer, Device> devices = new HashMap<>();

	public IkeaNetController(String hubSecurityToken, String hubIp, String hubRestProtocol, String hubRestIp, String hubRestPort) {
		this.hubSecurityToken = hubSecurityToken;
		this.hubIp = hubIp;
		this.hubRestProtocol = hubRestProtocol;
		this.hubRestIp = hubRestIp;
		this.hubRestPort = hubRestPort;

		objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		init();
	}

	public boolean incomplete() {
		return this.hubIp == null || this.hubSecurityToken.equals("") || this.hubRestIp == null || this.hubRestProtocol.equals("") || this.hubRestPort.equals("");
	}

	private String authToken;

	private HttpCommandController httpController = HttpCommandController.getInstance();

	protected static Logger log  = Logger.getLogger(IkeaNetController.class);

	private boolean getLoginToken() throws Exception {

		Map<String, String> messageBodyMap = new HashMap<String, String>();
		messageBodyMap.put("host", URLEncoder.encode(this.hubIp, "UTF-8"));
		messageBodyMap.put("code", URLEncoder.encode(this.hubSecurityToken, "UTF-8"));

		// Parse result
		JSONObject json = new JSONObject(requestApi("POST", LOGIN_URL, messageBodyMap));

		// Read tokens
		this.authToken = json.getString("token");

		return this.authToken != null;
	}

	private String requestApi(String method, String path ) throws Exception {
		return requestApi(method,path, null);
	}

	private String requestApi(String method, String path,Map<String, String> messageBodyMap  ) throws Exception {

		HttpCommand command = new HttpCommand();
		command.setUrl(this.hubRestProtocol+"://"+this.hubRestIp+":"+this.hubRestPort+path);
		command.setHttpMethod(method);
		command.setMessageType("application/json; charset=UTF-8");
		command.setMessageBody(messageBodyMap==null?null:Util.toString(messageBodyMap));
		command.addHeader("authorization", "Bearer "+this.authToken);
		log.debug("API Request: " + command.getUrl() + ", body: " + command.getMessageBody());
		HttpCommandMessage returnMessage;
		try {
			returnMessage = this.httpController.sendHttpCommand(command);
			if(returnMessage.responseCode > 400) {
				getLoginToken();
				returnMessage = this.httpController.sendHttpCommand(command);
			}
			log.debug("API Response: " + returnMessage.response);
			return returnMessage.response;
		}catch (Exception e) {
			log.debug("API error",e);
		}

		return null;
	}

	public void init() {

		// Login
		try {

			this.getLoginToken();

			for (Device device : this.getDevices()) {
				devices.put(device.getId(),device);
			}

		} catch (Exception e) {
			log.error("Could not start : " + e.getMessage());
		}
	}

	public Device getDevice(Integer deviceId) {
		Device device = devices.get(deviceId);
		if(device == null) {
			try {
				String deviceString = requestApi("GET",DEVICES_URL+"/"+deviceId);
				return objectMapper.readValue(deviceString,TypeFactory.defaultInstance().constructType(Device.class));
			} catch (Exception e) {
				log.error("Could not get device",e);
			}
		}
		return device;
	}

	public List<Device> getDevices() throws Exception {
		String devicesString = requestApi("GET",DEVICES_URL);
		return objectMapper.readValue(devicesString,
				TypeFactory.defaultInstance().constructCollectionType(List.class, Device.class));
	}

	// http://localhost:2080/devices/65538/blind/50 blindPosition between 0 and 100
	public void changeBlind(Integer deviceId, Integer blindPosition) {
		try {
			String changeBlindResponse = requestApi("PUT",DEVICES_URL+"/"+deviceId+"/blind/"+blindPosition,new HashMap<>());
			log.debug("Blind "+deviceId+" changed to "+blindPosition+"!");
			log.debug("response: "+changeBlindResponse);
		} catch (Exception e) {
			log.error("Could not change blind",e);
		}
	}
	// http://localhost:2080/devices/65545/dimmer/254	lightDim between 0 and 100
	public void changeLight(Integer deviceId, Double lightDim) {
		try {
			int value = (int)Math.round(lightDim / 100.0 * 254);
			String changeDimResponse = requestApi("PUT",DEVICES_URL+"/"+deviceId+"/dimmer/"+value,new HashMap<>());
			log.debug("Blind "+deviceId+" changed to "+value+"!");
			log.debug("response: "+changeDimResponse);
		} catch (Exception e) {
			log.error("Could not change dimmer",e);
		}
	}
	// http://localhost:2080/devices/65545/dimmer/1	1 or 0
	public void changeSocket(Integer deviceId, Boolean onOff) {
		try {
			Integer value = onOff?1:0;
			String changeSocketResponse = requestApi("PUT",DEVICES_URL+"/"+deviceId+"/state/"+value,new HashMap<>());
			log.debug("Socket "+deviceId+" changed to "+value+"!");
			log.debug("response: "+changeSocketResponse);
		} catch (Exception e) {
			log.error("Could not change socket",e);
		}
	}

	public enum IkeaDeviceType implements EnumConverter<String, IkeaDeviceType> {
		LIGHT("light"), BLIND("blind"), SOCKET("socket"), REPEATER("repeater"), UNKNOWN("unknown");

		private final String value;

		IkeaDeviceType(String value) {
			this.value = value;
		}

		private static ReverseEnumMap<String, IkeaDeviceType> map = new ReverseEnumMap<>(IkeaDeviceType.class);

		@Override
		public String convert() {
			return this.value;
		}

		public static IkeaDeviceType convert(String val) {
			return IkeaDeviceType.map.get(val);
		}

		public String getValue() {
			return this.value;
		}

	}


}
