package net.yourhome.server.base.rules;

import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import net.yourhome.common.base.enums.EnumConverter;
import net.yourhome.common.base.enums.ReverseEnumMap;
import net.yourhome.server.base.rules.scenes.Scene;
import net.yourhome.server.base.rules.triggers.ITrigger;
import net.yourhome.server.base.rules.triggers.TriggerGroup;

public class Rule {
	public enum LimitationPeriods implements EnumConverter<String, LimitationPeriods> {
		NO_LIMIT(""), TEN_SECONDS("ten_seconds"), FIVE_MINUTES("five_minutes"), ONE_DAY("one_day");

		private final String value;

		LimitationPeriods(String value) {
			this.value = value;
		}

		/* Reverse enum methods */
		private static ReverseEnumMap<String, LimitationPeriods> map = new ReverseEnumMap<String, LimitationPeriods>(LimitationPeriods.class);

		@Override
		public String convert() {
			return value;
		}

		public static LimitationPeriods convert(String val) {
			return map.get(val);
		}

		public String getValue() {
			return this.value;
		}

	}

	private JSONObject sourceJsonObject;
	private TriggerGroup trigger;
	private Scene action;
	private String name;
	private int id;
	private boolean active = true;

	protected static Logger log = Logger.getLogger(Rule.class);

	private boolean limited;
	private LimitationPeriods limitationPeriod;
	private Date lastRuntime;

	public Rule(JSONObject jsonObject) throws JSONException {
		sourceJsonObject = jsonObject;

		// Triggers
		trigger = new TriggerGroup(this, jsonObject.getJSONObject("triggers").getJSONObject("details"));

		// Actions
		action = new Scene(jsonObject);

		// Description
		JSONObject descriptionObject = jsonObject.getJSONObject("description");
		name = descriptionObject.getString("name");
		limited = descriptionObject.getBoolean("isLimited");
		if (limited) {
			limitationPeriod = LimitationPeriods.convert(descriptionObject.get("period").toString());
		}
	}

	public void setTriggers() {
		trigger.setTrigger();
	}

	public void unsetTriggers() {
		trigger.unsetTrigger();
	}

	public void run() {
		boolean run = true;
		if (limited && lastRuntime != null) {
			// Check if the rule can be executed
			Date now = new Date();
			long difference = (now.getTime() - lastRuntime.getTime()) / 1000;
			switch (this.limitationPeriod) {
			case TEN_SECONDS:
				if (difference < 10L) {
					run = false;
				}
				break;
			case FIVE_MINUTES:
				if (difference < 5 * 60L) {
					run = false;
				}
				break;
			case ONE_DAY:
				Calendar nowCal = Calendar.getInstance();
				Calendar lastRuntimeCal = Calendar.getInstance();
				nowCal.setTime(lastRuntime);
				lastRuntimeCal.setTime(now);
				boolean isOnSameDay = nowCal.get(Calendar.YEAR) == lastRuntimeCal.get(Calendar.YEAR) && nowCal.get(Calendar.DAY_OF_YEAR) == lastRuntimeCal.get(Calendar.DAY_OF_YEAR);
				run = !isOnSameDay;
				break;
			default:
				break;

			}
		}
		if (run) {
			lastRuntime = new Date();
			this.action.activate();
		} else {
			log.debug("[Rule] Skipped executing rule " + this.id + "." + this.name + " because of the time limitation. (last executed on " + lastRuntime + ")");
		}
	}

	public boolean evaluate(ITrigger startingTrigger) {
		if (trigger.evaluate(startingTrigger)) {
			run();
			return true;
		}
		return false;
	}

	public JSONObject getSourceJsonObject() {
		return sourceJsonObject;
	}

	/**
	 * @return the action
	 */
	public Scene getAction() {
		return action;
	}

	/**
	 * @param action
	 *            the action to set
	 */
	public void setAction(Scene action) {
		this.action = action;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(int id) {
		this.id = id;
		try {
			sourceJsonObject.put("id", id);
		} catch (JSONException e) {
			log.error("Exception occured: ", e);
		}
	}

	/**
	 * @return the active
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * @param active
	 *            the active to set
	 */
	public void setActive(boolean active) {
		this.active = active;
		try {
			sourceJsonObject.put("active", active);
		} catch (JSONException e) {
			log.error("Exception occured: ", e);
		}
	}

	public boolean getActive() {
		return this.active;
	}

}
