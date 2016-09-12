package net.yourhome.server.base;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.TimerTask;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.log4j.Logger;
import org.json.JSONException;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

import net.yourhome.common.base.enums.ControllerTypes;
import net.yourhome.common.base.enums.ValueTypes;
import net.yourhome.common.net.messagestructures.JSONMessage;
import net.yourhome.common.net.messagestructures.general.ActivationMessage;
import net.yourhome.common.net.messagestructures.general.ClientMessageMessage;
import net.yourhome.common.net.messagestructures.general.ClientNotificationMessage;
import net.yourhome.common.net.messagestructures.general.GCMRegistrationMessage;
import net.yourhome.common.net.messagestructures.general.SetValueMessage;
import net.yourhome.common.net.messagestructures.general.ValueHistoryMessage;
import net.yourhome.common.net.messagestructures.general.ValueHistoryRequest;
import net.yourhome.common.net.model.Device;
import net.yourhome.common.net.model.binding.ControlIdentifiers;
import net.yourhome.server.AbstractController;
import net.yourhome.server.ControllerNode;
import net.yourhome.server.ControllerValue;
import net.yourhome.server.IController;
import net.yourhome.server.base.rules.scenes.Scene;
import net.yourhome.server.base.rules.scenes.SceneManager;
import net.yourhome.server.base.rules.scenes.actions.notifications.GoogleCloudMessagingService;
import net.yourhome.server.demo.DemoController;
import net.yourhome.server.net.Server;
import net.yourhome.server.radio.BasicPlayer;

public class GeneralController extends AbstractController {

	public enum Settings {
		SERVER_NAME(new Setting("SERVER_NAME", "Server name")),
		NET_HTTP_PORT(new Setting("NET_HTTP_PORT", "Server Port")),
		NET_USERNAME(new Setting("NET_USERNAME", "Username of UI Designer", "Leave empty if none")),
		NET_PASSWORD(new Setting("NET_PASSWORD", "Password of UI Designer", "Leave empty if none")),
		SMTP_ADDRESS(new Setting("SMTP_ADDRESS", "SMTP Address (get one free at eg app.mailjet.com/signup)", "in-v3.mailjet.com")),
		SMTP_PORT(new Setting("SMTP_PORT", "SMTP Port")),
		SMTP_USER(new Setting("SMTP_USER", "SMTP User")),
		SMTP_PASSWORD(new Setting("SMTP_PASSWORD", "SMTP Password")),
		SMTP_SENDER(new Setting("SMTP_SENDER", "SMTP Sender Email")),
		SMS_KEY(new Setting("SMS_KEY", "SMS API Key (get one at nexmo.com)", "c164e41d")),
		SMS_PASSWORD(new Setting("SMS_PASSWORD", "SMS API Secret key", "9448240b")),
		SUNSET_LAT(new Setting("SUNSET_LAT", "Server Latitude (see www.latlong.net)", "50.8503")),
		SUNSET_LONG(new Setting("SUNSET_LONG", "Server Longitude", "4.3517"));
		private Setting setting;

		private Settings(Setting setting) {
			this.setting = setting;
		}

		public Setting get() {
			return setting;
		}
	}

	private DatabaseConnector dbConnector = DatabaseConnector.getInstance();

	// Singleton instance
	private static Logger log = Logger.getLogger("net.yourhome.server.base.General");
	private static volatile GeneralController generalControllerInstance;
	private static Object lock = new Object();

	public static GeneralController getInstance() {
		GeneralController r = generalControllerInstance;
		if (r == null) {
			synchronized (lock) { // while we were waiting for the lock, another
				r = generalControllerInstance; // thread may have instantiated
												// the object
				if (r == null) {
					r = new GeneralController();
					generalControllerInstance = r;
				}
			}
		}
		return generalControllerInstance;
	}

	@Override
	public JSONMessage parseNetMessage(JSONMessage message) {
		if (message instanceof GCMRegistrationMessage) {
			// Google cloud registration
			GCMRegistrationMessage GCMMessage = (GCMRegistrationMessage) message;
			GoogleCloudMessagingService GCMService = GoogleCloudMessagingService.getInstance();
			try {
				GCMService.registerClient(new Device(GCMMessage.registrationId, GCMMessage.name, GCMMessage.screenWidth, GCMMessage.screenHeight));
			} catch (SQLException e) {
				log.error("Exception occured: ", e);
			}
		} else if (message instanceof ValueHistoryRequest) {
			// Build original message
			ValueHistoryRequest message2 = (ValueHistoryRequest) message;
			
			// Prepare answer message
			ValueHistoryMessage historyMessage = new ValueHistoryMessage();
			historyMessage.controlIdentifiers = message2.controlIdentifiers;
			historyMessage.offset = message2.offset;
			
			try {
				String operation = "value as value";
				switch (message2.operation) {
				case AVERAGE:
					operation = "avg(value_d) as value_d";
					break;
				case DELTA:
					operation = "max(value_d)-min(value_d) as value_d";
					break;
				case MAX:
					operation = "max(value_d) as value_d";
					break;
				case MIN:
					operation = "min(value_d) as value_d";
					break;
				}
				String select = null;
				ResultSet dataTable = null;
				switch (message2.periodType) {
				case REALTIME:
					// Select data from database (no operation possible)
					select = "SELECT time as datetime,strftime('%s', time) as time,value_d, unit " + "  FROM home_history" + " WHERE value_identifier='" + message2.controlIdentifiers.getValueIdentifier() + "' " + "   AND " + " node_identifier = '" + message2.controlIdentifiers.getNodeIdentifier() + "' " + "   AND " + " controller_identifier = '" + message2.controlIdentifiers.getControllerIdentifier().convert() + "' " + "GROUP BY datetime " + "ORDER BY datetime DESC LIMIT " + message2.offset + "," + message2.historyAmount;
					dataTable = this.dbConnector.executeSelect(select, true);
					break;
				case DAILY:
					select = "SELECT strftime('%s',date(day)) as time,unit," + operation + " from ( SELECT time,strftime('%Y-%m-%d', time) as day,value_d, unit" + " 		FROM home_history" + " WHERE value_identifier='" + message2.controlIdentifiers.getValueIdentifier() + "' " + "   AND " + " node_identifier = '" + message2.controlIdentifiers.getNodeIdentifier() + "' " + "   AND " + "controller_identifier = '" + message2.controlIdentifiers.getControllerIdentifier().convert() + "' " + " 		GROUP BY time) as deltaPerDay" + " GROUP BY day" + " ORDER BY time DESC LIMIT " + message2.offset + "," + message2.historyAmount;

					if (message2.historyAmount > 31 && message2.offset == 0) {
						dataTable = this.dbConnector.executeSelectArchiving(select, true);
					} else {
						dataTable = this.dbConnector.executeSelect(select, true);
					}
					break;
				case WEEKLY:
					select = "SELECT strftime('%s',date(time,'weekday 0','-6 days')) as time,unit," + operation + " from ( SELECT time,strftime('%Y-%W', time) as week,value_d, unit" + " 		FROM home_history" + " WHERE value_identifier='" + message2.controlIdentifiers.getValueIdentifier() + "' " + "   AND " + " node_identifier = '" + message2.controlIdentifiers.getNodeIdentifier() + "' " + "   AND " + "controller_identifier = '" + message2.controlIdentifiers.getControllerIdentifier().convert() + "' " + " 		GROUP BY time) as deltaPerWeek" + " GROUP BY week" + " ORDER BY time DESC LIMIT " + message2.offset + "," + message2.historyAmount;

					if (message2.historyAmount > 4 && message2.offset == 0) {
						dataTable = this.dbConnector.executeSelectArchiving(select, true);
					} else {
						dataTable = this.dbConnector.executeSelect(select, true);
					}
					/*
					 * SELECT strftime('%s',date(time,'weekday 0','-6 days')) as
					 * time,max(value)-min(value) as value from ( SELECT time,
					 * strftime('%Y-%W', time) as week, value FROM home_history
					 * WHERE nodeid='39' AND valueid = '659324930' AND homeId =
					 * '3239454784' AND instance = '1' GROUP BY time) as
					 * deltaPerWeek group by week
					 */
					break;
				case MONTHLY:
					/*
					 * SELECT strftime('%s',date(month)) as
					 * time,max(value)-min(value) as value from ( SELECT
					 * strftime('%Y-%m-01', time) as month,value FROM
					 * home_history WHERE nodeid='39' AND valueid = '659324930'
					 * AND homeId = '3239454784' AND instance = '1' GROUP BY
					 * time) as deltaPerMonth group by month
					 */
					select = "SELECT strftime('%s',date(month)) as time,unit," + operation + " from ( SELECT time,strftime('%Y-%m-01', time) as month,value_d,unit" + " 		FROM home_history" + " WHERE value_identifier='" + message2.controlIdentifiers.getValueIdentifier() + "' " + "   AND " + " node_identifier = '" + message2.controlIdentifiers.getNodeIdentifier() + "' " + "   AND " + "controller_identifier = '" + message2.controlIdentifiers.getControllerIdentifier().convert() + "' " + " 		GROUP BY time) as deltaPerMonth" + " GROUP BY month" + " ORDER BY time DESC LIMIT " + message2.offset + "," + message2.historyAmount;
					dataTable = this.dbConnector.executeSelectArchiving(select, true);

					break;
				}
				if (dataTable != null) {
					while (dataTable.next()) {
						historyMessage.sensorValues.time.add(dataTable.getInt("time"));
						if (historyMessage.sensorValues.valueUnit == null || historyMessage.sensorValues.valueUnit == "") {
							historyMessage.sensorValues.valueUnit = dataTable.getString("unit");
						}
						Double value = dataTable.getDouble("value_d");
						if (value != null) {
							historyMessage.sensorValues.value.add(value);
						}
					}
					dataTable.close();
				}

				// Get name of value and use as graph title
				IController sourceController = Server.getInstance().getControllers().get(message2.controlIdentifiers.getControllerIdentifier().convert());
				String title = "";
				if (sourceController != null) {
					title = sourceController.getValueName(message2.controlIdentifiers);
					if (title == null) {
						title = "";
					}
				}
				historyMessage.title = title;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				log.error("Exception occured: ", e);
			}
			return historyMessage;
		} else if (message instanceof ActivationMessage) {
			// Scenes
			if (message.controlIdentifiers.getNodeIdentifier().equals("Scenes")) {
				try {
					Scene sceneToActivate = SceneManager.getScene(Integer.parseInt(message.controlIdentifiers.getValueIdentifier())); 
					if(sceneToActivate != null && sceneToActivate.activate()) {
						ClientMessageMessage informClientsMessage = new ClientMessageMessage();
						informClientsMessage.broadcast = true;
						informClientsMessage.messageContent = "Scene " + sceneToActivate.getName() + " activated";
						return informClientsMessage;
					}else {
						ClientMessageMessage informClientsMessage = new ClientMessageMessage();
						informClientsMessage.broadcast = false;
						informClientsMessage.messageContent = "Could not activate scene " + sceneToActivate.getName();
						return informClientsMessage;						
					}
				} catch (NumberFormatException | SQLException | JSONException e) {
					log.error("Exception occured: ", e);
				}
			} else if (message.controlIdentifiers.getNodeIdentifier().equals("Commands")) {
				if (message.controlIdentifiers.getValueIdentifier().equals(ValueTypes.SOUND_NOTIFICATION.convert())) {
					BasicPlayer notificationPlayer = new BasicPlayer();
					try {
						notificationPlayer.setDataSource(new File(SettingsManager.getBasePath(), "sounds/doorbell-1.mp3"));
						notificationPlayer.setVolume(100);
						notificationPlayer.startPlayback();
					} catch (UnsupportedAudioFileException e) {
						log.error("Exception occured: ", e);
					} catch (LineUnavailableException e) {
						log.error("Exception occured: ", e);
					} catch (IOException e) {
						log.error("Exception occured: ", e);
					}
				}
			}
		} else if (message instanceof SetValueMessage) {
			SetValueMessage setValueMessage = (SetValueMessage) message;
			if (setValueMessage.controlIdentifiers.getNodeIdentifier().equals("Commands")) {

				if (setValueMessage.controlIdentifiers.getValueIdentifier().equals(ValueTypes.SYSTEM_COMMAND.convert())) {
					Runtime r = Runtime.getRuntime();
					Process p;
					BufferedReader b = null;
					String result = null;

					try {
						String[] command = new String[] { "bash", "-c", setValueMessage.value };
						p = r.exec(command);

						p.waitFor();
						b = new BufferedReader(new InputStreamReader(p.getInputStream()));
						String line = "";
						while ((line = b.readLine()) != null) {
							result += line;
						}
						b.close();
					} catch (IOException e) {
						log.error("Error on performing action", e);
					} catch (InterruptedException e) {
						log.error("Error on performing action", e);
					} finally {
						if (b != null) {
							try {
								b.close();
							} catch (IOException e) {
							}
						}
					}
				} else if (setValueMessage.controlIdentifiers.getValueIdentifier().equals(ValueTypes.WAIT.convert())) {
					int seconds = Integer.parseInt(setValueMessage.value);
					try {
						Thread.sleep(seconds * 1000);
					} catch (InterruptedException e) {
						log.error("Exception occured: ", e);
					}
				}
			}
		}
		return null;
	}

	@Override
	public void init() {
		super.init();
		try {
			enableSunsetSunriseEvents();
		} catch (Exception e) {
			log.error("Error on scheduling sunrise/sunset events", e);
		}
		Server.getInstance().init();

		log.info("Initialized");
	}

	@Override
	public String getIdentifier() {
		return ControllerTypes.GENERAL.convert();
	}

	@Override
	public List<JSONMessage> initClient() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public String getName() {
		return "General";
	}

	@Override
	public List<ControllerNode> getNodes() {
		/*
		 * Nodes: * General - Message - Scene activation - Wait - Execute native
		 * system command
		 */
		List<ControllerNode> returnList = new ArrayList<ControllerNode>();
		ControllerNode commandsNode = new ControllerNode(this, "Commands", "Commands", "");
		commandsNode.addValue(new ControllerValue(ValueTypes.SEND_NOTIFICATION.convert(), "Send Notification", ValueTypes.SEND_NOTIFICATION));
		commandsNode.addValue(new ControllerValue(ValueTypes.SOUND_NOTIFICATION.convert(), "Play Notification", ValueTypes.SOUND_NOTIFICATION));
		commandsNode.addValue(new ControllerValue(ValueTypes.WAIT.convert(), "Wait", ValueTypes.WAIT));
		commandsNode.addValue(new ControllerValue(ValueTypes.SYSTEM_COMMAND.convert(), "System Command", ValueTypes.SYSTEM_COMMAND));

		ControllerNode scenesNode = new ControllerNode(this, "Scenes", "Scenes", "scenes");

		List<Scene> allScenes;
		try {
			allScenes = SceneManager.getAllScenes();
			for (Scene scene : allScenes) {
				scenesNode.addValue(new ControllerValue(scene.getId() + "", scene.getName(), ValueTypes.SCENE_ACTIVATION));
			}
		} catch (SQLException e) {
			log.error("Exception occured: ", e);
		}

		returnList.add(scenesNode);
		returnList.add(commandsNode);
		return returnList;
	}

	@Override
	public String getValueName(ControlIdentifiers valueIdentifier) {
		return null;
	}

	@Override
	public List<ControllerNode> getTriggers() {
		/*
		 * Nodes: * General - Time > Periodic - Time > Sunrise - Time > Sunset -
		 * Music > Music Started - Music > Music Stopped
		 */
		List<ControllerNode> returnList = new ArrayList<ControllerNode>();

		ControllerNode timeNode = new ControllerNode(this, "Time", "Time", "");
		timeNode.addValue(new ControllerValue("Periodic", "Periodic", ValueTypes.TIME_PERIOD));
		timeNode.addValue(new ControllerValue("Sunrise", "Sunrise", ValueTypes.EVENT));
		timeNode.addValue(new ControllerValue("Sunset", "Sunset", ValueTypes.EVENT));

		ControllerNode musicNode = new ControllerNode(this, "Music", "Music", "");
		musicNode.addValue(new ControllerValue("MusicStarted", "Music Started", ValueTypes.EVENT));
		musicNode.addValue(new ControllerValue("MusicStopped", "Music Stopped", ValueTypes.EVENT));

		ControllerNode scenesNode = new ControllerNode(this, "Scenes", "Activation of Scene", "scenes");
		List<Scene> allScenes;
		try {
			allScenes = SceneManager.getAllScenes();
			for (Scene scene : allScenes) {
				scenesNode.addValue(new ControllerValue(scene.getId() + "", scene.getName(), ValueTypes.SCENE_ACTIVATION));
			}
		} catch (SQLException e) {
			log.error("Exception occured: ", e);
		}

		returnList.add(timeNode);
		returnList.add(musicNode);
		returnList.add(scenesNode);
		return returnList;
	}

	private TimerTask sunsetTask;

	private void enableSunsetSunriseEvents() throws Exception {
		// Schedule every day the sunrise/sunset events. The sunset event will
		// schedule the events for the next day
		// Location location = new Location("50.8503", "4.3517"); // Brussels
		Location location = new Location(SettingsManager.getStringValue(getIdentifier(), Settings.SUNSET_LAT.get()), SettingsManager.getStringValue(getIdentifier(), Settings.SUNSET_LONG.get()));
		SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(location, TimeZone.getDefault());
		Calendar now = Calendar.getInstance();
		Calendar sunrise = calculator.getOfficialSunriseCalendarForDate(now);
		if (!sunrise.before(now)) {
			log.debug("Scheduling sunrise at " + new SimpleDateFormat("HH:mm:ss").format(sunrise.getTime()));
			sunsetTask = Scheduler.getInstance().schedule(new TimerTask() {
				@Override
				public void run() {
					triggerSunrise();
				}
			}, sunrise.getTime(), 0);
		} else {
			Calendar sunset = calculator.getOfficialSunsetCalendarForDate(now);
			if (!sunset.before(now)) {
				log.debug("Scheduling sunset at " + new SimpleDateFormat("HH:mm:ss").format(sunset.getTime()));
				sunsetTask = Scheduler.getInstance().schedule(new TimerTask() {
					@Override
					public void run() {
						triggerSunset();
					}
				}, sunset.getTime(), 0);
			} else {
				// Schedule the sunrise of tomorrow
				now.add(Calendar.DAY_OF_MONTH, 1);
				Calendar sunriseOfTomorrow = calculator.getOfficialSunriseCalendarForDate(now);
				log.debug("Scheduling sunrise tomorrow at " + new SimpleDateFormat("HH:mm:ss").format(sunriseOfTomorrow.getTime()));
				sunsetTask = Scheduler.getInstance().schedule(new TimerTask() {
					@Override
					public void run() {
						triggerSunrise();
					}
				}, sunriseOfTomorrow.getTime(), 0);
			}
		}
	}

	private void triggerSunset() {
		log.debug("Sunset! Good night!");
		triggerEvent("Time", "Sunset");
		// Schedule sunrise event
		Location location = new Location("50.8503", "4.3517"); // Brussels
		SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(location, TimeZone.getDefault());
		Calendar tomorrow = Calendar.getInstance();
		tomorrow.add(Calendar.DAY_OF_WEEK, 1);
		Calendar sunrise = calculator.getOfficialSunriseCalendarForDate(tomorrow);
		try {
			log.debug("Scheduling sunrise tomorrow at " + new SimpleDateFormat("HH:mm:ss").format(sunrise.getTime()));
			Scheduler.getInstance().schedule(new TimerTask() {
				@Override
				public void run() {
					triggerSunrise();
				}
			}, sunrise.getTime(), 0);
		} catch (Exception e) {
			log.error("Exception occured: ", e);
		}
	}

	private void triggerSunrise() {
		log.debug("Sunrise! Good morning!");
		triggerEvent("Time", "Sunrise");

		// Schedule sunset event
		Location location = new Location("50.8503", "4.3517"); // Brussels
		SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(location, TimeZone.getDefault());
		Calendar sunset = calculator.getOfficialSunsetCalendarForDate(Calendar.getInstance());
		try {
			log.debug("Scheduling sunset " + new SimpleDateFormat("HH:mm:ss").format(sunset.getTime()));
			Scheduler.getInstance().schedule(new TimerTask() {
				@Override
				public void run() {
					triggerSunset();
				}
			}, sunset.getTime(), 0);
		} catch (Exception e) {
			log.error("Exception occured: ", e);
		}
	}

	public void triggerMusicStopped() {
		triggerEvent("Music", "MusicStopped");
	}

	public void triggerMusicStarted() {
		triggerEvent("Music", "MusicStarted");
	}

	public void triggerSceneActivated(Scene scene) {
		triggerEvent("Scenes", scene.getId() + "");
	}

	@Override
	public String getValue(ControlIdentifiers valueIdentifiers) {
		return null;
	}

	@Override
	public boolean isInitialized() {
		return true;
	}

	@Override
	public List<Setting> getSettings() {
		List<Setting> returnList = new ArrayList<Setting>();
		for (Settings s : Settings.values()) {
			returnList.add(s.setting);
		}
		return returnList;
	}

	@Override
	public void destroy() {
		super.destroy();
		if (sunsetTask != null) {
			sunsetTask.cancel();
		}
		generalControllerInstance = null;
	}
}
