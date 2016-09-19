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
package net.yourhome.server.zwave;

import java.math.BigInteger;
import java.util.List;

import org.zwave4j.ValueGenre;
import org.zwave4j.ValueId;
import org.zwave4j.ValueType;

import net.yourhome.common.base.enums.ValueTypes;
import net.yourhome.common.base.enums.zwave.ZWaveCommandClassTypes;
import net.yourhome.common.net.messagestructures.zwave.ZWaveValue;
import net.yourhome.server.base.DatabaseConnector;

public class Value {

	private ValueId originalValueId;

	ZWaveValue zwaveDetails = new ZWaveValue();

	private String value;
	private String valueUnit;
	// private String valueId;
	private ValueType valueType;
	private String valueTypeTxt;
	private String help;
	// private byte commandClass;
	private ValueGenre valueGenre;
	private String valueGenreTxt;
	private String valueLabel;
	private Boolean readOnly;
	private Boolean polled;
	// private int instance;
	private List<String> valueList; // For list values
	private String valueListSelection; // For list values
	private int maxValue;

	private ZWaveController controller;

	public Value(ZWaveController controller, ValueId valueId) {
		this.controller = controller;
		this.originalValueId = valueId;
		this.setNodeId(this.originalValueId.getNodeId());
		this.setHomeId(this.originalValueId.getHomeId());
		this.setValueId(controller.getValueId(this.originalValueId));
		this.setHelp(controller.getValueHelp(this.originalValueId));
		this.setInstance(this.originalValueId.getInstance());
		this.setHomeId(this.originalValueId.getHomeId());
		this.setCommandClass((byte) this.originalValueId.getCommandClassId());
		this.setValueGenre(this.originalValueId.getGenre());
		this.setValueGenreTxt(this.originalValueId.getGenre().toString());
		this.setValueType(this.originalValueId.getType());
		this.setValueTypeTxt(this.originalValueId.getType().toString());
		this.setValueUnit(controller.getValueUnits(this.originalValueId));
		this.setValueIndex((byte) this.originalValueId.getIndex());
		this.setValueLabel(controller.getValueLabel(this.originalValueId));
		this.setMaxValue(controller.getManager().getValueMax(this.originalValueId));

		// If the valuetype is LIST, also add the possible options:
		if (this.originalValueId.getType() == ValueType.LIST) {
			this.setValueList(controller.getValueListItems(valueId));
			this.setValueListSelection(controller.getValueListSelection(valueId));
		} else {
			this.setValueList(null);
		}
	}

	public void readProperties() {
		this.setValue(String.valueOf(ZWaveController.getValueOfValue(this.originalValueId)));
		this.setPolled(this.controller.getValuePolled(this.originalValueId));
		this.setReadOnly(this.controller.getValueReadOnly(this.originalValueId));

		// Read details from DB and overwrite // complete settings
		DatabaseConnector.ValueSettings valueSettings = DatabaseConnector.getInstance()
				.getZWaveValueSettings(this.getHomeId(), this.getNodeId(), this.getValueId(), this.getInstance());
		this.setSubscribed(valueSettings.subscribed);

		String alias = DatabaseConnector.getInstance().getAlias(ZWaveNetController.getInstance().getIdentifier(),
				ZWaveController.getNodeIdentifier(this.getNodeId(), this.getHomeId()), this.getControlId());
		if (alias != null) {
			this.setValueLabel(alias);
		}
		this.setSubscribed(valueSettings.subscribed);
	}

	/**
	 * @return the originalValueId
	 */
	public ValueId getOriginalValueId() {
		return this.originalValueId;
	}

	/**
	 * @param originalValueId
	 *            the originalValueId to set
	 */
	public void setOriginalValueId(ValueId originalValueId) {
		this.originalValueId = originalValueId;
	}

	public String getControlId() {
		return this.zwaveDetails.toControlId();
	}

	/**
	 * @return the maxValue
	 */
	public int getMaxValue() {
		return this.maxValue;
	}

	/**
	 * @param maxValue
	 *            the maxValue to set
	 */
	public void setMaxValue(int maxValue) {
		this.maxValue = maxValue;
	}

	/**
	 * @return the listSelection
	 */
	public String getValueListSelection() {
		return this.valueListSelection;
	}

	/**
	 * @param listSelection
	 *            the listSelection to set
	 */
	public void setValueListSelection(String listSelection) {
		this.valueListSelection = listSelection;
	}

	/**
	 * @return the valueList
	 */
	public List<String> getValueList() {
		return this.valueList;
	}

	/**
	 * @param valueList
	 *            the valueList to set
	 */
	public void setValueList(List<String> valueList) {
		this.valueList = valueList;
	}

	private Boolean subscribed;
	/*
	 * Basic = valueId::ValueGenre_Basic, User = valueId::ValueGenre_User,
	 * Config = valueId::ValueGenre_Config, System = valueId::ValueGenre_System
	 */

	private byte valueIndex;

	/**
	 * @return the value
	 */
	public String getValue() {
		return this.value;
	}

	/**
	 * @param value
	 *            the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * @return the valueUnit
	 */
	public String getValueUnit() {
		return this.valueUnit;
	}

	/**
	 * @param valueUnit
	 *            the valueUnit to set
	 */
	public void setValueUnit(String valueUnit) {
		this.valueUnit = valueUnit;
	}

	/**
	 * @return the valueId
	 */
	public BigInteger getValueId() {
		return this.zwaveDetails.getValueId();
	}

	public void setValueId(BigInteger valueId) {
		this.zwaveDetails.setValueId(valueId);
	}

	/**
	 * @return the valueType
	 */
	public int getValueType() {
		return this.valueType.ordinal();
	}

	/**
	 * @return the valueType
	 */
	public ValueTypes getValueTypeOfValue() {
		ValueTypes valueType = null;
		ZWaveCommandClassTypes commandClass = ZWaveCommandClassTypes.fromByte(this.getCommandClass());

		switch (commandClass) {
		case SwitchBinary:
			valueType = ValueTypes.SWITCH_BINARY;// "zwave-binary-switch";
			break;
		case SwitchMultilevel:
			valueType = ValueTypes.DIMMER;// "zwave-light";
			break;
		case SensorBinary:
			valueType = ValueTypes.SENSOR_BINARY;// "zwave-binary-sensor";
			break;
		case SensorMultiLevel:
			valueType = ValueTypes.SENSOR_GENERAL;// "zwave-sensor";
			break;
		case ThermostatSetpoint:
			valueType = ValueTypes.HEATING;// "zwave-heating";
			break;
		case Meter:
			valueType = ValueTypes.METER;// "zwave-heating";
			break;
		case SensorAlarm:
			valueType = ValueTypes.SENSOR_ALARM;
			break;
		default:
			valueType = ValueTypes.UNKNOWN;
			break;
		}
		return valueType;
	}

	/**
	 * @param valueType
	 *            the valueType to set
	 */
	public void setValueType(ValueType valueType) {
		this.valueType = valueType;
	}

	/**
	 * @return the valueTypeTxt
	 */
	public String getValueTypeTxt() {
		return this.valueTypeTxt;
	}

	/**
	 * @param valueTypeTxt
	 *            the valueTypeTxt to set
	 */
	public void setValueTypeTxt(String valueTypeTxt) {
		this.valueTypeTxt = valueTypeTxt;
	}

	/**
	 * @return the help
	 */
	public String getHelp() {
		return this.help;
	}

	/**
	 * @param help
	 *            the help to set
	 */
	public void setHelp(String help) {
		this.help = help;
	}

	/**
	 * @return the commandClass
	 */
	public byte getCommandClass() {
		return this.zwaveDetails.getCommandClass().convert();
	}

	/**
	 * @param commandClass
	 *            the commandClass to set
	 */
	public void setCommandClass(byte commandClass) {
		this.zwaveDetails.setCommandClass(ZWaveCommandClassTypes.fromByte(commandClass));
	}

	public void setNodeId(short nodeId) {
		this.zwaveDetails.setNodeId(nodeId);
	}

	public short getNodeId() {
		return this.zwaveDetails.getNodeId();
	}

	/**
	 * @return the valueGenre
	 */
	public int getValueGenre() {
		return this.valueGenre.ordinal();
	}

	/**
	 * @param valueGenre
	 *            the valueGenre to set
	 */
	public void setValueGenre(ValueGenre valueGenre) {
		this.valueGenre = valueGenre;
	}

	/**
	 * @return the valueGenreTxt
	 */
	public String getValueGenreTxt() {
		return this.valueGenreTxt;
	}

	/**
	 * @param valueGenreTxt
	 *            the valueGenreTxt to set
	 */
	public void setValueGenreTxt(String valueGenreTxt) {
		this.valueGenreTxt = valueGenreTxt;
	}

	/**
	 * @return the valueLabel
	 */
	public String getValueLabel() {
		return this.valueLabel;
	}

	/**
	 * @param valueLabel
	 *            the valueLabel to set
	 */
	public void setValueLabel(String valueLabel) {
		this.valueLabel = valueLabel;
	}

	/**
	 * @return the readOnly
	 */
	public Boolean getReadOnly() {
		return this.readOnly;
	}

	/**
	 * @param readOnly
	 *            the readOnly to set
	 */
	public void setReadOnly(Boolean readOnly) {
		this.readOnly = readOnly;
	}

	/**
	 * @return the polled
	 */
	public Boolean getPolled() {
		return this.polled;
	}

	/**
	 * @param polled
	 *            the polled to set
	 */
	public void setPolled(Boolean polled) {
		this.polled = polled;
	}

	/**
	 * @return the nodeInstance
	 */
	public short getInstance() {
		return this.zwaveDetails.getInstance();
	}

	/**
	 * @param nodeInstance
	 *            the nodeInstance to set
	 */
	public void setInstance(short nodeInstance) {
		this.zwaveDetails.setInstance(nodeInstance);
	}

	/**
	 * @return the subscribed
	 */
	public Boolean getSubscribed() {
		return this.subscribed;
	}

	/**
	 * @param subscribed
	 *            the subscribed to set
	 */
	public void setSubscribed(Boolean subscribed) {
		this.subscribed = subscribed;
	}

	/**
	 * @return the homeId
	 */
	public long getHomeId() {
		return this.zwaveDetails.getHomeId();
	}

	/**
	 * @param homeId
	 *            the homeId to set
	 */
	public void setHomeId(long homeId) {
		this.zwaveDetails.setHomeId(homeId);
	}

	/**
	 * @return the valueIndex
	 */
	public byte getValueIndex() {
		return this.valueIndex;
	}

	/**
	 * @param valueIndex
	 *            the valueIndex to set
	 */
	public void setValueIndex(byte valueIndex) {
		this.valueIndex = valueIndex;
	}
}
