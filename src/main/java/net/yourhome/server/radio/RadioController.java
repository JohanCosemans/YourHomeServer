package net.yourhome.server.radio;

import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.log4j.Logger;

import javazoom.jl.decoder.JavaLayerException;
import net.yourhome.common.base.enums.ControllerTypes;
import net.yourhome.common.base.enums.ValueTypes;
import net.yourhome.common.net.messagestructures.JSONMessage;
import net.yourhome.common.net.messagestructures.general.ActivationMessage;
import net.yourhome.common.net.messagestructures.general.ClientMessageMessage;
import net.yourhome.common.net.messagestructures.radio.RadioOnOffMessage;
import net.yourhome.common.net.model.binding.ControlIdentifiers;
import net.yourhome.server.AbstractController;
import net.yourhome.server.ControllerNode;
import net.yourhome.server.ControllerValue;
import net.yourhome.server.base.DatabaseConnector;
import net.yourhome.server.base.Setting;
import net.yourhome.server.music.IMusicPlayer;
import net.yourhome.server.music.MusicPlayer;
import net.yourhome.server.net.Server;

public class RadioController extends AbstractController implements IMusicPlayer {

	private static volatile RadioController radioController;
	private static Object lock = new Object();

	private BasicPlayer radioPlayer;
	private boolean isPlaying = false;
	private RadioChannel currentRadioChannel = null;
	private Map<Integer, RadioChannel> radioChannels;

	private RadioController() {
		log = Logger.getLogger("net.yourhome.server.radio.Radio");
		
		// Read all the radio channels from the db
		radioChannels = new HashMap<Integer, RadioChannel>();
		readRadioStations();
		MusicPlayer.registerPlayer(this);
	}

	public void readRadioStations() {
		radioChannels.clear();

		String sql = "SELECT * from main.Radio_Channels";
		ResultSet allChannelsResult = null;
		try {
			allChannelsResult = DatabaseConnector.getInstance().executeSelect(sql);

			while (allChannelsResult.next()) {
				int channelId = allChannelsResult.getInt("id");
				RadioChannel radioChannel = new RadioChannel(channelId, allChannelsResult.getString("channelName"), allChannelsResult.getString("channelUrl"));
				this.radioChannels.put(channelId, radioChannel);
			}
		} catch (SQLException e) {
			log.error("Exception occured: ", e);
		} finally {
			try {
				if (allChannelsResult != null) {
					allChannelsResult.getStatement().close();
					allChannelsResult.close();
				}
			} catch (SQLException e) {
				log.error("Exception occured: ", e);
			}
		}
	}

	public static RadioController getInstance() {
		RadioController r = radioController;
		if (r == null) {
			synchronized (lock) { // while we were waiting for the lock, another
				r = radioController; // thread may have instantiated the object
				if (r == null) {
					r = new RadioController();
					radioController = r;
				}
			}
		}

		return radioController;
	}

	@Override
	public JSONMessage parseNetMessage(JSONMessage message) {
		if (message instanceof RadioOnOffMessage) {
			RadioOnOffMessage radioOnOffMessage = (RadioOnOffMessage) message;
			this.currentRadioChannel = this.getRadioChannel(Integer.parseInt(radioOnOffMessage.controlIdentifiers.getValueIdentifier()));
			if (radioOnOffMessage.status) {
				try {
					return this.playRadioStream();
				} catch (IOException e) {
					log.error("Exception occured: ", e);
				} catch (JavaLayerException e) {
					log.error("Exception occured: ", e);
				} catch (UnsupportedAudioFileException e) {
					log.error("Exception occured: ", e);
				} catch (LineUnavailableException e) {
					log.error("Exception occured: ", e);
				}
			} else {
				JSONMessage returnMessage = this.stop();
				ClientMessageMessage informClientsMessage = new ClientMessageMessage();
				informClientsMessage.broadcast = true;
				informClientsMessage.messageContent = "Radio " + currentRadioChannel.channelName + " stopped";
				Server.getInstance().broadcast(informClientsMessage);
				return returnMessage;
			}
		} else if (message instanceof ActivationMessage) {
			if (message.controlIdentifiers.getNodeIdentifier().equals("RadioChannels")) {

				this.currentRadioChannel = this.getRadioChannel(Integer.parseInt(message.controlIdentifiers.getValueIdentifier()));
				try {
					if (this.isPlaying) {
						JSONMessage returnMessage = this.stop();
						ClientMessageMessage informClientsMessage = new ClientMessageMessage();
						informClientsMessage.broadcast = true;
						informClientsMessage.messageContent = "Radio " + currentRadioChannel.channelName + " stopped";
						Server.getInstance().broadcast(informClientsMessage);
					} else {
						Server.getInstance().broadcast(this.playRadioStream());
					}
				} catch (IOException e) {
					log.error("Exception occured: ", e);
				} catch (JavaLayerException e) {
					log.error("Exception occured: ", e);
				} catch (UnsupportedAudioFileException e) {
					log.error("Exception occured: ", e);
				} catch (LineUnavailableException e) {
					log.error("Exception occured: ", e);
				}
			} else {
				String action = message.controlIdentifiers.getValueIdentifier();
				if (action.equals("VolumeUp")) {
					return this.volumeUp();
				} else if (action.equals("VolumeDown")) {
					return this.volumeDown();
				} else if (action.equals("Stop")) {
					return this.stop();
				}
			}
		}
		return null;
	}

	// Should only be called from MusicPlayer class!!
	@Override
	public void setVolume(int volume) {
		if (radioPlayer != null) {
			radioPlayer.setVolume(volume);
		}
	}

	@Override
	public JSONMessage stop() {
		if (radioPlayer != null) {
			radioPlayer.stopPlayback();
			this.isPlaying = false;
			MusicPlayer.stopAllPlayers(this);
		}

		RadioOnOffMessage returnMessage = getRadioMessage();
		returnMessage.broadcast = true;
		return returnMessage;
	}

	/**
	 * Read sampled audio data from the specified URL and play it
	 * 
	 * @throws JavaLayerException
	 * @throws IOException
	 * @throws LineUnavailableException
	 * @throws UnsupportedAudioFileException
	 */
	public void playRadioStream(RadioChannel r) throws IOException, JavaLayerException, UnsupportedAudioFileException, LineUnavailableException {
		this.currentRadioChannel = r;
		this.playRadioStream();
	}

	public JSONMessage playRadioStream() throws IOException, JavaLayerException, UnsupportedAudioFileException, LineUnavailableException {
		boolean firstRun = false;
		if (radioPlayer == null) {
			radioPlayer = new BasicPlayer();
			firstRun = true;
		}

		if (this.isPlaying) {
			radioPlayer.stopPlayback();
		} else {
			try {
				MusicPlayer.stopAllPlayers(this);
			} catch (Exception e) {
			}
		}
		if (currentRadioChannel != null) {
			radioPlayer.stopPlayback();
			radioPlayer.setDataSource(new URL(currentRadioChannel.channelUrl));
			MusicPlayer.playBackStarted();
			if (radioPlayer.startPlayback()) {
				this.isPlaying = true;
				ClientMessageMessage informClientsMessage = new ClientMessageMessage();
				informClientsMessage.broadcast = true;
				informClientsMessage.messageContent = "Radio " + currentRadioChannel.channelName + " started";
				Server.getInstance().broadcast(informClientsMessage);
			}
		}
		// MopidyController.getInstance().playStream(currentRadioChannel.channelUrl);
		return getRadioMessage();
	}

	private RadioOnOffMessage getRadioMessage() {
		RadioOnOffMessage radioActivationMessage = new RadioOnOffMessage();
		if (currentRadioChannel != null) {
			radioActivationMessage.controlIdentifiers = new ControlIdentifiers(getIdentifier(), "RadioChannels", currentRadioChannel.id + "");
		} else {
			radioActivationMessage.controlIdentifiers = new ControlIdentifiers(getIdentifier());
		}
		radioActivationMessage.status = isPlaying;
		radioActivationMessage.broadcast = true;

		return radioActivationMessage;
	}
	/*
	 * public RadioStatusMessage getPlayerStatus() { RadioStatusMessage
	 * radioStatusMessage = new RadioStatusMessage();
	 * radioStatusMessage.broadcast = true; if(this.isPlaying &&
	 * currentRadioChannel != null) { this.isPlaying = true;
	 * radioStatusMessage.channelName = currentRadioChannel.channelName;
	 * radioStatusMessage.channelId = currentRadioChannel.id; }
	 * radioStatusMessage.isPlaying = this.isPlaying; return radioStatusMessage;
	 * }
	 */

	public JSONMessage volumeUp() {
		/*
		 * if (this.radioPlayer != null) {
		 */
		int currentVolume = (MusicPlayer.getVolume());
		int newVolume = 0;

		if (currentVolume > 9) {
			newVolume = currentVolume + 5;
			if (newVolume >= 100) {
				newVolume = 100;
			}
		} else {
			newVolume = currentVolume + 1;
		}

		MusicPlayer.setVolume(newVolume);
		// }
		return MusicPlayer.getVolumeMessage();
	}

	public JSONMessage volumeDown() {
		// if (this.radioPlayer != null)
		// {
		int currentVolume = (MusicPlayer.getVolume());
		int newVolume = 0;

		if (currentVolume > 0) {
			if (currentVolume - 5 < 10) {
				newVolume = currentVolume - 1;
			} else {
				newVolume = currentVolume - 5;
			}
		}
		MusicPlayer.setVolume(newVolume);
		// }
		return MusicPlayer.getVolumeMessage();
	}

	// Should not be called directly: use MusicPlayer
	@Override
	public int getVolume() {
		if (this.radioPlayer != null) {
			return this.radioPlayer.getVolume();
		}
		return -1;
	}

	/*
	 * private VolumeMessage getVolumeMessage() { VolumeMessage message = new
	 * VolumeMessage();
	 * 
	 * //if (this.radioPlayer != null) //{ message.volume = (int)
	 * (MusicPlayer.getVolume()); message.broadcast = true; //} return message;
	 * }
	 */
	public String createRadioChannel(RadioChannel radioChannel) throws SQLException {
		String sql = "insert into main.Radio_Channels ('channelUrl', 'channelName')" + " VALUES ('" + radioChannel.channelUrl + "','" + radioChannel.channelName + "')";
		int id = DatabaseConnector.getInstance().insertConfigurationValue(sql);

		if (id > 0) {
			radioChannel.id = id;
			this.radioChannels.put(id, radioChannel);
		}

		return "{ \"id\" : " + id + " }";
	}

	public boolean deleteRadioChannel(int radioChannelId) throws SQLException {
		String sql = "delete from main.Radio_Channels where id = '" + radioChannelId + "'";
		boolean returnBool = DatabaseConnector.getInstance().executeQuery(sql);
		if (returnBool) {
			readRadioStations();
		}
		return returnBool;
	}

	public RadioChannel getRadioChannel(int channelId) {
		return this.radioChannels.get(channelId);
	}

	public class RadioCommand {
		public String commandName;
		public String command;

		public RadioCommand(String commandName, String command) {
			this.commandName = commandName;
			this.command = command;
		}

		public RadioCommand(RadioChannel radioChannel) {
			this.commandName = radioChannel.channelName;
			this.command = "RadioOnOff_" + radioChannel.id;
		}
	}

	public class RadioChannel {
		public int id;
		public String channelName;
		public String channelUrl;

		public RadioChannel(int id, String channelName, String channelUrl) {
			this(channelName, channelUrl);
			this.id = id;
		}

		public RadioChannel(String channelName, String channelUrl) {
			this.channelName = channelName;
			this.channelUrl = channelUrl;
		}
	}

	@Override
	public String getIdentifier() {
		// TODO Auto-generated method stub
		return ControllerTypes.RADIO.convert();
	}

	@Override
	public String getName() {
		return "Radio";
	}

	@Override
	public void init() {
		super.init();
		log.info("Initialized");
	}

	@Override
	public List<JSONMessage> initClient() {
		List<JSONMessage> returnList = new ArrayList<JSONMessage>();
		// if(isPlaying) {
		returnList.add(getRadioMessage());
		// }
		return returnList;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public List<ControllerNode> getNodes() {
		List<ControllerNode> returnList = new ArrayList<ControllerNode>();
		ControllerNode commandsNode = new ControllerNode(this, "RadioCommands", "Radio Commands", "");
		commandsNode.addValue(new ControllerValue("VolumeDown", "Volume Down", ValueTypes.MUSIC_ACTION));
		commandsNode.addValue(new ControllerValue("VolumeUp", "Volume Up", ValueTypes.MUSIC_ACTION));
		commandsNode.addValue(new ControllerValue("Stop", "Stop", ValueTypes.MUSIC_ACTION));
		returnList.add(commandsNode);

		ControllerNode channelsNode = new ControllerNode(this, "RadioChannels", "Radio Channels", "");
		// Add all radio channels
		for (Map.Entry<Integer, RadioChannel> entry : radioChannels.entrySet()) {
			channelsNode.addValue(new ControllerValue("" + entry.getValue().id, entry.getValue().channelName, ValueTypes.RADIO_STATION));
		}
		returnList.add(channelsNode);

		return returnList;
	}

	@Override
	public String getValueName(ControlIdentifiers valueIdentifier) {
		try {
			RadioChannel radioChannel = this.getRadioChannel(Integer.parseInt(valueIdentifier.getValueIdentifier()));
			if (radioChannel != null) {
				return radioChannel.channelName;
			}
		} catch (NumberFormatException e) {
		}

		return "Unkown";
	}

	@Override
	public List<ControllerNode> getTriggers() {
		return null;
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
		// TODO Auto-generated method stub
		return null;
	}

}
