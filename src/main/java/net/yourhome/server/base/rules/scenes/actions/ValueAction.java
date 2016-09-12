package net.yourhome.server.base.rules.scenes.actions;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONException;
import org.json.JSONObject;

import net.yourhome.common.net.messagestructures.general.SetValueMessage;
import net.yourhome.server.base.rules.scenes.Scene;
import net.yourhome.server.net.Server;

public class ValueAction extends Action {

	private int valueActionId;
	private String value;

	public ValueAction(Scene parentScene, JSONObject actionObject) throws JSONException {
		super(parentScene, actionObject);
		JSONObject detailsObject = actionObject.getJSONObject("details");
		value = detailsObject.get("value").toString();
	}

	public ValueAction(Scene parentScene, ResultSet actionObject) throws SQLException {
		super(parentScene, actionObject);
		value = actionObject.getString("value");
		valueActionId = actionObject.getInt("valueActionId");
	}

	public boolean perform() {
		SetValueMessage setValue = new SetValueMessage();
		setValue.broadcast = false;
		setValue.controlIdentifiers = this.identifiers;
		setValue.value = value;
		Server.getInstance().processMessage(setValue);
		return true;
	}

	/**
	 * @return the valueActionId
	 */
	public int getValueActionId() {
		return valueActionId;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

}
