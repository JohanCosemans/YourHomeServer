package net.yourhome.server.base.rules.triggers;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import net.yourhome.common.base.enums.ValueTypes;
import net.yourhome.common.net.model.binding.ControlIdentifiers;
import net.yourhome.server.IController;
import net.yourhome.server.base.DatabaseConnector;
import net.yourhome.server.base.rules.Rule;
import net.yourhome.server.net.Server;

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
		identifiers = new ControlIdentifiers(triggerObject);
	}

	@Override
	public JSONObject serialize() throws JSONException {
		JSONObject jsonObject = identifiers.serialize();
		jsonObject.put("id", id);
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
		return type;
	}

	@Override
	public boolean evaluate(ITrigger startingTrigger) {
		if (startingTrigger == this) {
			return true;
		}
		return false;
	}

	public ControlIdentifiers getIdentifiers() {
		return this.identifiers;
	}

	public void trigger() {
		this.parentRule.evaluate(this);
	}

}
