package net.yourhome.server.base.rules.scenes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.yourhome.common.base.enums.ControllerTypes;
import net.yourhome.common.base.enums.ValueTypes;
import net.yourhome.server.base.GeneralController;
import net.yourhome.server.base.SettingsManager;
import net.yourhome.server.base.rules.scenes.actions.Action;
import net.yourhome.server.base.rules.scenes.actions.NotificationAction;
import net.yourhome.server.base.rules.scenes.actions.ValueAction;
import net.yourhome.server.demo.DemoController;

public class Scene {

	private JSONObject sourceJsonObject;
	private Scene me = this;
	private int id;
	private String name;
	private List<Action> actions;

	private static Logger log = Logger.getLogger(Scene.class);
	private Map<Thread, Boolean> activeActions = new ConcurrentHashMap<Thread, Boolean>();

	public Scene(JSONObject jsonObject) throws JSONException {
		sourceJsonObject = jsonObject;

		JSONObject descriptionObject = jsonObject.getJSONObject("description");
		name = descriptionObject.getString("name");
		JSONArray actionsArray = jsonObject.getJSONArray("actions");
		actions = new ArrayList<Action>();
		for (int i = 0; i < actionsArray.length(); i++) {
			JSONObject actionObject = actionsArray.getJSONObject(i);
			ValueTypes actionValueType = ValueTypes.convert(actionObject.getString("valueType"));
			switch (actionValueType) {
			case GENERAL_COMMAND:
			case HTTP_COMMAND:
			case MUSIC_ACTION:
			case RADIO_STATION:
			case SCENE_ACTIVATION:
			case SOUND_NOTIFICATION:
				actions.add(new Action(this, actionObject));
				break;
			case DIMMER:
			case HEATING:
			case SWITCH_BINARY:
			case SYSTEM_COMMAND:
			case WAIT:
			case COLOR_BULB:
				actions.add(new ValueAction(this, actionObject));
				break;
			case SEND_NOTIFICATION:
				actions.add(new NotificationAction(this, actionObject));
				break;
			default:
				break;
			}
		}
	}

	public JSONObject getSourceJsonObject() {
		return sourceJsonObject;
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		try {
			sourceJsonObject.put("id", id);
		} catch (JSONException e) {
			log.error("Exception occured: ", e);
		}

		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the actions
	 */
	public List<Action> getActions() {
		return actions;
	}

	/**
	 * @return the activeActions
	 */
	public Map<Thread, Boolean> getActiveActions() {
		return activeActions;
	}

	public boolean activate() {
		
		// Do not activate when demo mode is activated
		/*Boolean isDemoEnabled = SettingsManager.getStringValue(ControllerTypes.DEMO.convert(), DemoController.Settings.DEMO_MODE.get(), "false").equals("true");
		if(!isDemoEnabled) {*/
			// Cancel all active threads
			for (Map.Entry<Thread, Boolean> entry : activeActions.entrySet()) {
				log.info("[" + Thread.currentThread().getId() + "] Cancelling active action " + entry.getKey().getId());
				entry.setValue(false);
			}
	
			// Start new synchronious action processor
			final Thread actionThread = new Thread(new Runnable() {
				public void run() {
					log.info("[" + Thread.currentThread().getId() + "] Processing " + actions.size() + " actions from scene " + id + ". " + name);
					for (Action action : actions) {
						Boolean continueProcessing = activeActions.get(Thread.currentThread());
						if (continueProcessing) {
							log.info("[" + Thread.currentThread().getId() + "] " + action.toString());
							action.perform();
							GeneralController.getInstance().triggerSceneActivated(me);
						}
					}
					activeActions.remove(Thread.currentThread());
				}
			});
			activeActions.put(actionThread, true);
			actionThread.start();
		//}
		return true;
	}
}
