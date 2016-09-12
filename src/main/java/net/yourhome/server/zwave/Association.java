package net.yourhome.server.zwave;

public class Association {
	private short fromNode;
	private short toNode;
	private int associationClass;

	public Association(short fromNode, short toNode, int associationClass) {
		this.fromNode = fromNode;
		this.toNode = toNode;
		this.associationClass = associationClass;
	}

	/**
	 * @return the fromNode
	 */
	public short getFromNode() {
		return fromNode;
	}

	/**
	 * @param fromNode
	 *            the fromNode to set
	 */
	public void setFromNode(short fromNode) {
		this.fromNode = fromNode;
	}

	/**
	 * @return the toNode
	 */
	public short getToNode() {
		return toNode;
	}

	/**
	 * @param toNode
	 *            the toNode to set
	 */
	public void setToNode(short toNode) {
		this.toNode = toNode;
	}

	/**
	 * @return the associationClass
	 */
	public int getAssociationClass() {
		return associationClass;
	}

	/**
	 * @param associationClass
	 *            the associationClass to set
	 */
	public void setAssociationClass(int associationClass) {
		this.associationClass = associationClass;
	}

}
