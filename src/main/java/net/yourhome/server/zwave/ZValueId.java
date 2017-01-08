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

import org.zwave4j.ValueId;

import java.math.BigInteger;

public class ZValueId extends ValueId {

	private BigInteger valueId;

	public ZValueId(ValueId v) {
		super(v.getHomeId(), v.getNodeId(), v.getGenre(), v.getCommandClassId(), v.getInstance(), v.getIndex(), v.getType());
		this.valueId = ZWaveManager.getInstance().getValueId(v);
	}

	/**
	 * @return the valueId
	 */
	public BigInteger getValueId() {
		return this.valueId;
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
