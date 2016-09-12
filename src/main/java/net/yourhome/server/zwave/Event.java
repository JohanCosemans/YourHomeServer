package net.yourhome.server.zwave;

import org.zwave4j.ValueId;

public class Event {
	private ValueId originalValueId;
	
	private String valueId;
	private String valueLabel = "Event";
	private long homeId;
	private int instance;

	public Event(ValueId valueId) {
		this.originalValueId = valueId;
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

	/**
	 * @return the homeId
	 */
	public long getHomeId() {
		return homeId;
	}

	/**
	 * @param homeId
	 *            the homeId to set
	 */
	public void setHomeId(long homeId) {
		this.homeId = homeId;
	}

	/**
	 * @return the valueId
	 */
	public String getValueId() {
		return valueId;
	}

	/**
	 * @param valueId
	 *            the valueId to set
	 */
	public void setValueId(String valueId) {
		this.valueId = valueId;
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
	 * @return the instance
	 */
	public int getInstance() {
		return instance;
	}

	/**
	 * @param instance
	 *            the instance to set
	 */
	public void setInstance(int instance) {
		this.instance = instance;
	}
}
