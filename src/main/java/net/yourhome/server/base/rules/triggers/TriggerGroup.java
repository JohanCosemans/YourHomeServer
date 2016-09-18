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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.yourhome.common.base.enums.ValueTypes;
import net.yourhome.common.net.model.binding.ControlIdentifiers;
import net.yourhome.server.base.rules.Rule;

public class TriggerGroup implements ITrigger {
	public enum Conditions {
		AND, OR
	}

	private Conditions condition;
	private List<ITrigger> triggers = new ArrayList<ITrigger>();
	protected Rule parentRule;
	protected int id;

	public TriggerGroup(Rule parentRule, JSONObject triggerGroup) throws JSONException {
		this.parentRule = parentRule;

		this.condition = Conditions.valueOf(triggerGroup.getString("condition"));
		JSONArray triggersArray = triggerGroup.getJSONArray("items");
		for (int i = 0; i < triggersArray.length(); i++) {
			JSONObject triggerObject = triggersArray.getJSONObject(i);
			String type = triggerObject.getString("type");
			if (type.equals("group")) {
				this.triggers.add(new TriggerGroup(parentRule, triggerObject.getJSONObject("details")));
			} else if (type.equals("trigger")) {
				ValueTypes valueType = ValueTypes.convert(triggerObject.getString("valueType"));
				ITrigger trigger = null;
				// Depending on trigger type!
				switch (valueType) {
				case SCENE_ACTIVATION:
				case EVENT:
					// Trigger
					trigger = new Trigger(parentRule, triggerObject.getJSONObject("details"));
					break;
				case TIME_PERIOD:
					// CronTrigger
					trigger = new CronTrigger(parentRule, triggerObject.getJSONObject("details"));
					break;
				case SWITCH_BINARY:
				case DIMMER:
				case HEATING:
				case METER:
				case SENSOR_TEMPERATURE:
				case SENSOR_HUMIDITY:
				case SENSOR_GENERAL:
				case SENSOR_LUMINOSITY:
				case SENSOR_ALARM:
					// ValueTrigger
					trigger = new ValueTrigger(parentRule, triggerObject.getJSONObject("details"));
					break;
				default:
					break;
				}

				if (trigger != null) {
					this.triggers.add(trigger);
				}
			}
		}
	}

	@Override
	public JSONObject serialize() throws JSONException {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("condition", this.condition);

		JSONArray triggersArray = new JSONArray();

		for (ITrigger trigger : this.getTriggers()) {
			if (trigger instanceof TriggerGroup) {

			} else if (trigger instanceof Trigger) {

			}
			triggersArray.put(trigger.serialize());
		}
		jsonObject.put("items", triggersArray);

		return jsonObject;
	}
	/*
	 * // JSONArray triggersArray = triggerGroup.getJSONArray("items"); for(int
	 * i=0;i<triggersArray.length();i++) { JSONObject triggerObject =
	 * triggersArray.getJSONObject(i); String type =
	 * triggerObject.getString("type"); if(type.equals("group")) {
	 * triggers.add(new
	 * TriggerGroup(parentRule,triggerObject.getJSONObject("details"))); }else
	 * if(type.equals("trigger")) { ValueTypes valueType =
	 * ValueTypes.convert(triggerObject.getString("valueType")); ITrigger
	 * trigger = null; // Depending on trigger type! switch(valueType) { case
	 * EVENT: // Trigger trigger = new
	 * Trigger(parentRule,triggerObject.getJSONObject("details")); break; case
	 * TIME_PERIOD: // CronTrigger trigger = new
	 * CronTrigger(parentRule,triggerObject.getJSONObject("details")); break;
	 * case SWITCH_BINARY: case DIMMER: case HEATING: case METER: case
	 * SENSOR_TEMPERATURE: case SENSOR_HUMIDITY: case SENSOR_LUMINOSITY: //
	 * ValueTrigger trigger = new
	 * ValueTrigger(parentRule,triggerObject.getJSONObject("details")); break;
	 * default: break; }
	 * 
	 * if(trigger != null) { triggers.add(trigger); } } } }
	 */

	/**
	 * @return the condition
	 */
	public Conditions getCondition() {
		return this.condition;
	}

	/**
	 * @param condition
	 *            the condition to set
	 */
	public void setCondition(Conditions condition) {
		this.condition = condition;
	}

	/**
	 * @return the triggers
	 */
	public List<ITrigger> getTriggers() {
		return this.triggers;
	}

	/**
	 * @param triggers
	 *            the triggers to set
	 */
	public void setTriggers(List<ITrigger> triggers) {
		this.triggers = triggers;
	}

	@Override
	public void setTrigger() {
		for (ITrigger trigger : this.getTriggers()) {
			trigger.setTrigger();
		}
	}

	@Override
	public void unsetTrigger() {
		for (ITrigger trigger : this.getTriggers()) {
			trigger.unsetTrigger();
		}
	}

	@Override
	public TriggerTypes getType() {
		return TriggerTypes.GROUP;
	}

	@Override
	public ControlIdentifiers getIdentifiers() {
		return null;
	}

	@Override
	public boolean evaluate(ITrigger startingTrigger) {
		switch (this.getCondition()) {
		case AND:
			int i = 0;
			boolean allInFavor = true;
			while (allInFavor && i < this.triggers.size()) {
				if (!this.triggers.get(i++).evaluate(startingTrigger)) {
					allInFavor = false;
				}
			}
			return allInFavor;
		case OR:
			int j = 0;
			boolean oneInFavor = false;
			while (!oneInFavor && j < this.triggers.size()) {
				if (this.triggers.get(j++).evaluate(startingTrigger)) {
					oneInFavor = true;
				}
			}
			return oneInFavor;
		}
		return false;
	}
}
