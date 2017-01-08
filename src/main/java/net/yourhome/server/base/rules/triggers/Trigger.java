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
package net.yourhome.server.base.rules.triggers;

import net.yourhome.common.base.enums.ValueTypes;
import net.yourhome.common.net.model.binding.ControlIdentifiers;
import net.yourhome.server.IController;
import net.yourhome.server.base.DatabaseConnector;
import net.yourhome.server.base.rules.Rule;
import net.yourhome.server.net.Server;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class Trigger implements ITrigger {

	protected ControlIdentifiers identifiers;
	protected ValueTypes valueType;
	protected Rule parentRule;
	protected TriggerTypes type = TriggerTypes.ACTIVATION;
	protected int id;
	protected static Logger log = Logger.getLogger(Trigger.class);
	protected DatabaseConnector db = DatabaseConnector.getInstance();

	public Trigger(Rule parentRule, JSONObject triggerObject) throws JSONException {
		this.parentRule = parentRule;
		this.identifiers = new ControlIdentifiers(triggerObject);
	}

	@Override
	public JSONObject serialize() throws JSONException {
		JSONObject jsonObject = this.identifiers.serialize();
		jsonObject.put("id", this.id);
		return jsonObject;
	}

	@Override
	public void setTrigger() {
		// Add me as value listener to my controller
		IController controller = Server.getInstance().getControllers().get(this.identifiers.getControllerIdentifier().convert());
		controller.addTriggerListener(this);
	}

	@Override
	public void unsetTrigger() {
		IController controller = Server.getInstance().getControllers().get(this.identifiers.getControllerIdentifier().convert());
		controller.removeTriggerListener(this);
	}

	@Override
	public TriggerTypes getType() {
		return this.type;
	}

	@Override
	public boolean evaluate(ITrigger startingTrigger) {
		if (startingTrigger == this) {
			return true;
		}
		return false;
	}

	@Override
	public ControlIdentifiers getIdentifiers() {
		return this.identifiers;
	}

	public void trigger() {
		this.parentRule.evaluate(this);
	}

}
