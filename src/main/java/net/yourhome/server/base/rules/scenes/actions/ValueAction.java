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
package net.yourhome.server.base.rules.scenes.actions;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONException;
import org.json.JSONObject;

import net.yourhome.common.net.messagestructures.general.SetValueMessage;
import net.yourhome.server.base.rules.scenes.Scene;
import net.yourhome.server.net.Server;

public class ValueAction extends Action {

	private int valueActionId;
	private String value;

	public ValueAction(Scene parentScene, JSONObject actionObject) throws JSONException {
		super(parentScene, actionObject);
		JSONObject detailsObject = actionObject.getJSONObject("details");
		this.value = detailsObject.get("value").toString();
	}

	public ValueAction(Scene parentScene, ResultSet actionObject) throws SQLException {
		super(parentScene, actionObject);
		this.value = actionObject.getString("value");
		this.valueActionId = actionObject.getInt("valueActionId");
	}

	@Override
	public boolean perform() {
		SetValueMessage setValue = new SetValueMessage();
		setValue.broadcast = false;
		setValue.controlIdentifiers = this.identifiers;
		setValue.value = this.value;
		Server.getInstance().processMessage(setValue);
		return true;
	}

	/**
	 * @return the valueActionId
	 */
	public int getValueActionId() {
		return this.valueActionId;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return this.value;
	}

}
