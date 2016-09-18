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
package net.yourhome.server.base.rules.scenes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.yourhome.common.base.enums.ValueTypes;
import net.yourhome.server.base.GeneralController;
import net.yourhome.server.base.rules.scenes.actions.Action;
import net.yourhome.server.base.rules.scenes.actions.NotificationAction;
import net.yourhome.server.base.rules.scenes.actions.ValueAction;

public class Scene {

	private JSONObject sourceJsonObject;
	private Scene me = this;
	private int id;
	private String name;
	private List<Action> actions;

	private static Logger log = Logger.getLogger(Scene.class);
	private Map<Thread, Boolean> activeActions = new ConcurrentHashMap<Thread, Boolean>();

	public Scene(JSONObject jsonObject) throws JSONException {
		this.sourceJsonObject = jsonObject;

		JSONObject descriptionObject = jsonObject.getJSONObject("description");
		this.name = descriptionObject.getString("name");
		JSONArray actionsArray = jsonObject.getJSONArray("actions");
		this.actions = new ArrayList<Action>();
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
				this.actions.add(new Action(this, actionObject));
				break;
			case DIMMER:
			case HEATING:
			case SWITCH_BINARY:
			case SYSTEM_COMMAND:
			case WAIT:
			case COLOR_BULB:
				this.actions.add(new ValueAction(this, actionObject));
				break;
			case SEND_NOTIFICATION:
				this.actions.add(new NotificationAction(this, actionObject));
				break;
			default:
				break;
			}
		}
	}

	public JSONObject getSourceJsonObject() {
		return this.sourceJsonObject;
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		try {
			this.sourceJsonObject.put("id", id);
		} catch (JSONException e) {
			Scene.log.error("Exception occured: ", e);
		}

		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return the actions
	 */
	public List<Action> getActions() {
		return this.actions;
	}

	/**
	 * @return the activeActions
	 */
	public Map<Thread, Boolean> getActiveActions() {
		return this.activeActions;
	}

	public boolean activate() {

		// Do not activate when demo mode is activated
		/*
		 * Boolean isDemoEnabled =
		 * SettingsManager.getStringValue(ControllerTypes.DEMO.convert(),
		 * DemoController.Settings.DEMO_MODE.get(), "false").equals("true");
		 * if(!isDemoEnabled) {
		 */
		// Cancel all active threads
		for (Map.Entry<Thread, Boolean> entry : this.activeActions.entrySet()) {
			Scene.log.info("[" + Thread.currentThread().getId() + "] Cancelling active action " + entry.getKey().getId());
			entry.setValue(false);
		}

		// Start new synchronious action processor
		final Thread actionThread = new Thread(new Runnable() {
			@Override
			public void run() {
				Scene.log.info("[" + Thread.currentThread().getId() + "] Processing " + Scene.this.actions.size() + " actions from scene " + Scene.this.id + ". " + Scene.this.name);
				for (Action action : Scene.this.actions) {
					Boolean continueProcessing = Scene.this.activeActions.get(Thread.currentThread());
					if (continueProcessing) {
						Scene.log.info("[" + Thread.currentThread().getId() + "] " + action.toString());
						action.perform();
						GeneralController.getInstance().triggerSceneActivated(Scene.this.me);
					}
				}
				Scene.this.activeActions.remove(Thread.currentThread());
			}
		});
		this.activeActions.put(actionThread, true);
		actionThread.start();
		// }
		return true;
	}
}
