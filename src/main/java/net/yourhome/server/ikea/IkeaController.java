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

import com.jayway.jsonpath.Configuration;
import net.yourhome.common.base.enums.ControllerTypes;
import net.yourhome.common.base.enums.EnumConverter;
import net.yourhome.common.base.enums.ReverseEnumMap;
import net.yourhome.common.base.enums.ValueTypes;
import net.yourhome.common.net.messagestructures.JSONMessage;
import net.yourhome.common.net.messagestructures.general.*;
import net.yourhome.common.net.messagestructures.http.HttpCommand;
import net.yourhome.common.net.messagestructures.http.HttpCommandMessage;
import net.yourhome.common.net.model.binding.ControlIdentifiers;
import net.yourhome.server.AbstractController;
import net.yourhome.server.ControllerNode;
import net.yourhome.server.ControllerValue;
import net.yourhome.server.IController;
import net.yourhome.server.base.*;
import net.yourhome.server.http.HttpCommandController;
import net.yourhome.server.net.Server;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.*;

public class IkeaController extends AbstractController {
	public enum Settings {

		IKEA_HUB_IP(
				new Setting("IKEA_HUB_IP", "Ikea IP Hub", "192.168.0.246")
		),IKEA_HUB_SECURITY_TOKEN(
				new Setting("IKEA_HUB_SECURITY_TOKEN", "Ikea Hub Security Token (on the back)")
		), IKEA_HUB_REST_PROTOCOL(
				new Setting("IKEA_HUB_REST_PROTOCOL", "IKEA Rest protocol (default: http)")
		), IKEA_HUB_REST_IP(
				new Setting("IKEA_HUB_REST_IP", "IKEA Rest API address (default: localhost)")
		),IKEA_HUB_REST_PORT(
				new Setting("IKEA_HUB_REST_PORT", "IKEA Rest API port (default: 2080)")
		);

		private Setting setting;

		private Settings(Setting setting) {
			this.setting = setting;
		}

		public Setting get() {
			return this.setting;
		}
	}

	private IkeaNetController ikeaNetController;

	private boolean enabled;

	private Map<String, ControllerNode> controllerMap = new LinkedHashMap<String, ControllerNode>();
	private Map<String, ValueChangedMessage> values = new HashMap<String, ValueChangedMessage>();

	private static IkeaController instance;
	private static final Object lock = new Object();

	private IkeaController() {
		this.log = Logger.getLogger(IkeaController.class);
	}

	public static IkeaController getInstance() {
		IkeaController r = IkeaController.instance;
		if (r == null) {
			synchronized (IkeaController.lock) {
				r = IkeaController.instance;
				if (r == null) {
					r = new IkeaController();
					IkeaController.instance = r;
				}
			}
		}
		return IkeaController.instance;
	}

	@Override
	public String getIdentifier() {
		return ControllerTypes.IKEA.convert();
	}

	@Override
	public void init() {
		super.init();

		ikeaNetController = new IkeaNetController(
			SettingsManager.getStringValue(this.getIdentifier(), Settings.IKEA_HUB_SECURITY_TOKEN.get()),
			SettingsManager.getStringValue(this.getIdentifier(), Settings.IKEA_HUB_IP.get()),
			SettingsManager.getStringValue(this.getIdentifier(), Settings.IKEA_HUB_REST_PROTOCOL.get()),
			SettingsManager.getStringValue(this.getIdentifier(), Settings.IKEA_HUB_REST_IP.get()),
			SettingsManager.getStringValue(this.getIdentifier(), Settings.IKEA_HUB_REST_PORT.get())
		);

		if (ikeaNetController.incomplete()) {
			this.log.info("Could not find Ikea settings. Disabling Ikea.");
			this.enabled = false;
		} else {
			this.enabled = true;

			// Poll for updates each 5 minutes
			/*Scheduler.getInstance().scheduleCron(new TimerTask() {
				@Override
				public void run() {
					try {
						IkeaController.this.updateThermostatDetails();
					} catch (Exception e) {

					}
				}
			}, "0,5,10,15,20,25,30,35,40,45,50,55 * * * *");*/

			// Initialize node structure
			this.getNodes();

			this.log.info("Initialized");
		}
	}

	@Override
	public List<JSONMessage> initClient() {
		return new ArrayList<>(this.values.values());
	}

	@Override
	public boolean isEnabled() {
		return this.enabled;
	}

	@Override
	public String getName() {
		return "Ikea Smart Home";
	}

	private List<ControllerValue> parseValues(Device ikeaDevice) {
		List<ControllerValue> parsedValues = new ArrayList<>();
		switch(ikeaDevice.getIkeaDeviceType()) {
			case BLIND:
				parsedValues.add(new ControllerValue("blind", "Blind Level", ValueTypes.DIMMER));
			break;
			case LIGHT:
				parsedValues.add(new ControllerValue("dimmer", "Light Level", ValueTypes.DIMMER));
			break;
			case SOCKET:
				parsedValues.add(new ControllerValue("state", "Switch state", ValueTypes.SWITCH_BINARY));
			break;
			case REPEATER:
			case UNKNOWN:
				break;
		}
		return parsedValues;
	}

	@Override
	public Collection<ControllerNode> getNodes() {

		if (this.controllerMap.size() == 0) {
			try {
				// Get Devices and index them by id

				List<Device> devices = ikeaNetController.getDevices();
				for(Device device : devices) {
					ControllerNode node = new ControllerNode(this, device.getId()+"", device.getName(), "");
					for(ControllerValue value : parseValues(device)) {
						node.addValue(value);
					}
					this.controllerMap.put(node.getIdentifier(), node);
				}

			} catch (Exception e) {
				log.error("Exception when parsing ikea nodes",e);
			}
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
		return !this.values.isEmpty();
	}

	@Override
	public List<Setting> getSettings() {
		List<Setting> returnList = new ArrayList<Setting>();
		for (Settings s : Settings.values()) {
			returnList.add(s.setting);
		}
		return returnList;
	}

	@Override
	public JSONMessage parseNetMessage(JSONMessage message) {
		/*if (message instanceof ValueHistoryRequest) {
			// Let the general controller handle this one
			IController generalController = Server.getInstance().getControllers().get(ControllerTypes.GENERAL.convert());
			if (generalController != null) {
				return generalController.parseNetMessage(message);
			}
		}else if (message instanceof SetValueMessage) {
			SetValueMessage setValueMessage = (SetValueMessage) message;
			ValueChangedMessage valueChanged = new ValueChangedMessage(message);
			valueChanged.value = setValueMessage.value;
			ControllerValue value = this.getControllerValue(message.controlIdentifiers);
			if (value != null) {
				valueChanged.valueType = ValueTypes.convert(value.getValueType());
			}
			valueChanged.broadcast = true;

			return valueChanged;
		}*/
		return null;
	}


}
