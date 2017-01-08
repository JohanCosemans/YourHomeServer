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
package net.yourhome.server.base;

import com.google.common.io.Files;
import net.yourhome.common.net.messagestructures.zwave.ZWaveValue;
import net.yourhome.common.net.model.binding.ControlIdentifiers;
import org.apache.commons.collections.map.LRUMap;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class DatabaseConnector {
	private final String DBPATH = SettingsManager.getBasePath() + "/database/";
	private final String PATH_ARCHIVE = this.DBPATH + "home_history_archive.db";
	private final String PATH_WEEKLY = this.DBPATH + "home_history_weekly.db";
	private final String PATH_DEFAULT = this.DBPATH + "home_history_weekly_default.db";
	private final String INSERT_VALUE_CHANGE = "insert into main.Home_History ('controller_identifier', 'node_identifier', 'value_identifier', 'time', 'unit','value','value_d') VALUES (?,?,?,?,?,?,?)";

	private SimpleDateFormat sqliteTimestampFormat;
	private Connection allHistoryConnection;
	private volatile int currentHistoryBatchSize = 0;
	private final int HISTORY_BATCH_SIZE = 1000;
	private volatile PreparedStatement weeklyHistoryStm = null;

	private Connection weeklylHistoryConnection;
	private volatile int currentWeeklyBatchSize = 0;
	private final int WEEKLY_BATCH_SIZE = 1000;
	private volatile PreparedStatement historyBatchStm = null;

	private static Logger log = Logger.getLogger("net.yourhome.server.base.Database");
	private static volatile DatabaseConnector instance;
	private static Object lock = new Object();

	private boolean cleaning = false;

	private DatabaseConnector() {
		File dbPath = new File(this.DBPATH);
		if (!dbPath.exists()) {
			dbPath.mkdirs();
		}

		File dbFileArchive = new File(this.PATH_ARCHIVE);
		File dbFileWeekly = new File(this.PATH_WEEKLY);

		if (!dbFileArchive.exists()) {
			DatabaseConnector.log.info("Database history file does not exist. Creating new file on location " + this.PATH_ARCHIVE);
			try {
				dbFileArchive.createNewFile();
				this.allHistoryConnection = this.connect(this.PATH_ARCHIVE);
				this.createInitialZWaveArchive(this.allHistoryConnection);
			} catch (IOException e) {
				DatabaseConnector.log.error("Database file could not be created (" + e.getMessage() + ')');
			}
		} else {
			this.allHistoryConnection = this.connect(this.PATH_ARCHIVE);
		}
		if (!dbFileWeekly.exists()) {
			DatabaseConnector.log.info("Database weekly history file does not exist. Creating new file on location " + this.PATH_WEEKLY);
			try {
				File dbDefault = new File(this.PATH_DEFAULT);
				if (dbDefault.exists()) {
					/*
					 * Import default database data if the default database is
					 * present
					 */
					Files.copy(dbDefault, dbFileWeekly);
					this.weeklylHistoryConnection = this.connect(this.PATH_WEEKLY);
				} else {
					/*
					 * Create new database if the default database is not
					 * present
					 */
					dbFileWeekly.createNewFile();
					this.weeklylHistoryConnection = this.connect(this.PATH_WEEKLY);
					this.createInitialZWaveArchive(this.weeklylHistoryConnection);
					this.createInitialSettingsDatabase(this.weeklylHistoryConnection);
				}
			} catch (IOException e) {
				DatabaseConnector.log.error("Database file could not be created (" + e.getMessage() + ')');
			}
		} else {
			this.weeklylHistoryConnection = this.connect(this.PATH_WEEKLY);
		}
		try {
			this.weeklyHistoryStm = this.weeklylHistoryConnection.prepareStatement(this.INSERT_VALUE_CHANGE);
			this.historyBatchStm = this.allHistoryConnection.prepareStatement(this.INSERT_VALUE_CHANGE);
		} catch (SQLException e) {
		}

		// Schedule data cleanup every morning at 3am
		Scheduler.getInstance().scheduleCron(new TimerTask() {
			@Override
			public void run() {
				DatabaseConnector.this.cleanWeeklyDB();
				DatabaseConnector.this.flushWeeklyDb();
				DatabaseConnector.this.flushHistoryDb();
			}
		}, "00 03 * * *");

		// Schedule data cleanup every 1st and 15th at 10 past 3
		Scheduler.getInstance().scheduleCron(new TimerTask() {
			@Override
			public void run() {
				DatabaseConnector.this.flushHistoryDb();
				DatabaseConnector.this.cleanArchivingDB();
			}
		}, "10 03 01,15 * *");

		this.cleanWeeklyDB();
	}

	public void flushWeeklyDb() {
		if (this.weeklyHistoryStm != null && this.currentWeeklyBatchSize > 0) {
			try {
				this.weeklylHistoryConnection.setAutoCommit(false);
				DatabaseConnector.log.debug("Inserting remaining " + this.currentWeeklyBatchSize + " batch values into weekly db");
				this.weeklyHistoryStm.executeBatch();
				DatabaseConnector.log.debug("Inserting remaining " + this.currentWeeklyBatchSize + " batch values into weekly db - done");
				this.currentWeeklyBatchSize = 0;
			} catch (SQLException e) {
				DatabaseConnector.log.error("Error with batch insert", e);
			} finally {
				try {
					this.weeklylHistoryConnection.commit();
					this.weeklylHistoryConnection.setAutoCommit(true);
				} catch (SQLException e) {
					DatabaseConnector.log.error("Error with batch commit", e);
				}
			}
		}
	}

	private void flushHistoryDb() {
		if (this.historyBatchStm != null && this.currentHistoryBatchSize > 0) {
			try {
				this.allHistoryConnection.setAutoCommit(false);
				DatabaseConnector.log.debug("Inserting remaining " + this.currentHistoryBatchSize + " batch values");
				this.historyBatchStm.executeBatch();
				DatabaseConnector.log.debug("Inserting remaining " + this.currentHistoryBatchSize + " batch values - done");
				this.currentHistoryBatchSize = 0;
			} catch (SQLException e) {
				DatabaseConnector.log.error("Error with batch insert", e);
			} finally {
				try {
					this.allHistoryConnection.commit();
					this.allHistoryConnection.setAutoCommit(true);
				} catch (SQLException e) {
					DatabaseConnector.log.error("Error with batch commit", e);
				}
			}
		}
	}

	public static DatabaseConnector getInstance() {
		DatabaseConnector r = DatabaseConnector.instance;
		if (r == null) {
			synchronized (DatabaseConnector.lock) { // while we were waiting for
													// the lock, another
				r = DatabaseConnector.instance; // thread may have instantiated
												// the object
				if (r == null) {
					r = new DatabaseConnector();
					DatabaseConnector.instance = r;
				}
			}
		}
		return DatabaseConnector.instance;
	}

	private Connection connect(String path) {
		try {
			Class.forName("org.sqlite.JDBC");
			return DriverManager.getConnection("jdbc:sqlite:" + path);
		} catch (Exception e) {
			DatabaseConnector.log.fatal("Could not connect to database", e);
		}
		return null;
	}

	public ResultSet executeSelect(String sql, boolean flushFirst) throws SQLException {
		if (flushFirst) {
			this.flushWeeklyDb();
		}
		return this.executeSelect(sql);
	}

	public ResultSet executeSelect(String sql) throws SQLException {
		if (!this.cleaning) {
			ResultSet rs = null;
			Statement stmt = this.weeklylHistoryConnection.createStatement();
			rs = stmt.executeQuery(sql);
			return rs;
		}
		return null;
	}

	public ResultSet executeSelectArchiving(String sql, boolean flushFirst) throws SQLException {
		if (flushFirst) {
			this.flushHistoryDb();
		}
		return this.executeSelectArchiving(sql);
	}

	public ResultSet executeSelectArchiving(String sql) throws SQLException {
		if (!this.cleaning) {
			ResultSet rs = null;
			Statement stmt = this.allHistoryConnection.createStatement();
			rs = stmt.executeQuery(sql);
			return rs;
		}
		return null;
	}

	public PreparedStatement prepareStatement(String sql) throws SQLException {
		return this.weeklylHistoryConnection.prepareStatement(sql);
	}

	public ResultSet executePreparedStatement(PreparedStatement stmt) throws SQLException {
		return stmt.executeQuery();
	}

	public int executePreparedUpdate(PreparedStatement stmt) throws SQLException {
		int returnId = 0;
		stmt.executeUpdate();
		ResultSet keys = stmt.getGeneratedKeys();
		if (keys != null) {
			try {
				returnId = keys.getInt("last_insert_rowid()");
			} catch (SQLException e) {
				// Set status of returnId?
			} finally {
				try {
					keys.close();
				} catch (SQLException e) {
					DatabaseConnector.log.error("Exception occured: ", e);
				}
			}
		}
		return returnId;
	}

	public boolean executeQuery(String sql) throws SQLException {
		if (!this.cleaning) {
			boolean result = false;
			// try {
			Statement stmt = this.weeklylHistoryConnection.createStatement();
			result = stmt.execute(sql);
			stmt.close();
			result = true;
			// } catch (SQLException e) {
			// log.error("Exception occured: ",e);
			// }
			return result;
		} else {
			return false;
		}
	}

	private void cleanArchivingDB() {
		this.cleaning = true;

		try {
			String vacuumSQL = "VACUUM";
			DatabaseConnector.log.debug("Cleaning: " + vacuumSQL);
			Statement vacuumStm = this.allHistoryConnection.createStatement();
			vacuumStm.execute(vacuumSQL);
			vacuumStm.close();

		} catch (SQLException e) {
			DatabaseConnector.log.error("Exception occured: ", e);
		}

		this.cleaning = false;
	}

	private void cleanWeeklyDB() {
		DatabaseConnector.log.debug("Daily database cleanup for week db started!");
		Calendar today = Calendar.getInstance();
		long lastMonth = (today.getTimeInMillis() - (1000L * 60 * 60 * 24 * 32)) / 1000L;
		this.cleaning = true;

		try {
			String cleanSQL = "DELETE from Home_History where cast(strftime('%s', time) as integer) < '" + lastMonth + "'";
			DatabaseConnector.log.debug("Cleaning: " + cleanSQL);
			Statement cleanStm = this.weeklylHistoryConnection.createStatement();
			cleanStm.execute(cleanSQL);
			cleanStm.close();

			String vacuumSQL = "VACUUM";
			DatabaseConnector.log.debug("Cleaning: " + vacuumSQL);
			Statement vacuumStm = this.weeklylHistoryConnection.createStatement();
			vacuumStm.execute(vacuumSQL);
			vacuumStm.close();

		} catch (SQLException e) {
			DatabaseConnector.log.error("Exception occured: ", e);
		}

		this.cleaning = false;
	}

	private void createInitialSettingsDatabase(Connection connection) {

		String createCommand = " CREATE TABLE IF NOT EXISTS Scenes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE, name VARCHAR, json TEXT);" + " CREATE TABLE IF NOT EXISTS Http_Nodes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE, name VARCHAR, parentId INTEGER);" + " CREATE TABLE IF NOT EXISTS Radio_Channels (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE, channelUrl VARCHAR, channelName VARCHAR);" + " CREATE TABLE IF NOT EXISTS Rules (id INTEGER PRIMARY KEY NOT NULL, name VARCHAR, json TEXT DEFAULT (NULL), active BOOLEAN);" + " CREATE TABLE IF NOT EXISTS IP_Cameras (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name VARCHAR, snapshotUrl VARCHAR, videoUrl VARCHAR);"
				+ " CREATE TABLE IF NOT EXISTS Notification_GCM (registration_id VARCHAR PRIMARY KEY NOT NULL UNIQUE, name VARCHAR (200), width INTEGER (6),height INTEGER (6) );" + " CREATE TABLE IF NOT EXISTS Home_History (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, controller_identifier VARCHAR DEFAULT (NULL), node_identifier VARCHAR DEFAULT (NULL), value_identifier VARCHAR DEFAULT (NULL), time INTEGER DEFAULT (CURRENT_TIMESTAMP), unit VARCHAR, value VARCHAR, value_d VARCHAR);" + " CREATE TABLE IF NOT EXISTS Aliases (controller_identifier NOT NULL, node_identifier, value_identifier, alias, PRIMARY KEY (controller_identifier, node_identifier, value_identifier));"
				+ " CREATE TABLE IF NOT EXISTS Http_Commands (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE, parentNodeId INTEGER, name VARCHAR, description VARCHAR, url VARCHAR, httpMethod VARCHAR, messageType VARCHAR,header_1_key VARCHAR,header_1_value VARCHAR,header_2_key VARCHAR,header_2_value VARCHAR,header_3_key VARCHAR,header_3_value VARCHAR,header_4_key VARCHAR,header_4_value VARCHAR, messageBody VARCHAR,json TEXT DEFAULT (NULL));" + " CREATE TABLE IF NOT EXISTS ZWave_Value_Settings (homeId INTEGER NOT NULL, nodeId INTEGER NOT NULL, valueId INTEGER NOT NULL, nodeInstance INTEGER, subscribed BOOL, polled BOOL, PRIMARY KEY (homeId, nodeId, valueId, nodeInstance));";
		try {
			Statement stmt = connection.createStatement();
			stmt.executeUpdate(createCommand);
			stmt.close();
		} catch (SQLException e) {
			DatabaseConnector.log.error("Exception occured: ", e);
		}
	}

	public String getAlias(String controlIdentifier) {
		return (this.getAliasFromSql("SELECT alias FROM aliases WHERE controller_identifier='" + controlIdentifier + "'"));
	}

	public String getAlias(String controlIdentifier, String nodeIdentifier) {
		return (this.getAliasFromSql("SELECT alias FROM aliases WHERE controller_identifier='" + controlIdentifier + "' and node_identifier='" + nodeIdentifier + "' and value_identifier IS NULL"));
	}

	public String getAlias(String controlIdentifier, String nodeIdentifier, String valueIdentifier) {
		return (this.getAliasFromSql("SELECT alias FROM aliases WHERE controller_identifier='" + controlIdentifier + "' and node_identifier='" + nodeIdentifier + "' and value_identifier='" + valueIdentifier + "'"));
	}

	private Map<String, String> aliasCache = new LRUMap(1000);

	private String getAliasFromSql(String sql) {
		String alias = this.aliasCache.get(sql);
		if (alias != null) {
			return alias;
		} else {
			ResultSet result = null;
			try {
				result = this.executeSelect(sql);
				if (result.next()) {
					return result.getString("alias");
				}
			} catch (SQLException e) {
				DatabaseConnector.log.error("Exception occured: ", e);
			} finally {
				if (result != null) {
					try {
						result.getStatement().close();
						result.close();
					} catch (SQLException e) {
					}
				}
			}
		}
		return null;
	}

	public boolean setAlias(String controllerIdentifier, String alias) throws SQLException {
		String existingAlias = this.getAlias(controllerIdentifier);
		this.aliasCache.clear();
		if (existingAlias == null) {
			return this.executeQuery("INSERT INTO aliases (controller_identifier, alias) VALUES ('" + controllerIdentifier + "', '" + alias + "')");
		} else {
			return this.executeQuery("UPDATE aliases SET alias='" + alias + "' WHERE controller_identifier = '" + controllerIdentifier + "' and node_identifier is null and value_identifier is null");
		}
	}

	public boolean setAlias(String controllerIdentifier, String nodeIdentifier, String alias) throws SQLException {
		String existingAlias = this.getAlias(controllerIdentifier, nodeIdentifier);
		this.aliasCache.clear();
		if (existingAlias == null) {
			return this.executeQuery("INSERT INTO aliases (controller_identifier,node_identifier, alias) VALUES ('" + controllerIdentifier + "', '" + nodeIdentifier + "','" + alias + "')");
		} else {
			return this.executeQuery("UPDATE aliases SET alias='" + alias + "' WHERE controller_identifier = '" + controllerIdentifier + "' and node_identifier = '" + nodeIdentifier + "' and value_identifier is null");
		}

	}

	public boolean setAlias(String controllerIdentifier, String nodeIdentifier, String valueIdentifier, String alias) throws SQLException {
		String existingAlias = this.getAlias(controllerIdentifier, nodeIdentifier, valueIdentifier);
		this.aliasCache.clear();
		if (existingAlias == null) {
			return this.executeQuery("INSERT INTO aliases (controller_identifier, node_identifier,value_identifier, alias) VALUES ('" + controllerIdentifier + "', '" + nodeIdentifier + "', '" + valueIdentifier + "','" + alias + "')");
		} else {
			return this.executeQuery("UPDATE aliases SET alias='" + alias + "' WHERE controller_identifier = '" + controllerIdentifier + "' and node_identifier = '" + nodeIdentifier + "' and value_identifier = '" + valueIdentifier + "'");
		}
	}

	private void createInitialZWaveArchive(Connection connection) {
		String createCommand = "CREATE TABLE \"Home_History\" ( \"id\" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL ,\"controller_identifier\" VARCHAR DEFAULT (null) ,\"node_identifier\" VARCHAR DEFAULT (null) ,\"value_identifier\" VARCHAR DEFAULT (null) ,\"time\" INTEGER DEFAULT (CURRENT_TIMESTAMP) ,\"unit\" VARCHAR,\"value\" VARCHAR,\"value_d\" VARCHAR);";

		try {
			Statement stmt = connection.createStatement();
			stmt.executeUpdate(createCommand);
			stmt.close();
		} catch (SQLException e) {
			DatabaseConnector.log.error("Exception occured: ", e);
		}

	}

	public void insertValueChange(ControlIdentifiers controlIdentifiers, String unit, String valueString, Double valueDouble) {
		String currentTime = getTimestamp();
		try {
			this.historyBatchStm.setString(1, controlIdentifiers.getControllerIdentifier().convert());
			this.historyBatchStm.setString(2, controlIdentifiers.getNodeIdentifier());
			this.historyBatchStm.setString(3, controlIdentifiers.getValueIdentifier());
			this.historyBatchStm.setString(4, currentTime);
			this.historyBatchStm.setString(5, unit);
			this.historyBatchStm.setString(6, valueString);
			this.historyBatchStm.setDouble(7, valueDouble);
			this.historyBatchStm.addBatch();
			this.currentHistoryBatchSize++;
			if (this.currentHistoryBatchSize >= this.HISTORY_BATCH_SIZE) {
				this.flushHistoryDb();
			}
		} catch (SQLException e) {
			DatabaseConnector.log.error("Exception occured: ", e);
			e.printStackTrace();
		}
		try {
			this.weeklyHistoryStm.setString(1, controlIdentifiers.getControllerIdentifier().convert());
			this.weeklyHistoryStm.setString(2, controlIdentifiers.getNodeIdentifier());
			this.weeklyHistoryStm.setString(3, controlIdentifiers.getValueIdentifier());
			this.weeklyHistoryStm.setString(4, currentTime);
			this.weeklyHistoryStm.setString(5, unit);
			this.weeklyHistoryStm.setString(6, valueString);
			this.weeklyHistoryStm.setDouble(7, valueDouble);
			this.weeklyHistoryStm.addBatch();
			this.currentWeeklyBatchSize++;

			if (this.currentWeeklyBatchSize >= this.WEEKLY_BATCH_SIZE) {
				this.flushWeeklyDb();
			}
		} catch (SQLException e) {
			DatabaseConnector.log.error("Exception occured: ", e);
			e.printStackTrace();
		}

	}

	private String getTimestamp() {
		Date date = new Date();
		if (sqliteTimestampFormat == null) {
			sqliteTimestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
			sqliteTimestampFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		}
		return sqliteTimestampFormat.format(date);
	}

	private Map<String, ValueSettings> valueSettingsCache = new HashMap<String, ValueSettings>();

	public boolean insertOrUpdateZWaveValueSettings(ValueSettings settings) throws SQLException {
		if (settings.homeId != 0 & settings.nodeId != 0 && settings.valueId.intValue() != 0) {
			int polledFlag = (settings.polled) ? 1 : 0;
			int subscribedflag = (settings.subscribed) ? 1 : 0;
			String setValueSetting = "INSERT or REPLACE INTO ZWave_Value_Settings (homeId,nodeId,valueId,nodeInstance,subscribed,polled)" + " VALUES  ('" + settings.homeId + "','" + settings.nodeId + "','" + settings.valueId + "','" + settings.nodeInstance + "','" + subscribedflag + "','" + polledFlag + "')";
			this.executeQuery(setValueSetting);

			ZWaveValue value = new ZWaveValue((short) 0, settings.nodeId, settings.valueId, settings.homeId, settings.nodeInstance);
			this.valueSettingsCache.put(value.toControlId(), settings);
		}
		return false;
	}

	public ValueSettings getZWaveValueSettings(long homeId, short nodeId, BigInteger valueId, short nodeInstance) {
		ZWaveValue value = new ZWaveValue((short) 0, nodeId, valueId, homeId, nodeInstance);
		ValueSettings settings = this.valueSettingsCache.get(value.toControlId());
		if (settings != null) {
			return settings;
		} else {
			ResultSet resultTable = null;
			try {
				String getValueSetting = "SELECT * FROM ZWave_Value_Settings WHERE homeId = '" + homeId + "' AND nodeId = '" + nodeId + "' AND valueId = '" + valueId + "' AND nodeInstance = '" + nodeInstance + "'";
				resultTable = this.executeSelect(getValueSetting);

				// //resultTable.next();
			} catch (SQLException e) {
				DatabaseConnector.log.error("Exception occured: ", e);
			}
			settings = new ValueSettings(homeId, nodeId, valueId, nodeInstance, resultTable);
			this.valueSettingsCache.put(value.toControlId(), settings);
			return settings;
		}
	}

	public List<ValueSettings> getAllZWaveValueSettings(long homeId, short nodeId) {
		List<ValueSettings> valueSettingsList = new ArrayList<ValueSettings>();
		try {
			String getValueSetting = "SELECT * FROM ZWave_Value_Settings WHERE homeId = '" + homeId + "' AND nodeId = '" + nodeId + "'";
			ResultSet resultTable = this.executeSelect(getValueSetting);

			while (resultTable.next()) {
				BigInteger valueId = BigInteger.valueOf(resultTable.getLong("valueId"));
				short nodeInstance = resultTable.getShort("nodeInstance");
				ValueSettings vs = new ValueSettings(homeId, nodeId, valueId, nodeInstance, resultTable);
				valueSettingsList.add(vs);

				ZWaveValue value = new ZWaveValue((short) 0, nodeId, valueId, homeId, nodeInstance);
				this.valueSettingsCache.put(value.toControlId(), vs);

			}
		} catch (SQLException e) {
			DatabaseConnector.log.error("Exception occured: ", e);
		}

		return valueSettingsList;
	}

	public class ValueSettings {
		public ValueSettings(long homeId, short nodeId, BigInteger valueId, short nodeInstance, ResultSet resultRow) {
			this.homeId = homeId;
			this.nodeId = nodeId;
			this.valueId = valueId;
			this.nodeInstance = nodeInstance;
			if (resultRow != null) {
				try {
					this.subscribed = resultRow.getBoolean("subscribed");
					this.polled = resultRow.getBoolean("polled");
					this.alias = resultRow.getString("alias");
				} catch (SQLException e) {
					// Record does not exist
				}
			}
		}

		public long homeId;
		public short nodeId;
		public BigInteger valueId;
		public short nodeInstance;
		public boolean subscribed;
		public boolean polled;
		public String alias;

	}

	public int insertConfigurationValue(String insertString) throws SQLException {

		// Insert in temporary weekly db
		int returnId = 0;
		if (!this.cleaning) {
			Statement stmt = null;
			try {
				stmt = this.weeklylHistoryConnection.createStatement();
				stmt.executeUpdate(insertString);
				ResultSet keys = stmt.getGeneratedKeys();
				if (keys != null) {
					try {
						returnId = keys.getInt("last_insert_rowid()");
					} catch (SQLException e) {
						// Set status of returnId?
					} finally {
						try {
							keys.close();
						} catch (SQLException e) {
							DatabaseConnector.log.error("Exception occured: ", e);
						}
					}
				}
			} finally {
				if (stmt != null) {
					try {
						stmt.close();
					} catch (SQLException e) {
					}
				}
			}
		}
		return returnId;
	}

	public void destroy() {
		this.flushWeeklyDb();
		this.flushHistoryDb();
		try {
			if (this.weeklylHistoryConnection != null) {
				this.weeklylHistoryConnection.close();
			}
		} catch (SQLException e) {
		}
		try {
			if (this.allHistoryConnection != null) {
				this.allHistoryConnection.close();
			}
		} catch (SQLException e) {
		}
	}
}
