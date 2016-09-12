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
			return evaluate();
		}

		return evaluate();
	}

	private boolean evaluate() {
		boolean trigger = false;
		// Go get the actual value from the controller
		IController myController = Server.getInstance().getControllers().get(this.getIdentifiers().getControllerIdentifier().convert());
		if (myController != null && myController.isInitialized()) {
			String actualValue = myController.getValue(getIdentifiers());
			if (this.operand.equals("EQ")) {
				if (this.targetValue.equals(actualValue))
					trigger = true;
			} else if (this.operand.equals("LE")) {
				if (Float.parseFloat(actualValue) <= Float.parseFloat(this.targetValue))
					trigger = true;
			} else if (this.operand.equals("L")) {
				if (Float.parseFloat(actualValue) < Float.parseFloat(this.targetValue))
					trigger = true;
			} else if (this.operand.equals("GE")) {
				if (Float.parseFloat(actualValue) >= Float.parseFloat(this.targetValue))
					trigger = true;
			} else if (this.operand.equals("G")) {
				if (Float.parseFloat(actualValue) > Float.parseFloat(this.targetValue))
					trigger = true;
			}
		}
		return trigger;
	}

	public void trigger() {
		switch (this.isOrBecomes) {
		case BECOMES:
			if (evaluate()) {
				this.parentRule.evaluate(this);
			}
			break;
		case IS:
			// this.parentRule.evaluate(this);
			break;
		}
	}
}
