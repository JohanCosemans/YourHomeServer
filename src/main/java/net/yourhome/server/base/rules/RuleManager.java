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
package net.yourhome.server.base.rules;

import net.yourhome.server.base.DatabaseConnector;
import net.yourhome.server.base.rules.scenes.SceneManager;
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

public class RuleManager {
	private static DatabaseConnector db = DatabaseConnector.getInstance();
	private static Logger log = Logger.getLogger(SceneManager.class);
	private static Map<Integer, Rule> allRulesMap = null;

	public static void init() {
		try {
			RuleManager.getAllRules();
			for (Rule r : RuleManager.allRulesMap.values()) {
				r.setTriggers();
			}
		} catch (SQLException e) {
			RuleManager.log.error("Exception occured: ", e);
		}
	}

	public static void destroy() {
		for (Rule r : RuleManager.allRulesMap.values()) {
			r.unsetTriggers();
		}
		RuleManager.allRulesMap.clear();
	}

	public static List<Rule> getAllRules() throws SQLException {
		if (RuleManager.allRulesMap == null) {
			RuleManager.allRulesMap = new ConcurrentHashMap<Integer, Rule>();

			ResultSet ruleResult = RuleManager.db.executePreparedStatement(RuleManager.db.prepareStatement("select * from Rules"));
			while (ruleResult.next()) {
				try {
					Rule resultRule = new Rule(new JSONObject(ruleResult.getString("json")));
					resultRule.setId(ruleResult.getInt("id"));
					resultRule.setActive(ruleResult.getBoolean("active"));
					RuleManager.allRulesMap.put(resultRule.getId(), resultRule);
				} catch (JSONException | SQLException e) {
					RuleManager.log.error("[RuleManager] Failed to instantiate rule " + ruleResult.getInt("id"), e);
				}
			}
			ruleResult.close();
		}
		return new ArrayList<Rule>(RuleManager.allRulesMap.values());
	}

	public static Rule getRule(int ruleId) throws SQLException, JSONException {
		Rule resultRule = RuleManager.allRulesMap.get(ruleId);
		if (resultRule == null) {
			ResultSet ruleResult = RuleManager.db.executeSelect("select * from Rules where id = " + ruleId);
			if (ruleResult.next()) {
				resultRule = new Rule(new JSONObject(ruleResult.getString("json")));
				resultRule.setActive(ruleResult.getBoolean("active"));
				resultRule.setId(ruleId);
				RuleManager.allRulesMap.put(resultRule.getId(), resultRule);
			}
		}
		return resultRule;

	}

	public static int save(Rule rule) throws SQLException {
		if (rule.getId() != 0) {
			// Update Scene details
			PreparedStatement preparedStatement = RuleManager.db.prepareStatement("update Rules SET name=?, json=? where id=?");
			preparedStatement.setString(1, rule.getName());
			preparedStatement.setString(2, rule.getSourceJsonObject().toString());
			preparedStatement.setInt(3, rule.getId());

			RuleManager.db.executePreparedUpdate(preparedStatement);

			try {
				Rule originalRule = RuleManager.getRule(rule.getId());
				originalRule.unsetTriggers();
			} catch (JSONException e) {
			}
			rule.unsetTriggers();
			RuleManager.allRulesMap.remove(rule.getId());

		} else {

			// Rules
			JSONObject ruleJson = rule.getSourceJsonObject();
			PreparedStatement preparedStatement = RuleManager.db.prepareStatement("insert into Rules ('name', 'json', 'active') VALUES (?,?,?)");
			preparedStatement.setString(1, rule.getName());
			preparedStatement.setString(2, ruleJson.toString().toString());
			preparedStatement.setBoolean(3, true);
			rule.setId(RuleManager.db.executePreparedUpdate(preparedStatement));

			/*
			 * scene.setId(db.insertConfigurationValue(
			 * "insert into Scenes ('name', 'json') " + "VALUES ('" +
			 * scene.getName() + "', '"+sceneJson.toString()+"')"));
			 */
		}

		RuleManager.allRulesMap.put(rule.getId(), rule);
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
		RuleManager.db.executeQuery("DELETE FROM Rules WHERE id = '" + rule.getId() + "'");
		rule.unsetTriggers();
		RuleManager.allRulesMap.remove(rule.getId());
	}

	public static void setActive(Rule rule, boolean active) throws SQLException {
		PreparedStatement preparedStatement = RuleManager.db.prepareStatement("update Rules SET active=? where id=?");
		preparedStatement.setBoolean(1, active);
		preparedStatement.setInt(2, rule.getId());
		RuleManager.db.executePreparedUpdate(preparedStatement);

		if (!rule.isActive() && active) {
			rule.setTriggers();
		} else if (rule.isActive() && !active) {
			rule.unsetTriggers();
		}

		rule.setActive(active);

		// Update cache
		RuleManager.allRulesMap.remove(rule.getId());
		RuleManager.allRulesMap.put(rule.getId(), rule);
	}
}
