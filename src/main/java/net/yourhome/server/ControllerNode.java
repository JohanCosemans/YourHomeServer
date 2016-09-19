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
package net.yourhome.server;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ControllerNode {

	// private List<ControllerValue> values = new ArrayList<ControllerValue>();
	private Map<String, ControllerValue> values = new LinkedHashMap<String, ControllerValue>();
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
		return this.name;
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
		return this.identifier;
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
		return this.values.values();
	}

	public ControllerValue getValue(String valueIdentifier) {
		return this.values.get(valueIdentifier);
	}

	public void addValue(ControllerValue v) {
		this.values.put(v.getIdentifier(), v);
	}

	/**
	 * @return the style
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * @param style
	 *            the style to set
	 */
	public void setType(String type) {
		this.type = type;
	}

}
