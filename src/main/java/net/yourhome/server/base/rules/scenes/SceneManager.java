package net.yourhome.server.base.rules.scenes;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import net.yourhome.server.base.DatabaseConnector;

public class SceneManager {
	private static DatabaseConnector db = DatabaseConnector.getInstance();
	private static Logger log = Logger.getLogger(SceneManager.class);
	private static Map<Integer, Scene> allScenesMap = null;

	static {
		init();
	}

	public static void init() {
		try {
			getAllScenes();
		} catch (SQLException e) {
			log.error("Exception occured: ", e);
		}
	}

	public static void destroy() {
		allScenesMap.clear();
	}

	public static int save(Scene scene) throws SQLException {
		if (scene.getId() != 0) {
			// Update Scene details
			PreparedStatement preparedStatement = db.prepareStatement("update Scenes SET name=?, json=? where id=?");
			preparedStatement.setString(1, scene.getName());
			preparedStatement.setString(2, scene.getSourceJsonObject().toString());
			preparedStatement.setInt(3, scene.getId());

			db.executePreparedUpdate(preparedStatement);
			allScenesMap.remove(scene.getId());
		} else {

			// Scenes
			JSONObject sceneJson = scene.getSourceJsonObject();
			PreparedStatement preparedStatement = db.prepareStatement("insert into Scenes ('name', 'json') VALUES (?,?)");
			preparedStatement.setString(1, scene.getName());
			preparedStatement.setString(2, sceneJson.toString().toString());

			scene.setId(db.executePreparedUpdate(preparedStatement));
		}

		allScenesMap.put(scene.getId(), scene);
		return scene.getId();
	}

	public static void delete(Scene scene) throws SQLException {
		// Delete db
		db.executeQuery("DELETE FROM Scenes WHERE id = '" + scene.getId() + "'");
		allScenesMap.remove(scene.getId());
	}

	public static List<Scene> getAllScenes() throws SQLException {
		if (allScenesMap == null) {
			allScenesMap = new ConcurrentHashMap<Integer, Scene>();

			ResultSet sceneResult = db.executeSelect("select * from Scenes");
			while (sceneResult.next()) {
				try {
					Scene scene = new Scene(new JSONObject(sceneResult.getString("json")));
					scene.setId(sceneResult.getInt("id"));
					allScenesMap.put(scene.getId(), scene);

				} catch (JSONException | SQLException e) {
					log.error("Exception occured: ", e);
				}
			}
			sceneResult.close();
		}
		return new ArrayList<Scene>(allScenesMap.values());
	}

	public static Scene getScene(int sceneId) throws SQLException, JSONException {

		Scene resultScene = allScenesMap.get(sceneId);
		if (resultScene == null) {
			ResultSet sceneResult = db.executeSelect("select * from Scenes where id = " + sceneId);
			if (sceneResult.next()) {
				resultScene = new Scene(new JSONObject(sceneResult.getString("json")));
				resultScene.setId(sceneId);
				allScenesMap.put(resultScene.getId(), resultScene);
			}
		}
		return resultScene;
	}
}
