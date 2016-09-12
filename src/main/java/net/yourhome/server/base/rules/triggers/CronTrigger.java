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
package net.yourhome.server.base.rules.triggers;

import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import net.yourhome.server.base.Scheduler;
import net.yourhome.server.base.rules.Rule;

public class CronTrigger extends Trigger {
	private Trigger me = this;
	private String cronString;
	private String cronScheduleId;

	public CronTrigger(Rule parentRule, JSONObject triggerObject) throws JSONException {
		super(parentRule, triggerObject);
		this.type = TriggerTypes.CRON;
		JSONObject details = triggerObject.getJSONObject("details");
		this.cronString = details.getString("value");
	}

	@Override
	public void setTrigger() {
		if (this.cronScheduleId == null) {
			this.cronScheduleId = Scheduler.getInstance().scheduleCron(new TimerTask() {
				@Override
				public void run() {
					Trigger.log.debug("[CronTrigger] Cron triggered! cron: " + CronTrigger.this.cronString + ", Rule: " + CronTrigger.this.parentRule.getId() + ". " + CronTrigger.this.parentRule.getName());
					CronTrigger.this.parentRule.evaluate(CronTrigger.this.me);
				}
			}, this.cronString);
		}
	}

	@Override
	public void unsetTrigger() {
		if (this.cronScheduleId != null) {
			Scheduler.getInstance().descheduleCron(this.cronScheduleId);
			this.cronScheduleId = null;
		}
	}
}
