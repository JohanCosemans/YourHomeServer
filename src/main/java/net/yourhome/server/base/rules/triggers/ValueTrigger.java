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

import org.json.JSONException;
import org.json.JSONObject;

import net.yourhome.server.IController;
import net.yourhome.server.base.rules.Rule;
import net.yourhome.server.net.Server;

public class ValueTrigger extends Trigger {
	public enum States {
		IS, BECOMES
	}

	protected States isOrBecomes;
	protected String targetValue;
	protected String operand;

	protected String lastValue;

	public ValueTrigger(Rule parentRule, JSONObject triggerObject) throws JSONException {
		super(parentRule, triggerObject);
		JSONObject details = triggerObject.getJSONObject("details");
		this.targetValue = details.getString("value");
		this.operand = details.getString("operand");
		this.type = TriggerTypes.VALUE;
		String isOrBecomesString = "BECOMES";
		try {
			isOrBecomesString = details.getString("isBecomes");
		} catch (JSONException e) {
		}

		this.isOrBecomes = States.valueOf(isOrBecomesString);
	}

	@Override
	public boolean evaluate(ITrigger startingTrigger) {
		switch (this.isOrBecomes) {
		case BECOMES:
			// This means it already went through the trigger() method of this
			// trigger and was evaluated back then.
			// Only return true if the trigger was me.
			return startingTrigger == this;
		case IS:
			// Evaluate the current value of this trigger
			return this.evaluate();
		}

		return this.evaluate();
	}

	private boolean evaluate() {
		boolean trigger = false;
		// Go get the actual value from the controller
		IController myController = Server.getInstance().getControllers().get(this.getIdentifiers().getControllerIdentifier().convert());
		if (myController != null && myController.isInitialized()) {
			String actualValue = myController.getValue(this.getIdentifiers());
			if (this.operand.equals("EQ")) {
				if (this.targetValue.equals(actualValue)) {
					trigger = true;
				}
			} else if (this.operand.equals("LE")) {
				if (Float.parseFloat(actualValue) <= Float.parseFloat(this.targetValue)) {
					trigger = true;
				}
			} else if (this.operand.equals("L")) {
				if (Float.parseFloat(actualValue) < Float.parseFloat(this.targetValue)) {
					trigger = true;
				}
			} else if (this.operand.equals("GE")) {
				if (Float.parseFloat(actualValue) >= Float.parseFloat(this.targetValue)) {
					trigger = true;
				}
			} else if (this.operand.equals("G")) {
				if (Float.parseFloat(actualValue) > Float.parseFloat(this.targetValue)) {
					trigger = true;
				}
			}
		}
		return trigger;
	}

	@Override
	public void trigger() {
		switch (this.isOrBecomes) {
		case BECOMES:
			if (this.evaluate()) {
				this.parentRule.evaluate(this);
			}
			break;
		case IS:
			// this.parentRule.evaluate(this);
			break;
		}
	}
}
