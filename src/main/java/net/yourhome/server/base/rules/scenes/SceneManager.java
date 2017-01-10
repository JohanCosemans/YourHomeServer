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

import net.yourhome.server.base.DatabaseConnector;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SceneManager {
	private static DatabaseConnector db = DatabaseConnector.getInstance();
	private static Logger log = Logger.getLogger(SceneManager.class);
	private static Map<Integer, Scene> allScenesMap = null;

	static {
		SceneManager.init();
	}

	public static void init() {
		try {
			SceneManager.getAllScenes();
		} catch (SQLException e) {
			SceneManager.log.error("Exception occured: ", e);
		}
	}

	public static void destroy() {
		SceneManager.allScenesMap.clear();
	}

	public static int save(Scene scene) throws SQLException {
		if (scene.getId() != 0) {
			// Update Scene details
			PreparedStatement preparedStatement = SceneManager.db.prepareStatement("update Scenes SET name=?, json=? where id=?");
			preparedStatement.setString(1, scene.getName());
			preparedStatement.setString(2, scene.getSourceJsonObject().toString());
			preparedStatement.setInt(3, scene.getId());

			SceneManager.db.executePreparedUpdate(preparedStatement);
			SceneManager.allScenesMap.remove(scene.getId());
		} else {

			// Scenes
			JSONObject sceneJson = scene.getSourceJsonObject();
			PreparedStatement preparedStatement = SceneManager.db.prepareStatement("insert into Scenes ('name', 'json') VALUES (?,?)");
			preparedStatement.setString(1, scene.getName());
			preparedStatement.setString(2, sceneJson.toString().toString());

			scene.setId(SceneManager.db.executePreparedUpdate(preparedStatement));
		}

		SceneManager.allScenesMap.put(scene.getId(), scene);
		return scene.getId();
	}

	public static void delete(Scene scene) throws SQLException {
		// Delete db
		SceneManager.db.executeQuery("DELETE FROM Scenes WHERE id = '" + scene.getId() + "'");
		SceneManager.allScenesMap.remove(scene.getId());
	}

	public static List<Scene> getAllScenes() throws SQLException {
		if (SceneManager.allScenesMap == null) {
			SceneManager.allScenesMap = new ConcurrentHashMap<Integer, Scene>();

			ResultSet sceneResult = SceneManager.db.executeSelect("select * from Scenes");
			while (sceneResult.next()) {
				try {
					Scene scene = new Scene(new JSONObject(sceneResult.getString("json")));
					scene.setId(sceneResult.getInt("id"));
					SceneManager.allScenesMap.put(scene.getId(), scene);

				} catch (JSONException | SQLException e) {
					SceneManager.log.error("Exception occured: ", e);
				}
			}
			sceneResult.close();
		}
		return new ArrayList<Scene>(SceneManager.allScenesMap.values());
	}

	public static Scene getScene(int sceneId) throws SQLException, JSONException {

		Scene resultScene = SceneManager.allScenesMap.get(sceneId);
		if (resultScene == null) {
			ResultSet sceneResult = SceneManager.db.executeSelect("select * from Scenes where id = " + sceneId);
			if (sceneResult.next()) {
				resultScene = new Scene(new JSONObject(sceneResult.getString("json")));
				resultScene.setId(sceneId);
				SceneManager.allScenesMap.put(resultScene.getId(), resultScene);
			}
		}
		return resultScene;
	}
	public static List<Scene> getScenesByName(String sceneName) throws SQLException, JSONException {
		List<Scene> sceneMatches = new ArrayList<>();
		log.debug("Looking for scenes that match "+sceneName);
		for(Scene scene : getAllScenes()) {
			if(scene.getName().toLowerCase().contains(sceneName.toLowerCase())) {
				sceneMatches.add(scene);
                log.debug("Match found: "+scene.getName());
			}
		}
        log.info(sceneMatches.size() + " scenes found that match the name "+sceneName);
		return sceneMatches;
	}
}
