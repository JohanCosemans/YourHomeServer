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
package net.yourhome.server.demo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import net.yourhome.common.base.enums.ValueTypes;
import net.yourhome.common.net.messagestructures.JSONMessage;
import net.yourhome.common.net.messagestructures.general.SetValueMessage;
import net.yourhome.common.net.messagestructures.general.ValueChangedMessage;
import net.yourhome.common.net.messagestructures.general.ValueHistoryMessage;
import net.yourhome.common.net.messagestructures.general.ValueHistoryRequest;
import net.yourhome.common.net.model.binding.ControlIdentifiers;
import net.yourhome.server.AbstractController;
import net.yourhome.server.ControllerNode;
import net.yourhome.server.ControllerValue;
import net.yourhome.server.base.Setting;
import net.yourhome.server.base.SettingsManager;

public class DemoController extends AbstractController {
	public enum Settings {

		DEMO_MODE(new Setting("DEMO_MODE", "", "false"));

		private Setting setting;

		private Settings(Setting setting) {
			this.setting = setting;
		}

		public Setting get() {
			return this.setting;
		}
	}

	private static volatile DemoController instance;
	private static Object lock = new Object();

	private DemoController() {
		this.log = Logger.getLogger("net.yourhome.server.ipcamera.IPCamera");
	}

	public static DemoController getInstance() {
		DemoController r = DemoController.instance;
		if (r == null) {
			synchronized (DemoController.lock) { // while we were waiting for
													// the lock, another
				r = DemoController.instance; // thread may have instantiated the
				// object
				if (r == null) {
					r = new DemoController();
					DemoController.instance = r;
				}
			}
		}

		return DemoController.instance;
	}

	@Override
	public String getIdentifier() {
		return "demo";
	}

	@Override
	public String getName() {
		return "Demo";
	}

	@Override
	public String getValueName(ControlIdentifiers valueIdentifiers) {
		return null;
	}

	@Override
	public String getValue(ControlIdentifiers valueIdentifiers) {
		return null;
	}

	private ControllerValue getControllerValue(ControlIdentifiers identifiers) {
		List<ControllerNode> nodes = this.getNodes();
		for (ControllerNode node : nodes) {
			if (node.getIdentifier().equals(identifiers.getNodeIdentifier())) {
				for (ControllerValue v : node.getValues()) {
					if (v.getIdentifier().equals(identifiers.getValueIdentifier())) {
						return v;
					}
				}
			}
		}
		return null;
	}

	@Override
	public List<JSONMessage> initClient() {

		/* Send a (random) value for each demo value */
		List<JSONMessage> returnList = new ArrayList<JSONMessage>();
		List<ControllerNode> allNodes = this.getNodes();
		for (ControllerNode node : allNodes) {
			for (ControllerValue v : node.getValues()) {
				ControlIdentifiers c = new ControlIdentifiers(this.getIdentifier(), node.getIdentifier(), v.getIdentifier());
				ValueChangedMessage m = this.generateValueFor(c, ValueTypes.convert(v.getValueType()), null);
				if (m != null) {
					returnList.add(m);
				}
			}
		}

		return returnList;
	}

	public ValueHistoryMessage generateValuesFor(ValueHistoryRequest c) {
		List<ControllerNode> allNodes = this.getNodes();
		ValueHistoryMessage returnMessage = new ValueHistoryMessage();
		returnMessage.controlIdentifiers = c.controlIdentifiers;
		for (ControllerNode n : allNodes) {
			if (n.getIdentifier().equals(c.controlIdentifiers.getNodeIdentifier())) {
				for (ControllerValue v : n.getValues()) {
					if (v.getIdentifier().equals(c.controlIdentifiers.getValueIdentifier())) {

						long time = new Date().getTime();
						for (int i = 0; i < c.historyAmount; i++) {
							time -= 3600L * c.historyAmount;
							ValueChangedMessage m = null;
							if (i > 0) {
								m = this.generateValueFor(c.controlIdentifiers, ValueTypes.convert(v.getValueType()), returnMessage.sensorValues.value.get(i - 1));
							} else {
								m = this.generateValueFor(c.controlIdentifiers, ValueTypes.convert(v.getValueType()), null);
							}
							returnMessage.sensorValues.time.add((int) (time / 1000L));
							returnMessage.sensorValues.value.add(Double.parseDouble(m.value));
						}
						break;
					}
				}
			}
		}
		returnMessage.title = c.controlIdentifiers.getValueIdentifier().toLowerCase();

		return returnMessage;
	}

	private ValueChangedMessage generateValueFor(ControlIdentifiers c, ValueTypes v, Double previousValue) {

		ValueChangedMessage valueChanged = new ValueChangedMessage();
		valueChanged.broadcast = false;
		valueChanged.controlIdentifiers = c;
		valueChanged.valueType = v;
		switch (v) {
		case DIMMER:
			valueChanged.value = "" + String.format("%.0f", (float) Math.abs(this.generateNewValue(100.0, 20.0, previousValue))).replace(",", ".");
			break;
		case HEATING:
			valueChanged.value = "" + String.format("%.2f", (float) Math.abs(Math.round(this.generateNewValue(30.0, 1.0, previousValue)))).replace(",", ".");
			valueChanged.unit = "°C";
			break;
		case METER:
			valueChanged.value = "" + String.format("%.2f", (float) Math.abs(this.generateNewValue(1500.0, 150.0, previousValue))).replace(",", ".");
			valueChanged.unit = "kWh";
			break;
		/*
		 * case MUSIC_ALBUM_IMAGE: break; case MUSIC_PLAYLIST: break; case
		 * MUSIC_PLAY_PAUSE: break; case MUSIC_PROGRESS: break; case
		 * MUSIC_RANDOM: break; case MUSIC_TRACK_DISPLAY: break;
		 */
		case SENSOR_BINARY:
			valueChanged.value = "true";
			break;
		case SENSOR_GENERAL:
			valueChanged.value = String.format("%.2f", (float) Math.abs(this.generateNewValue(1500.0, 150.0, previousValue))).replace(",", ".");
			valueChanged.unit = "W";
			break;
		case SENSOR_HUMIDITY:
			valueChanged.value = String.format("%.2f", (float) Math.abs(this.generateNewValue(120.0, 20.0, previousValue))).replace(",", ".");
			valueChanged.unit = "%";
			break;
		case SENSOR_LUMINOSITY:
			valueChanged.value = String.format("%.0f", (float) Math.abs(this.generateNewValue(500.0, 10.0, previousValue))).replace(",", ".");
			valueChanged.unit = "lux";
			break;
		case SENSOR_MOTION:
			valueChanged.value = "true";
			break;
		case SENSOR_TEMPERATURE:
			valueChanged.value = "" + String.format("%.1f", this.generateNewValue(30.0, 1.0, previousValue)).replace(",", ".");
			valueChanged.unit = "°C";
			break;
		case SWITCH_BINARY:
			valueChanged.value = "true";
			break;
		case TEXT:
			valueChanged.value = "Random text";
			break;
		default:
			return null;
		}
		return valueChanged;
	}

	private Double generateNewValue(Double maxValue, Double maxDelta, Double previousValue) {
		if (previousValue != null) {
			Double deltaValue = Math.random() * maxDelta;
			deltaValue = Math.random() > 0.5 ? -1 * deltaValue : deltaValue;
			Double newValue = previousValue + deltaValue;
			return newValue;
		} else {
			return Math.random() * maxValue;
		}
	}

	@Override
	public boolean isEnabled() {
		Boolean isEnabled = SettingsManager.getStringValue(this.getIdentifier(), DemoController.Settings.DEMO_MODE.get(), "false").equals("true");
		return isEnabled;
	}

	@Override
	public boolean isInitialized() {
		return true;
	}

	@Override
	public JSONMessage parseNetMessage(JSONMessage message) {
		if (message instanceof ValueHistoryRequest) {
			return (this.generateValuesFor((ValueHistoryRequest) message));
		} else if (message instanceof SetValueMessage) {
			SetValueMessage setValueMessage = (SetValueMessage) message;
			ValueChangedMessage valueChanged = new ValueChangedMessage(message);
			valueChanged.value = setValueMessage.value;
			ControllerValue value = this.getControllerValue(message.controlIdentifiers);
			if (value != null) {
				valueChanged.valueType = ValueTypes.convert(value.getValueType());
			}
			valueChanged.broadcast = true;

			return valueChanged;
		}
		return null;
	}

	private List<ControllerNode> nodesBuffer = null;

	@Override
	public List<ControllerNode> getNodes() {
		if (this.nodesBuffer == null) {
			this.nodesBuffer = new ArrayList<ControllerNode>();
			ControllerNode measurementsNode = new ControllerNode(this, "Sensors", "Sensors", "");
			measurementsNode.addValue(new ControllerValue("TEMPERATURE", "Temperature Sensor", ValueTypes.SENSOR_TEMPERATURE));
			measurementsNode.addValue(new ControllerValue("MOTION", "Motion Sensor", ValueTypes.SENSOR_MOTION));
			measurementsNode.addValue(new ControllerValue("BINARY", "Binary Sensor (on/off)", ValueTypes.SENSOR_BINARY));
			measurementsNode.addValue(new ControllerValue("ALARM", "Alarm Sensor", ValueTypes.SENSOR_ALARM));
			measurementsNode.addValue(new ControllerValue("HUMIDITY", "Humidity Sensor", ValueTypes.SENSOR_HUMIDITY));
			measurementsNode.addValue(new ControllerValue("LUMINOSITY", "Luminosity Sensor", ValueTypes.SENSOR_LUMINOSITY));
			measurementsNode.addValue(new ControllerValue("METER", "Electricity meter", ValueTypes.METER));
			measurementsNode.addValue(new ControllerValue("HEATING", "Setpoint Thermostat", ValueTypes.HEATING));
			this.nodesBuffer.add(measurementsNode);

			ControllerNode lightsNode = new ControllerNode(this, "Lights", "Lights", "");
			lightsNode.addValue(new ControllerValue("BINARY_1", "On Off Switch 1", ValueTypes.SWITCH_BINARY));
			lightsNode.addValue(new ControllerValue("BINARY_2", "On Off Switch 2", ValueTypes.SWITCH_BINARY));
			lightsNode.addValue(new ControllerValue("BINARY_3", "On Off Switch 3", ValueTypes.SWITCH_BINARY));
			lightsNode.addValue(new ControllerValue("DIMMER_1", "Dimmer 1", ValueTypes.DIMMER));
			lightsNode.addValue(new ControllerValue("DIMMER_2", "Dimmer 2", ValueTypes.DIMMER));
			lightsNode.addValue(new ControllerValue("DIMMER_3", "Dimmer 3", ValueTypes.DIMMER));
			this.nodesBuffer.add(lightsNode);

			ControllerNode musicNode = new ControllerNode(this, "Music", "Music Player", "");
			musicNode.addValue(new ControllerValue("NEXT", "Next", ValueTypes.MUSIC_ACTION));
			musicNode.addValue(new ControllerValue("PREVIOUS", "Previous", ValueTypes.MUSIC_ACTION));
			musicNode.addValue(new ControllerValue("PLAY_PAUSE", "Play / Pause", ValueTypes.MUSIC_PLAY_PAUSE));
			musicNode.addValue(new ControllerValue("ALBUM_IMAGE", "Album Image", ValueTypes.MUSIC_ALBUM_IMAGE));
			musicNode.addValue(new ControllerValue("PLAYLIST", "Playlist", ValueTypes.MUSIC_PLAYLIST));
			musicNode.addValue(new ControllerValue("SELECT_PLAYLIST", "Select Playlist", ValueTypes.MUSIC_PLAYLISTS));
			musicNode.addValue(new ControllerValue("PROGRESS", "Song Progress", ValueTypes.MUSIC_PROGRESS));
			musicNode.addValue(new ControllerValue("RANDOM", "Toggle Random", ValueTypes.MUSIC_RANDOM));
			musicNode.addValue(new ControllerValue("TITLE_ARTIST", "Title / Artist", ValueTypes.MUSIC_TRACK_DISPLAY));
			this.nodesBuffer.add(musicNode);
		}
		return this.nodesBuffer;
	}

	@Override
	public List<ControllerNode> getTriggers() {
		List<ControllerNode> allNodes = this.getNodes();
		for (int i = 0; i < allNodes.size(); i++) {
			if (allNodes.get(i).getIdentifier().equals("Music")) {
				allNodes.remove(i--);
			}
		}

		return allNodes;
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
