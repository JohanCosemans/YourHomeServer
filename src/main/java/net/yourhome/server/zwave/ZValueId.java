package net.yourhome.server.zwave;

import java.math.BigInteger;

import org.zwave4j.ValueId;

public class ZValueId extends ValueId {

	private BigInteger valueId;

	public ZValueId(ValueId v) {
		super(v.getHomeId(), v.getNodeId(), v.getGenre(), v.getCommandClassId(), v.getInstance(), v.getIndex(), v.getType());
		valueId = ZWaveController.getInstance().getValueId(v);
	}

	/**
	 * @return the valueId
	 */
	public BigInteger getValueId() {
		return valueId;
	}

	/**
	 * @param valueId
	 *            the valueId to set
	 */
	public void setValueId(BigInteger valueId) {
		this.valueId = valueId;
	}

	@Override
	public boolean equals(Object v) {
		if (v instanceof ValueId) {
			if (((ValueId) v).getHomeId() == this.getHomeId() && ((ValueId) v).getNodeId() == this.getNodeId() && ((ValueId) v).getGenre() == this.getGenre() && ((ValueId) v).getCommandClassId() == this.getCommandClassId() && ((ValueId) v).getInstance() == this.getInstance() && ((ValueId) v).getIndex() == this.getIndex() && ((ValueId) v).getType() == this.getType()) {
				return true;
			}
		}

		return false;
	}

}
