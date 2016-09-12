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
 * THIS SOFTWARE IS PROVIDED BY THE NETBSD FOUNDATION, INC. AND CONTRIBUTORS
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

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import net.yourhome.common.base.enums.ValueTypes;
import net.yourhome.common.net.messagestructures.general.ActivationMessage;
import net.yourhome.common.net.model.binding.ControlIdentifiers;
import net.yourhome.server.base.DatabaseConnector;
import net.yourhome.server.base.rules.scenes.Scene;
import net.yourhome.server.net.Server;

public class Action {
	protected Scene parentScene;

	protected int id;
	protected ControlIdentifiers identifiers;
	protected int sequence;
	protected ValueTypes valueType;
	protected static Logger log = Logger.getLogger(Scene.class);

	protected DatabaseConnector db = DatabaseConnector.getInstance();

	public Action(Scene parentScene, JSONObject actionObject) throws JSONException {
		this.parentScene = parentScene;
		try {
			this.id = actionObject.getInt("id");
		} catch (JSONException e) {
		}
		this.identifiers = new ControlIdentifiers(actionObject);
		this.valueType = ValueTypes.convert(actionObject.getString("valueType"));
	}

	public Action(Scene parentScene, ResultSet actionObject) throws SQLException {
		this.parentScene = parentScene;
		this.id = actionObject.getInt("id");
		this.identifiers = new ControlIdentifiers(actionObject);
		this.sequence = actionObject.getInt("sequence");
		this.valueType = ValueTypes.convert(actionObject.getString("value_type"));
	}

	public boolean perform() {
		ActivationMessage activation = new ActivationMessage();
		activation.broadcast = false;
		activation.controlIdentifiers = this.identifiers;
		Server.getInstance().processMessage(activation);
		return true;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * @return the identifiers
	 */
	public ControlIdentifiers getIdentifiers() {
		return this.identifiers;
	}

	/**
	 * @return the sequence
	 */
	public int getSequence() {
		return this.sequence;
	}

	/**
	 * @param sequence
	 *            the sequence to set
	 */
	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	/**
	 * @return the valueType
	 */
	public ValueTypes getValueType() {
		return this.valueType;
	}

	public void setParentScene(Scene parentScene) {
		this.parentScene = parentScene;
	}

}
