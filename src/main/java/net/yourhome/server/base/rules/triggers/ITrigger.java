package net.yourhome.server.base.rules.triggers;

import org.json.JSONException;
import org.json.JSONObject;

import net.yourhome.common.net.model.binding.ControlIdentifiers;

public interface ITrigger {

	public JSONObject serialize() throws JSONException;

	public void setTrigger();

	public void unsetTrigger();

	public TriggerTypes getType();

	public ControlIdentifiers getIdentifiers();

	public boolean evaluate(ITrigger startingTrigger);
}
