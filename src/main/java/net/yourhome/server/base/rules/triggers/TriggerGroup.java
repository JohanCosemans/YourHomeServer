package net.yourhome.server.base.rules.triggers;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.yourhome.common.base.enums.ValueTypes;
import net.yourhome.common.net.model.binding.ControlIdentifiers;
import net.yourhome.server.base.DatabaseConnector;
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

		condition = Conditions.valueOf(triggerGroup.getString("condition"));
		JSONArray triggersArray = triggerGroup.getJSONArray("items");
		for (int i = 0; i < triggersArray.length(); i++) {
			JSONObject triggerObject = triggersArray.getJSONObject(i);
			String type = triggerObject.getString("type");
			if (type.equals("group")) {
				triggers.add(new TriggerGroup(parentRule, triggerObject.getJSONObject("details")));
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
					triggers.add(trigger);
				}
			}
		}
	}

	public JSONObject serialize() throws JSONException {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("condition", condition);

		JSONArray triggersArray = new JSONArray();

		for (ITrigger trigger : getTriggers()) {
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
		return condition;
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
		return triggers;
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
		switch (getCondition()) {
		case AND:
			int i = 0;
			boolean allInFavor = true;
			while (allInFavor && i < triggers.size()) {
				if (!triggers.get(i++).evaluate(startingTrigger)) {
					allInFavor = false;
				}
			}
			return allInFavor;
		case OR:
			int j = 0;
			boolean oneInFavor = false;
			while (!oneInFavor && j < triggers.size()) {
				if (triggers.get(j++).evaluate(startingTrigger)) {
					oneInFavor = true;
				}
			}
			return oneInFavor;
		}
		return false;
	}
}
