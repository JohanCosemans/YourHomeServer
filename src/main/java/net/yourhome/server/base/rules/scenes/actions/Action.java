package net.yourhome.server.base.rules.scenes.actions;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import net.yourhome.common.base.enums.ValueTypes;
import net.yourhome.common.net.messagestructures.general.ActivationMessage;
import net.yourhome.common.net.model.binding.ControlIdentifiers;
import net.yourhome.server.base.DatabaseConnector;
import net.yourhome.server.base.rules.scenes.Scene;
import net.yourhome.server.net.Server;

public class Action {
	protected Scene parentScene;

	protected int id;
	protected ControlIdentifiers identifiers;
	protected int sequence;
	protected ValueTypes valueType;
	protected static Logger log = Logger.getLogger(Scene.class);

	protected DatabaseConnector db = DatabaseConnector.getInstance();

	public Action(Scene parentScene, JSONObject actionObject) throws JSONException {
		this.parentScene = parentScene;
		try {
			id = actionObject.getInt("id");
		} catch (JSONException e) {
		}
		identifiers = new ControlIdentifiers(actionObject);
		valueType = ValueTypes.convert(actionObject.getString("valueType"));
	}

	public Action(Scene parentScene, ResultSet actionObject) throws SQLException {
		this.parentScene = parentScene;
		id = actionObject.getInt("id");
		identifiers = new ControlIdentifiers(actionObject);
		sequence = actionObject.getInt("sequence");
		valueType = ValueTypes.convert(actionObject.getString("value_type"));
	}

	public boolean perform() {
		ActivationMessage activation = new ActivationMessage();
		activation.broadcast = false;
		activation.controlIdentifiers = this.identifiers;
		Server.getInstance().processMessage(activation);
		return true;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the identifiers
	 */
	public ControlIdentifiers getIdentifiers() {
		return identifiers;
	}

	/**
	 * @return the sequence
	 */
	public int getSequence() {
		return sequence;
	}

	/**
	 * @param sequence
	 *            the sequence to set
	 */
	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	/**
	 * @return the valueType
	 */
	public ValueTypes getValueType() {
		return valueType;
	}

	public void setParentScene(Scene parentScene) {
		this.parentScene = parentScene;
	}

}
