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
		return this.originalValueId;
	}

	/**
	 * @param originalValueId
	 *            the originalValueId to set
	 */
	public void setOriginalValueId(ValueId originalValueId) {
		this.originalValueId = originalValueId;
	}

	/**
	 * @return the homeId
	 */
	public long getHomeId() {
		return this.homeId;
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
		return this.valueId;
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
	 * @return the instance
	 */
	public int getInstance() {
		return this.instance;
	}

	/**
	 * @param instance
	 *            the instance to set
	 */
	public void setInstance(int instance) {
		this.instance = instance;
	}
}
