package net.yourhome.server.zwave;

public class AssociationGroup {
	private short associationGroup;
	private int maxAssociations;
	private String label;

	/**
	 * @return the associationGroup
	 */
	public short getAssociationGroup() {
		return associationGroup;
	}

	/**
	 * @param associationGroup
	 *            the associationGroup to set
	 */
	public void setAssociationGroup(short associationGroup) {
		this.associationGroup = associationGroup;
	}

	/**
	 * @return the maxAssociations
	 */
	public int getMaxAssociations() {
		return maxAssociations;
	}

	/**
	 * @param maxAssociations
	 *            the maxAssociations to set
	 */
	public void setMaxAssociations(int maxAssociations) {
		this.maxAssociations = maxAssociations;
	}

	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @param label
	 *            the label to set
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	public AssociationGroup(short associationGroup, int maxAssociations, String groupLabel) {
		this.associationGroup = associationGroup;
		this.maxAssociations = maxAssociations;
		this.label = groupLabel;
	}

}
