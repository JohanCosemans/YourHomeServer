package net.yourhome.server.base;

public class Setting {

	private String name;
	private String example;
	private String description;

	public Setting(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public Setting(String name, String description, String example) {
		this(name, description);
		this.example = example;
	}

	public String convert() {
		return this.name;
	}

	/**
	 * @return the example
	 */
	public String getExample() {
		return example;
	}

	/**
	 * @param example
	 *            the example to set
	 */
	public void setExample(String example) {
		this.example = example;
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
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description
	 *            the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

}
