package net.yourhome.server;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ControllerNode {

	//private List<ControllerValue> values = new ArrayList<ControllerValue>();
	private Map<String,ControllerValue> values = new LinkedHashMap<String,ControllerValue>();
	private IController controller;
	private String name;
	private String type;
	private String identifier;

	public ControllerNode(IController controller, String identifier, String name, String type) {
		this.controller = controller;
		this.name = name;
		this.identifier = identifier;
		if (type == null || type.equals("")) {
			this.type = "unknown_node";
		} else {
			this.type = type;
		}
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

	/**
	 * @return the values
	 */
	public Collection<ControllerValue> getValues() {
		return values.values();
	}
	
	public ControllerValue getValue(String valueIdentifier) {
		return values.get(valueIdentifier);
	}

	public void addValue(ControllerValue v) {
		this.values.put(v.getIdentifier(),v);
	}

	/**
	 * @return the style
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param style
	 *            the style to set
	 */
	public void setType(String type) {
		this.type = type;
	}

}
