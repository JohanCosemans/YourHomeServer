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
		setNodeId(originalValueId.getNodeId());
		setHomeId(originalValueId.getHomeId());
		setValueId(controller.getValueId(originalValueId));
		setHelp(controller.getValueHelp(originalValueId));
		setInstance(originalValueId.getInstance());
		setHomeId(originalValueId.getHomeId());
		setCommandClass((byte) originalValueId.getCommandClassId());
		setValueGenre(originalValueId.getGenre());
		setValueGenreTxt(originalValueId.getGenre().toString());
		setValueType(originalValueId.getType());
		setValueTypeTxt(originalValueId.getType().toString());
		setValueUnit(controller.getValueUnits(originalValueId));
		setValueIndex((byte) originalValueId.getIndex());
		setValueLabel(controller.getValueLabel(originalValueId));
		setMaxValue(controller.getManager().getValueMax(originalValueId));
		
		// If the valuetype is LIST, also add the possible options:
		if (originalValueId.getType() == ValueType.LIST) {
			setValueList(controller.getValueListItems(valueId));
			setValueListSelection(controller.getValueListSelection(valueId));
		} else {
			setValueList(null);
		}
	}	
	
	public void readProperties() {
		setValue(String.valueOf(ZWaveController.getValueOfValue(originalValueId)));
		setPolled(controller.getValuePolled(originalValueId));
		setReadOnly(controller.getValueReadOnly(originalValueId));

		// Read details from DB and overwrite // complete settings
		DatabaseConnector.ValueSettings valueSettings = DatabaseConnector.getInstance().getZWaveValueSettings(getHomeId(), getNodeId(), getValueId(), getInstance());
		setSubscribed(valueSettings.subscribed);

		String alias = DatabaseConnector.getInstance().getAlias(ZWaveNetController.getInstance().getIdentifier(), ZWaveController.getNodeIdentifier(getNodeId(), getHomeId()), getControlId());
		if (alias != null) {
			setValueLabel(alias);
		}
		setSubscribed(valueSettings.subscribed);
	}

	/**
	 * @return the originalValueId
	 */
	public ValueId getOriginalValueId() {
		return originalValueId;
	}

	/**
	 * @param originalValueId the originalValueId to set
	 */
	public void setOriginalValueId(ValueId originalValueId) {
		this.originalValueId = originalValueId;
	}

	public String getControlId() {
		return zwaveDetails.toControlId();
	}

	/**
	 * @return the maxValue
	 */
	public int getMaxValue() {
		return maxValue;
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
		return valueListSelection;
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
		return valueList;
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
	 * Config = valueId::ValueGenre_Config, System =
	 * valueId::ValueGenre_System
	 */

	private byte valueIndex;

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
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
		return valueUnit;
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
		return zwaveDetails.getValueId();
	}

	public void setValueId(BigInteger valueId) {
		zwaveDetails.setValueId(valueId);
	}

	/**
	 * @return the valueType
	 */
	public int getValueType() {
		return valueType.ordinal();
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
		return valueTypeTxt;
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
		return help;
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
		return zwaveDetails.getCommandClass().convert();
	}

	/**
	 * @param commandClass
	 *            the commandClass to set
	 */
	public void setCommandClass(byte commandClass) {
		zwaveDetails.setCommandClass(ZWaveCommandClassTypes.fromByte(commandClass));
	}

	public void setNodeId(short nodeId) {
		zwaveDetails.setNodeId(nodeId);
	}

	public short getNodeId() {
		return zwaveDetails.getNodeId();
	}

	/**
	 * @return the valueGenre
	 */
	public int getValueGenre() {
		return valueGenre.ordinal();
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
		return valueGenreTxt;
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
		return valueLabel;
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
		return readOnly;
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
		return polled;
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
		return zwaveDetails.getInstance();
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
		return subscribed;
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
		return zwaveDetails.getHomeId();
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
		return valueIndex;
	}

	/**
	 * @param valueIndex
	 *            the valueIndex to set
	 */
	public void setValueIndex(byte valueIndex) {
		this.valueIndex = valueIndex;
	}
}
