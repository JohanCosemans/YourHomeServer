package net.yourhome.server;

import net.yourhome.common.base.enums.ValueTypes;

public class ControllerValue {

	private String name;
	private ValueTypes valueType;
	private String identifier;
	private boolean virtual = false;

	public ControllerValue(String identifier, String name, ValueTypes valueType) {
		this.name = name;
		this.identifier = identifier;
		this.valueType = valueType;
	}

	
	/**
	 * @return the virtual
	 */
	public boolean isVirtual() {
		return virtual;
	}


	/**
	 * @param virtual the virtual to set
	 */
	public void setVirtual(boolean virtual) {
		this.virtual = virtual;
	}


	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the identifier
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * @param identifier
	 *            the identifier to set
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getValueType() {
		return valueType.convert();
	}

	public void setValueType(ValueTypes valueType) {
		this.valueType = valueType;
	}

}
