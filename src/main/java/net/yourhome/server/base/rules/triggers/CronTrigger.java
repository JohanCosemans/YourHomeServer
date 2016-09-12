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
		type = TriggerTypes.CRON;
		JSONObject details = triggerObject.getJSONObject("details");
		this.cronString = details.getString("value");
	}

	@Override
	public void setTrigger() {
		if (cronScheduleId == null) {
			cronScheduleId = Scheduler.getInstance().scheduleCron(new TimerTask() {
				@Override
				public void run() {
					log.debug("[CronTrigger] Cron triggered! cron: " + cronString + ", Rule: " + parentRule.getId() + ". " + parentRule.getName());
					parentRule.evaluate(me);
				}
			}, cronString);
		}
	}

	@Override
	public void unsetTrigger() {
		if (cronScheduleId != null) {
			Scheduler.getInstance().descheduleCron(cronScheduleId);
			cronScheduleId = null;
		}
	}
}
