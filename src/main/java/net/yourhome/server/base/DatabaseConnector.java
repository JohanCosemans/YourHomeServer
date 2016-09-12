package net.yourhome.server.base;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import org.apache.commons.collections.map.LRUMap;
import org.apache.log4j.Logger;

import com.google.common.io.Files;

import net.yourhome.common.net.messagestructures.zwave.ZWaveValue;
import net.yourhome.common.net.model.binding.ControlIdentifiers;

public class DatabaseConnector {
	private final String DBPATH = SettingsManager.getBasePath() + "/database/";
	private final String PATH_ARCHIVE = DBPATH + "home_history_archive.db";
	private final String PATH_WEEKLY = DBPATH + "home_history_weekly.db";
	private final String PATH_DEFAULT = DBPATH + "home_history_weekly_default.db";
	private final String INSERT_VALUE_CHANGE = "insert into main.Home_History ('controller_identifier', 'node_identifier', 'value_identifier','unit','value','value_d') VALUES (?,?,?,?,?,?)";

	private Connection allHistoryConnection;	
	private volatile int currentHistoryBatchSize=0;
	private final int HISTORY_BATCH_SIZE = 1000;
	private volatile PreparedStatement weeklyHistoryStm = null;

	private Connection weeklylHistoryConnection;
	private volatile int currentWeeklyBatchSize=0;
	private final int WEEKLY_BATCH_SIZE = 1000;
	private volatile PreparedStatement historyBatchStm = null;
	
	
	private static Logger log = Logger.getLogger("net.yourhome.server.base.Database");
	private static volatile DatabaseConnector instance;
	private static Object lock = new Object();

	private boolean cleaning = false;

	private DatabaseConnector() {
		File dbPath = new File(DBPATH);
		if (!dbPath.exists()) {
			dbPath.mkdirs();
		}

		File dbFileArchive = new File(PATH_ARCHIVE);
		File dbFileWeekly = new File(PATH_WEEKLY);
		
		if (!dbFileArchive.exists()) {
			log.info("Database history file does not exist. Creating new file on location " + PATH_ARCHIVE);
			try {
				dbFileArchive.createNewFile();
				allHistoryConnection = this.connect(PATH_ARCHIVE);
				this.createInitialZWaveArchive(allHistoryConnection);
			} catch (IOException e) {
				log.error("Database file could not be created (" + e.getMessage() + ')');
			}
		}else {
			allHistoryConnection = this.connect(PATH_ARCHIVE);
		}
		if (!dbFileWeekly.exists()) {
			log.info("Database weekly history file does not exist. Creating new file on location " + PATH_WEEKLY);
			try {
				File dbDefault = new File(PATH_DEFAULT);
				if(dbDefault.exists()) {
					/* Import default database data if the default database is present */
					Files.copy(dbDefault, dbFileWeekly);
					weeklylHistoryConnection = this.connect(PATH_WEEKLY);
				}else {
					/* Create new database if the default database is not present */
					dbFileWeekly.createNewFile();
					weeklylHistoryConnection = this.connect(PATH_WEEKLY);
					this.createInitialZWaveArchive(weeklylHistoryConnection);
					this.createInitialSettingsDatabase(weeklylHistoryConnection);
				}
			} catch (IOException e) {
				log.error("Database file could not be created (" + e.getMessage() + ')');
			}
		}else {
			weeklylHistoryConnection = this.connect(PATH_WEEKLY);
		}
		try {
			this.weeklyHistoryStm = weeklylHistoryConnection.prepareStatement(INSERT_VALUE_CHANGE);
			this.historyBatchStm = allHistoryConnection.prepareStatement(INSERT_VALUE_CHANGE);
		} catch (SQLException e) {}
		

		// Schedule data cleanup every morning at 3am
		Scheduler.getInstance().scheduleCron(new TimerTask() {
			@Override
			public void run() {
				cleanWeeklyDB();
				flushWeeklyDb();
				flushHistoryDb();
			}
		}, "00 03 * * *");

		// Schedule data cleanup every 1st and 15th at 10 past 3
		Scheduler.getInstance().scheduleCron(new TimerTask() {
			@Override
			public void run() {
				flushHistoryDb();
				cleanArchivingDB();
			}
		}, "10 03 01,15 * *");

		cleanWeeklyDB();
	}
	public void flushWeeklyDb() {			
		if(weeklyHistoryStm != null && currentWeeklyBatchSize > 0) {
			try {
				weeklylHistoryConnection.setAutoCommit(false);
				log.debug("Inserting remaining "+currentWeeklyBatchSize+" batch values into weekly db");
				weeklyHistoryStm.executeBatch();
				log.debug("Inserting remaining "+currentWeeklyBatchSize+" batch values into weekly db - done");
				currentWeeklyBatchSize = 0;
			} catch (SQLException e) {
				log.error("Error with batch insert", e);
			}finally {
				try {
					weeklylHistoryConnection.commit();
					weeklylHistoryConnection.setAutoCommit(true);
				} catch (SQLException e) {
					log.error("Error with batch commit", e);
				}
			}
		}
	}
	private void flushHistoryDb() {
		if(historyBatchStm != null && currentHistoryBatchSize > 0) {
			try {
				allHistoryConnection.setAutoCommit(false);
				log.debug("Inserting remaining "+currentHistoryBatchSize+" batch values");
				historyBatchStm.executeBatch();
				log.debug("Inserting remaining "+currentHistoryBatchSize+" batch values - done");
				currentHistoryBatchSize = 0;
			} catch (SQLException e) {
				log.error("Error with batch insert", e);
			}finally {
				try {
					allHistoryConnection.commit();
					allHistoryConnection.setAutoCommit(true);
				} catch (SQLException e) {
					log.error("Error with batch commit", e);
				}
			}
		}
	}
	public static DatabaseConnector getInstance() {
		DatabaseConnector r = instance;
		if (r == null) {
			synchronized (lock) { // while we were waiting for the lock, another
				r = instance; // thread may have instantiated the object
				if (r == null) {
					r = new DatabaseConnector();
					instance = r;
				}
			}
		}
		return instance;
	}

	private Connection connect(String path) {
		try {
			Class.forName("org.sqlite.JDBC");
			return DriverManager.getConnection("jdbc:sqlite:" + path);
		} catch (Exception e) {
			log.fatal("Could not connect to database", e);
		}
		return null;
	}

	public ResultSet executeSelect(String sql, boolean flushFirst) throws SQLException {
		if(flushFirst) {
			flushWeeklyDb();
		}
		return executeSelect(sql);
	}
	public ResultSet executeSelect(String sql) throws SQLException {
		if (!cleaning) {
			ResultSet rs = null;
			Statement stmt = weeklylHistoryConnection.createStatement();
			rs = stmt.executeQuery(sql);
			return rs;
		}
		return null;
	}

	public ResultSet executeSelectArchiving(String sql, boolean flushFirst) throws SQLException {
		if(flushFirst) {
			flushHistoryDb();
		}
		return executeSelectArchiving(sql);
	}
	public ResultSet executeSelectArchiving(String sql) throws SQLException {
		if (!cleaning) {
			ResultSet rs = null;
			Statement stmt = allHistoryConnection.createStatement();
			rs = stmt.executeQuery(sql);
			return rs;
		}
		return null;
	}

	public PreparedStatement prepareStatement(String sql) throws SQLException {
		return weeklylHistoryConnection.prepareStatement(sql);
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
					log.error("Exception occured: ", e);
				}
			}
		}
		return returnId;
	}

	public boolean executeQuery(String sql) throws SQLException {
		if (!cleaning) {
			boolean result = false;
			// try {
			Statement stmt = weeklylHistoryConnection.createStatement();
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
		cleaning = true;

		try {
			String vacuumSQL = "VACUUM";
			log.debug("Cleaning: " + vacuumSQL);
			Statement vacuumStm = allHistoryConnection.createStatement();
			vacuumStm.execute(vacuumSQL);
			vacuumStm.close();

		} catch (SQLException e) {
			log.error("Exception occured: ", e);
		}

		cleaning = false;
	}

	private void cleanWeeklyDB() {
		log.debug("Daily database cleanup for week db started!");
		Calendar today = Calendar.getInstance();
		long lastMonth = (today.getTimeInMillis() - (1000L * 60 * 60 * 24 * 32)) / 1000L;
		cleaning = true;

		try {
			String cleanSQL = "DELETE from Home_History where cast(strftime('%s', time) as integer) < '" + lastMonth + "'";
			log.debug("Cleaning: " + cleanSQL);
			Statement cleanStm = weeklylHistoryConnection.createStatement();
			cleanStm.execute(cleanSQL);
			cleanStm.close();

			String vacuumSQL = "VACUUM";
			log.debug("Cleaning: " + vacuumSQL);
			Statement vacuumStm = weeklylHistoryConnection.createStatement();
			vacuumStm.execute(vacuumSQL);
			vacuumStm.close();

		} catch (SQLException e) {
			log.error("Exception occured: ", e);
		}

		cleaning = false;
	}

	private void createInitialSettingsDatabase(Connection connection) {

		String createCommand = " CREATE TABLE IF NOT EXISTS Scenes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE, name VARCHAR, json TEXT);" 
				+ " CREATE TABLE IF NOT EXISTS Http_Nodes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE, name VARCHAR, parentId INTEGER);" 
				+ " CREATE TABLE IF NOT EXISTS Radio_Channels (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE, channelUrl VARCHAR, channelName VARCHAR);" 
				+ " CREATE TABLE IF NOT EXISTS Rules (id INTEGER PRIMARY KEY NOT NULL, name VARCHAR, json TEXT DEFAULT (NULL), active BOOLEAN);" 
				+ " CREATE TABLE IF NOT EXISTS IP_Cameras (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name VARCHAR, snapshotUrl VARCHAR, videoUrl VARCHAR);"
				+ " CREATE TABLE IF NOT EXISTS Notification_GCM (registration_id VARCHAR PRIMARY KEY NOT NULL UNIQUE, name VARCHAR (200), width INTEGER (6),height INTEGER (6) );" 
				+ " CREATE TABLE IF NOT EXISTS Home_History (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, controller_identifier VARCHAR DEFAULT (NULL), node_identifier VARCHAR DEFAULT (NULL), value_identifier VARCHAR DEFAULT (NULL), time INTEGER DEFAULT (CURRENT_TIMESTAMP), unit VARCHAR, value VARCHAR, value_d VARCHAR);" 
				+ " CREATE TABLE IF NOT EXISTS Aliases (controller_identifier NOT NULL, node_identifier, value_identifier, alias, PRIMARY KEY (controller_identifier, node_identifier, value_identifier));"
				+ " CREATE TABLE IF NOT EXISTS Http_Commands (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE, parentNodeId INTEGER, name VARCHAR, description VARCHAR, url VARCHAR, httpMethod VARCHAR, messageType VARCHAR,header_1_key VARCHAR,header_1_value VARCHAR,header_2_key VARCHAR,header_2_value VARCHAR,header_3_key VARCHAR,header_3_value VARCHAR,header_4_key VARCHAR,header_4_value VARCHAR, messageBody VARCHAR,json TEXT DEFAULT (NULL));" 
				+ " CREATE TABLE IF NOT EXISTS ZWave_Value_Settings (homeId INTEGER NOT NULL, nodeId INTEGER NOT NULL, valueId INTEGER NOT NULL, nodeInstance INTEGER, subscribed BOOL, polled BOOL, PRIMARY KEY (homeId, nodeId, valueId, nodeInstance));";
		try {
			Statement stmt = connection.createStatement();
			stmt.executeUpdate(createCommand);
			stmt.close();
		} catch (SQLException e) {
			log.error("Exception occured: ", e);
		}
	}

	public String getAlias(String controlIdentifier) {
		return (getAliasFromSql("SELECT alias FROM aliases WHERE controller_identifier='" + controlIdentifier + "'"));
	}

	public String getAlias(String controlIdentifier, String nodeIdentifier) {
		return (getAliasFromSql("SELECT alias FROM aliases WHERE controller_identifier='" + controlIdentifier + "' and node_identifier='" + nodeIdentifier + "' and value_identifier IS NULL"));
	}

	public String getAlias(String controlIdentifier, String nodeIdentifier, String valueIdentifier) {
		return (getAliasFromSql("SELECT alias FROM aliases WHERE controller_identifier='" + controlIdentifier + "' and node_identifier='" + nodeIdentifier + "' and value_identifier='" + valueIdentifier + "'"));
	}

	private Map<String, String> aliasCache = new LRUMap(1000);

	private String getAliasFromSql(String sql) {
		String alias = aliasCache.get(sql);
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
				log.error("Exception occured: ", e);
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
		aliasCache.clear();
		if (existingAlias == null) {
			return this.executeQuery("INSERT INTO aliases (controller_identifier, alias) VALUES ('" + controllerIdentifier + "', '" + alias + "')");
		} else {
			return this.executeQuery("UPDATE aliases SET alias='" + alias + "' WHERE controller_identifier = '" + controllerIdentifier + "' and node_identifier is null and value_identifier is null");
		}
	}

	public boolean setAlias(String controllerIdentifier, String nodeIdentifier, String alias) throws SQLException {
		String existingAlias = this.getAlias(controllerIdentifier, nodeIdentifier);
		aliasCache.clear();
		if (existingAlias == null) {
			return this.executeQuery("INSERT INTO aliases (controller_identifier,node_identifier, alias) VALUES ('" + controllerIdentifier + "', '" + nodeIdentifier + "','" + alias + "')");
		} else {
			return this.executeQuery("UPDATE aliases SET alias='" + alias + "' WHERE controller_identifier = '" + controllerIdentifier + "' and node_identifier = '" + nodeIdentifier + "' and value_identifier is null");
		}

	}

	public boolean setAlias(String controllerIdentifier, String nodeIdentifier, String valueIdentifier, String alias) throws SQLException {
		String existingAlias = this.getAlias(controllerIdentifier, nodeIdentifier, valueIdentifier);
		aliasCache.clear();
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
			log.error("Exception occured: ", e);
		}

	}

	public void insertValueChange(ControlIdentifiers controlIdentifiers, String unit, String valueString, Double valueDouble) {

		try {			
			historyBatchStm.setString(1, controlIdentifiers.getControllerIdentifier().convert());
			historyBatchStm.setString(2, controlIdentifiers.getNodeIdentifier());
			historyBatchStm.setString(3, controlIdentifiers.getValueIdentifier());
			historyBatchStm.setString(4, unit);
			historyBatchStm.setString(5, valueString);
			historyBatchStm.setDouble(6, valueDouble);
			historyBatchStm.addBatch();
			currentHistoryBatchSize++;
			if(currentHistoryBatchSize >= HISTORY_BATCH_SIZE) {
				flushHistoryDb();
			}
		} catch (SQLException e) {
			log.error("Exception occured: ", e);
		}
		try {
			weeklyHistoryStm.setString(1, controlIdentifiers.getControllerIdentifier().convert());
			weeklyHistoryStm.setString(2, controlIdentifiers.getNodeIdentifier());
			weeklyHistoryStm.setString(3, controlIdentifiers.getValueIdentifier());
			weeklyHistoryStm.setString(4, unit);
			weeklyHistoryStm.setString(5, valueString);
			weeklyHistoryStm.setDouble(6, valueDouble);
			weeklyHistoryStm.addBatch();
			currentWeeklyBatchSize++;
			
			if(currentWeeklyBatchSize >= WEEKLY_BATCH_SIZE) {
				flushWeeklyDb();
			}
		} catch (SQLException e) {
			log.error("Exception occured: ", e);
		}		
		
	}

	private Map<String, ValueSettings> valueSettingsCache = new HashMap<String, ValueSettings>();

	public boolean insertOrUpdateZWaveValueSettings(ValueSettings settings) throws SQLException {
		if (settings.homeId != 0 & settings.nodeId != 0 && settings.valueId.intValue() != 0) {
			int polledFlag = (settings.polled) ? 1 : 0;
			int subscribedflag = (settings.subscribed) ? 1 : 0;
			String setValueSetting = "INSERT or REPLACE INTO ZWave_Value_Settings (homeId,nodeId,valueId,nodeInstance,subscribed,polled)" + " VALUES  ('" + settings.homeId + "','" + settings.nodeId + "','" + settings.valueId + "','" + settings.nodeInstance + "','" + subscribedflag + "','" + polledFlag + "')";
			this.executeQuery(setValueSetting);

			ZWaveValue value = new ZWaveValue((short) 0, settings.nodeId, settings.valueId, settings.homeId, settings.nodeInstance);
			valueSettingsCache.put(value.toControlId(), settings);
		}
		return false;
	}

	public ValueSettings getZWaveValueSettings(long homeId, short nodeId, BigInteger valueId, short nodeInstance) {
		ZWaveValue value = new ZWaveValue((short) 0, nodeId, valueId, homeId, nodeInstance);
		ValueSettings settings = valueSettingsCache.get(value.toControlId());
		if (settings != null) {
			return settings;
		} else {
			ResultSet resultTable = null;
			try {
				String getValueSetting = "SELECT * FROM ZWave_Value_Settings WHERE homeId = '" + homeId + "' AND nodeId = '" + nodeId + "' AND valueId = '" + valueId + "' AND nodeInstance = '" + nodeInstance + "'";
				resultTable = this.executeSelect(getValueSetting);

				// //resultTable.next();
			} catch (SQLException e) {
				log.error("Exception occured: ", e);
			}
			settings = new ValueSettings(homeId, nodeId, valueId, nodeInstance, resultTable);
			valueSettingsCache.put(value.toControlId(), settings);
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
				valueSettingsCache.put(value.toControlId(), vs);

			}
		} catch (SQLException e) {
			log.error("Exception occured: ", e);
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
		if (!cleaning) {
			Statement stmt = null;
			try {
				stmt = weeklylHistoryConnection.createStatement();
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
							log.error("Exception occured: ", e);
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
		flushWeeklyDb();
		flushHistoryDb();
		try {
			if(this.weeklylHistoryConnection != null) {
				this.weeklylHistoryConnection.close();
			}
		} catch (SQLException e) {}
		try {
			if(allHistoryConnection != null) {
				this.allHistoryConnection.close();
			}
		} catch (SQLException e) { }
	}
}
