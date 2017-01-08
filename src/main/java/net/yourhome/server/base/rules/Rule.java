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
package net.yourhome.server.base.rules;

import net.yourhome.common.base.enums.EnumConverter;
import net.yourhome.common.base.enums.ReverseEnumMap;
import net.yourhome.server.base.rules.scenes.Scene;
import net.yourhome.server.base.rules.triggers.ITrigger;
import net.yourhome.server.base.rules.triggers.TriggerGroup;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;

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
			return this.value;
		}

		public static LimitationPeriods convert(String val) {
			return LimitationPeriods.map.get(val);
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
		this.sourceJsonObject = jsonObject;

		// Triggers
		this.trigger = new TriggerGroup(this, jsonObject.getJSONObject("triggers").getJSONObject("details"));

		// Actions
		this.action = new Scene(jsonObject);

		// Description
		JSONObject descriptionObject = jsonObject.getJSONObject("description");
		this.name = descriptionObject.getString("name");
		this.limited = descriptionObject.getBoolean("isLimited");
		if (this.limited) {
			this.limitationPeriod = LimitationPeriods.convert(descriptionObject.get("period").toString());
		}
	}

	public void setTriggers() {
		this.trigger.setTrigger();
	}

	public void unsetTriggers() {
		this.trigger.unsetTrigger();
	}

	public void run() {
		boolean run = true;
		if (this.limited && this.lastRuntime != null) {
			// Check if the rule can be executed
			Date now = new Date();
			long difference = (now.getTime() - this.lastRuntime.getTime()) / 1000;
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
				nowCal.setTime(this.lastRuntime);
				lastRuntimeCal.setTime(now);
				boolean isOnSameDay = nowCal.get(Calendar.YEAR) == lastRuntimeCal.get(Calendar.YEAR) && nowCal.get(Calendar.DAY_OF_YEAR) == lastRuntimeCal.get(Calendar.DAY_OF_YEAR);
				run = !isOnSameDay;
				break;
			default:
				break;

			}
		}
		if (run) {
			this.lastRuntime = new Date();
			this.action.activate();
		} else {
			Rule.log.debug("[Rule] Skipped executing rule " + this.id + "." + this.name + " because of the time limitation. (last executed on " + this.lastRuntime + ")");
		}
	}

	public boolean evaluate(ITrigger startingTrigger) {
		if (this.trigger.evaluate(startingTrigger)) {
			this.run();
			return true;
		}
		return false;
	}

	public JSONObject getSourceJsonObject() {
		return this.sourceJsonObject;
	}

	/**
	 * @return the action
	 */
	public Scene getAction() {
		return this.action;
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
	 * @return the id
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(int id) {
		this.id = id;
		try {
			this.sourceJsonObject.put("id", id);
		} catch (JSONException e) {
			Rule.log.error("Exception occured: ", e);
		}
	}

	/**
	 * @return the active
	 */
	public boolean isActive() {
		return this.active;
	}

	/**
	 * @param active
	 *            the active to set
	 */
	public void setActive(boolean active) {
		this.active = active;
		try {
			this.sourceJsonObject.put("active", active);
		} catch (JSONException e) {
			Rule.log.error("Exception occured: ", e);
		}
	}

	public boolean getActive() {
		return this.active;
	}

}
