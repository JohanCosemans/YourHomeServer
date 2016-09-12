package net.yourhome.server.base.rules;

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
import net.yourhome.server.base.rules.scenes.SceneManager;

public class RuleManager {
	private static DatabaseConnector db = DatabaseConnector.getInstance();
	private static Logger log = Logger.getLogger(SceneManager.class);
	private static Map<Integer, Rule> allRulesMap = null;

	public static void init() {
		try {
			getAllRules();
			for (Rule r : allRulesMap.values()) {
				r.setTriggers();
			}
		} catch (SQLException e) {
			log.error("Exception occured: ", e);
		}
	}

	public static void destroy() {
		for (Rule r : allRulesMap.values()) {
			r.unsetTriggers();
		}
		allRulesMap.clear();
	}

	public static List<Rule> getAllRules() throws SQLException {
		if (allRulesMap == null) {
			allRulesMap = new ConcurrentHashMap<Integer, Rule>();

			ResultSet ruleResult = db.executePreparedStatement(db.prepareStatement("select * from Rules"));
			while (ruleResult.next()) {
				try {
					Rule resultRule = new Rule(new JSONObject(ruleResult.getString("json")));
					resultRule.setId(ruleResult.getInt("id"));
					resultRule.setActive(ruleResult.getBoolean("active"));
					allRulesMap.put(resultRule.getId(), resultRule);
				} catch (JSONException | SQLException e) {
					log.error("[RuleManager] Failed to instantiate rule " + ruleResult.getInt("id"), e);
				}
			}
			ruleResult.close();
		}
		return new ArrayList<Rule>(allRulesMap.values());
	}

	public static Rule getRule(int ruleId) throws SQLException, JSONException {
		Rule resultRule = allRulesMap.get(ruleId);
		if (resultRule == null) {
			ResultSet ruleResult = db.executeSelect("select * from Rules where id = " + ruleId);
			if (ruleResult.next()) {
				resultRule = new Rule(new JSONObject(ruleResult.getString("json")));
				resultRule.setActive(ruleResult.getBoolean("active"));
				resultRule.setId(ruleId);
				allRulesMap.put(resultRule.getId(), resultRule);
			}
		}
		return resultRule;

	}

	public static int save(Rule rule) throws SQLException {
		if (rule.getId() != 0) {
			// Update Scene details
			PreparedStatement preparedStatement = db.prepareStatement("update Rules SET name=?, json=? where id=?");
			preparedStatement.setString(1, rule.getName());
			preparedStatement.setString(2, rule.getSourceJsonObject().toString());
			preparedStatement.setInt(3, rule.getId());

			db.executePreparedUpdate(preparedStatement);

			try {
				Rule originalRule = getRule(rule.getId());
				originalRule.unsetTriggers();
			} catch (JSONException e) {
			}
			rule.unsetTriggers();
			allRulesMap.remove(rule.getId());

		} else {

			// Rules
			JSONObject ruleJson = rule.getSourceJsonObject();
			PreparedStatement preparedStatement = db.prepareStatement("insert into Rules ('name', 'json', 'active') VALUES (?,?,?)");
			preparedStatement.setString(1, rule.getName());
			preparedStatement.setString(2, ruleJson.toString().toString());
			preparedStatement.setBoolean(3, true);
			rule.setId(db.executePreparedUpdate(preparedStatement));

			/*
			 * scene.setId(db.insertConfigurationValue(
			 * "insert into Scenes ('name', 'json') " + "VALUES ('" +
			 * scene.getName() + "', '"+sceneJson.toString()+"')"));
			 */
		}

		allRulesMap.put(rule.getId(), rule);
		rule.setTriggers();
		/*
		 * if(rule.getId() != 0) { // Update rule details db.executeQuery(
		 * "update Rules SET name='"+rule.getName()+"', json='"
		 * +rule.getSourceJsonObject().toString()+"' WHERE id = '"
		 * +rule.getId()+"'");
		 * 
		 * // Reschedule all triggers! }else {
		 * 
		 * // Rules JSONObject ruleJson = rule.getSourceJsonObject();
		 * rule.setId(db.insertConfigurationValue(
		 * "insert into Rules ('name', 'json') " + "VALUES ('" + rule.getName()
		 * + "', '"+ruleJson.toString()+"')")); }
		 */

		return rule.getId();
	}

	public static void delete(Rule rule) throws SQLException {
		// TODO: Deschedule all listeners !

		// Delete db & cache
		db.executeQuery("DELETE FROM Rules WHERE id = '" + rule.getId() + "'");
		rule.unsetTriggers();
		allRulesMap.remove(rule.getId());
	}

	public static void setActive(Rule rule, boolean active) throws SQLException {
		PreparedStatement preparedStatement = db.prepareStatement("update Rules SET active=? where id=?");
		preparedStatement.setBoolean(1, active);
		preparedStatement.setInt(2, rule.getId());
		db.executePreparedUpdate(preparedStatement);

		if (!rule.isActive() && active) {
			rule.setTriggers();
		} else if (rule.isActive() && !active) {
			rule.unsetTriggers();
		}

		rule.setActive(active);

		// Update cache
		allRulesMap.remove(rule.getId());
		allRulesMap.put(rule.getId(), rule);
	}
}
