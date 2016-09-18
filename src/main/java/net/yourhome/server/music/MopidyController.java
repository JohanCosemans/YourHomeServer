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
package net.yourhome.server.music;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDPlaylist;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.MPDStatusMonitor;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.event.TrackPositionListener;
import org.a0z.mpd.exception.MPDException;
import org.a0z.mpd.item.Item;
import org.a0z.mpd.item.Music;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.yourhome.common.base.enums.ControllerTypes;
import net.yourhome.common.base.enums.ValueTypes;
import net.yourhome.common.net.messagestructures.JSONMessage;
import net.yourhome.common.net.messagestructures.general.ActivationMessage;
import net.yourhome.common.net.messagestructures.general.SetValueMessage;
import net.yourhome.common.net.messagestructures.musicplayer.MusicPlayerStatusMessage;
import net.yourhome.common.net.messagestructures.musicplayer.PlaylistMessage;
import net.yourhome.common.net.messagestructures.musicplayer.PlaylistsMessage;
import net.yourhome.common.net.messagestructures.musicplayer.PlaylistsRequestMessage;
import net.yourhome.common.net.messagestructures.musicplayer.TrackProgressMessage;
import net.yourhome.common.net.model.binding.ControlIdentifiers;
import net.yourhome.server.AbstractController;
import net.yourhome.server.ControllerNode;
import net.yourhome.server.ControllerValue;
import net.yourhome.server.base.Setting;
import net.yourhome.server.base.SettingsManager;
import net.yourhome.server.base.Util;
import net.yourhome.server.net.Server;

public class MopidyController extends AbstractController implements IMusicPlayer {
	public enum Settings {

		MPD_HOST(new Setting("MPD_HOST", "Mopidy Host Address", "127.0.0.1")), MPD_PORT(new Setting("MPD_PORT", "Mopidy Port", "6600")), MPD_PASSWORD(new Setting("MPD_PASSWORD", "Mopidy Password (if any)"));

		private Setting setting;

		private Settings(Setting setting) {
			this.setting = setting;
		}

		public Setting get() {
			return this.setting;
		}
	}

	public final static String IMAGE_PATH = "/web/albumImages";
	public final static String IMAGE_WEBPATH = "/albumImages";

	private Server netWebSocketServer;
	private MPD mpd;
	private boolean isEnabled = true;
	private boolean isPlaying = false;
	private boolean isPaused = false;
	private boolean isStopped = true;
	private boolean isRandom = false;
	private MPDStatusMonitor statusMonitor;

	private MopidyController() {
		this.log = Logger.getLogger("net.yourhome.server.music.Mopidy");
		this.netWebSocketServer = Server.getInstance();
	}

	// Singleton instance
	private static volatile MopidyController mopidyControllerInstance;
	private static Object lock = new Object();

	public static MopidyController getInstance() {
		MopidyController r = MopidyController.mopidyControllerInstance;
		if (r == null) {
			synchronized (MopidyController.lock) { // while we were waiting for
													// the lock, another
				r = MopidyController.mopidyControllerInstance; // thread may
																// have
																// instantiated
				// the object
				if (r == null) {
					r = new MopidyController();
					MopidyController.mopidyControllerInstance = r;
				}
			}
		}
		return MopidyController.mopidyControllerInstance;
	}

	@Override
	public void init() {
		super.init();
		try {
			String mpdHost = SettingsManager.getStringValue(this.getIdentifier(), Settings.MPD_HOST.get());
			int mpdPort = Integer.parseInt(SettingsManager.getStringValue(this.getIdentifier(), Settings.MPD_PORT.get()));

			if (mpdHost == null || mpdHost.equals("") || mpdPort == 0) {
				throw new NumberFormatException();
			} else {

				String mpdPassword = SettingsManager.getStringValue(this.getIdentifier(), Settings.MPD_PASSWORD.get());
				try {
					if (this.mpd != null) {
						this.mpd.disconnect();
					}

					this.mpd = new CachedMPD(true);
					if (mpdPassword == null || mpdPassword.equals("")) {
						this.mpd.connect(mpdHost, mpdPort, null);
					} else {
						this.mpd.connect(mpdHost, mpdPort, mpdPassword);
					}
					MusicPlayer.registerPlayer(this);

					if (this.statusMonitor != null) {
						this.statusMonitor.giveup();
					}
					this.statusMonitor = new MPDStatusMonitor(this.mpd, 1000 / 2L, new String[] { MPDStatusMonitor.IDLE_DATABASE, MPDStatusMonitor.IDLE_MIXER, MPDStatusMonitor.IDLE_OPTIONS, MPDStatusMonitor.IDLE_OUTPUT, MPDStatusMonitor.IDLE_PLAYER, MPDStatusMonitor.IDLE_PLAYLIST, MPDStatusMonitor.IDLE_STICKER, MPDStatusMonitor.IDLE_UPDATE, });
					this.statusMonitor.addStatusChangeListener(new StatusChangeListener() {

						@Override
						public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {
							// System.out.println("volumeChanged: " +
							// mpdStatus.getVolume());
							if (MusicPlayer.getVolume() != mpdStatus.getVolume()) {
								MusicPlayer.setVolume(mpdStatus.getVolume());
								MopidyController.this.netWebSocketServer.broadcast(MusicPlayer.getVolumeMessage());
							}
						}

						@Override
						public void trackChanged(MPDStatus mpdStatus, int oldTrack) {
							// System.out.println("trackChanged: " +
							// mpdStatus.getSongPos());
							MopidyController.this.netWebSocketServer.broadcast(MopidyController.this.getPlayerStatus());
						}

						@Override
						public void stickerChanged(MPDStatus mpdStatus) {
							// System.out.println("stickerChanged");

						}

						@Override
						public void stateChanged(MPDStatus mpdStatus, int oldState) {
							MopidyController.this.updateState(mpdStatus);
							MopidyController.this.netWebSocketServer.broadcast(MopidyController.this.getPlayerStatus());
							// System.out.println("stateChanged: " +
							// mpdStatus.getState());
							if (mpdStatus.getState() == MPDStatus.STATE_PLAYING) {
								MusicPlayer.playBackStarted();
							}
						}

						@Override
						public void repeatChanged(boolean repeating) {
							// System.out.println("repeatChanged");
						}

						@Override
						public void randomChanged(boolean random) {
							// System.out.println("randomChanged");
							MopidyController.this.isRandom = random;
							MopidyController.this.netWebSocketServer.broadcast(MopidyController.this.getPlayerStatus());
						}

						@Override
						public void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion) {
							// System.out.println("playlistChanged");
							MopidyController.this.netWebSocketServer.broadcast(MopidyController.this.getPlaylist(0));
						}

						@Override
						public void libraryStateChanged(boolean updating, boolean dbChanged) {

							// System.out.println("libraryStateChanged");

						}

						@Override
						public void connectionStateChanged(boolean connected, boolean connectionLost) {
							// TODO Auto-generated method stub
							// System.out.println("connectionStateChanged");

						}
					});

					this.statusMonitor.addTrackPositionListener(new TrackPositionListener() {

						@Override
						public void trackPositionChanged(MPDStatus status) {
							// System.out.println("trackPositionChanged: " +
							// status.getElapsedTime());
							TrackProgressMessage trackProgressMessage = new TrackProgressMessage();
							trackProgressMessage.controlIdentifiers = new ControlIdentifiers(MopidyController.this.getIdentifier());
							trackProgressMessage.trackProgressPercentage = MopidyController.this.getTrackProgressPercentage();
							MopidyController.this.netWebSocketServer.broadcast(trackProgressMessage);
						}
					});

					this.statusMonitor.start();
				} catch (UnknownHostException e) {
					this.log.error("Exception occured: ", e);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					this.log.error("Exception occured: ", e);
				} catch (MPDException e) {
					// TODO Auto-generated catch block
					this.log.error("Exception occured: ", e);
				}
			}
			this.log.info("Initialized");
		} catch (NumberFormatException e) {
			this.log.info("Could not find mopidy settings. Disabling mopidy.");
			this.isEnabled = false;
		}
	}

	private int getSongIdIndex() {

		if (this.ensureConnected()) {
			return this.mpd.getStatus().getSongPos();
		}
		return 0;
	}

	@Override
	public void setVolume(int volume) {
		try {
			if (this.ensureConnected()) {
				this.mpd.setVolume(volume);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			this.log.error("Exception occured: ", e);
		} catch (MPDException e) {
			// TODO Auto-generated catch block
			this.log.error("Exception occured: ", e);
		}
	}

	@Override
	public int getVolume() {
		if (this.ensureConnected()) {
			return this.mpd.getStatus().getVolume();
		}
		return 0;
	}

	public void playStream(String streamUrl) {
		if (this.ensureConnected()) {
			try {
				this.mpd.addStream(streamUrl, false, true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				this.log.error("Exception occured: ", e);
			} catch (MPDException e) {
				// TODO Auto-generated catch block
				this.log.error("Exception occured: ", e);
			}
		}
	}

	@Override
	public JSONMessage stop() {
		if (this.ensureConnected()) {
			try {
				this.mpd.stop();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				this.log.error("Exception occured: ", e);
			} catch (MPDException e) {
				// TODO Auto-generated catch block
				this.log.error("Exception occured: ", e);
			}
		}
		return null;
	}

	public MusicPlayerStatusMessage getPlayerStatus() {
		MusicPlayerStatusMessage playerStatusMessage = new MusicPlayerStatusMessage();
		if (this.ensureConnected()) {
			Music currentSong = this.mpd.getPlaylist().getByIndex(this.mpd.getStatus().getSongPos());
			if (!this.isStopped && currentSong != null) {
				playerStatusMessage.status.artist = currentSong.getAlbumArtistOrArtist();
				playerStatusMessage.status.title = currentSong.getTitle();
				playerStatusMessage.status.trackIndex = this.mpd.getStatus().getSongPos();
				playerStatusMessage.status.imagePath = this.getAlbumImage(currentSong);
				playerStatusMessage.status.trackProgressPercentage = this.getTrackProgressPercentage();
			}

			// updateState(this.mpd.getStatus());
			playerStatusMessage.status.isPaused = this.isPaused;
			playerStatusMessage.status.isPlaying = this.isPlaying;
			playerStatusMessage.status.isStopped = this.isStopped;
			playerStatusMessage.status.randomStatus = this.isRandom;

			playerStatusMessage.broadcast = true;
			playerStatusMessage.status.trackIndex = this.getSongIdIndex();// this.mpdPlayer.getCurrentSong().getId();
			return playerStatusMessage;
		}
		return null;
	}

	private String getAlbumImage(Music song) {
		// http://ws.audioscrobbler.com/2.0/?artist=Ben+Howard&album=I+Forget+Where+We+Were&method=album.getInfo&api_key=077c9fa281240d1c38b24d48bc234940&callback=jsonp1426372798115&format=json
		try {
			String artist = URLEncoder.encode(song.getAlbumArtistOrArtist(), "UTF-8");
			String album = URLEncoder.encode(song.getAlbum(), "UTF-8");

			File albumImagefile = new File(SettingsManager.getBasePath() + MopidyController.IMAGE_PATH + "/" + artist + "_" + album + ".jpg");

			if (!albumImagefile.exists()) {

				// Get JSON info regarding the current album
				try {
					JSONObject albumInfo = Util.readJsonFromUrl("http://ws.audioscrobbler.com/2.0/?artist=" + artist + "&album=" + album + "&method=album.getInfo&api_key=077c9fa281240d1c38b24d48bc234940&format=json");

					// Parse URL from albuminfo
					JSONObject albumObj = albumInfo.getJSONObject("album");
					JSONArray imageObjArr = albumObj.getJSONArray("image");
					String imageUrl = null;
					for (int i = 0; i < imageObjArr.length(); i++) {
						JSONObject imageObj = imageObjArr.getJSONObject(i);
						if (imageObj.getString("size").equals("extralarge")) {
							imageUrl = imageObj.getString("#text");
						}
					}

					if (imageUrl != null && !imageUrl.equals("")) {
						Util.writeToFile(new URL(imageUrl).openStream(), albumImagefile);
					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					this.log.error("Exception occured: ", e);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					this.log.error("Exception occured: ", e);
				}

			}

			if (albumImagefile.exists()) {
				try {
					return MopidyController.IMAGE_WEBPATH + "/" + URLEncoder.encode(albumImagefile.getName(), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					return "";
				}
			}

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			this.log.error("Exception occured: ", e);
		}

		return "";
		// ;

		// Save album image to file
		/*
		 * if(!file.exists()) { byte[] albumImageBytes = albumImage.getBytes();
		 * if(albumImageBytes != null && albumImageBytes.length > 0) {
		 * FileOutputStream fos = new
		 * FileOutputStream(Settings.getBasePath()+this
		 * .IMAGE_PATH+"/"+imageFile); if(fos != null && albumImage != null) {
		 * fos.write(albumImageBytes); fos.close(); } } }
		 */
	}

	private void updateState(MPDStatus fromState) {
		switch (fromState.getState()) {
		case MPDStatus.STATE_PAUSED:
			this.isPaused = true;
			this.isPlaying = false;
			this.isStopped = false;
			break;
		case MPDStatus.STATE_PLAYING:
			this.isPaused = false;
			this.isPlaying = true;
			this.isStopped = false;
			break;
		case MPDStatus.STATE_STOPPED:
			this.isPaused = false;
			this.isPlaying = false;
			this.isStopped = true;
			break;
		case MPDStatus.STATE_UNKNOWN:
		}
	}

	@Override
	public JSONMessage parseNetMessage(JSONMessage message) {
		if (message instanceof ActivationMessage) {
			String action = message.controlIdentifiers.getValueIdentifier();
			if (action.equals("Stop")) {
				MusicPlayer.stopAllPlayers();
			} else if (action.equals("Next")) {
				this.next();
			} else if (action.equals("Previous")) {
				this.previous();
			} else if (action.equals("VolumeUp")) {
				return this.volumeUp();
			} else if (action.equals("VolumeDown")) {
				JSONMessage m = this.volumeDown();
				m.broadcast = true;
				return m;
			}
			return this.getPlayerStatus();
		} else if (message instanceof SetValueMessage) {
			SetValueMessage setValueMessage = (SetValueMessage) message;
			String value = setValueMessage.value.toLowerCase();
			if (setValueMessage.controlIdentifiers.getValueIdentifier().equals("PlayPause")) {
				if (value.equals("play")) {
					this.resume();
				} else if (value.equals("pause")) {
					this.pause();
				}
			} else if (setValueMessage.controlIdentifiers.getValueIdentifier().equals("Random")) {
				if (value.equals("true")) {
					this.setRandom(true);
				} else if (value.equals("false")) {
					this.setRandom(false);
				}
			} else if (setValueMessage.controlIdentifiers.getValueIdentifier().equals("Playlist")) {
				int trackIndex = Integer.parseInt(setValueMessage.value);
				this.playCurrentPlaylistItem(trackIndex);
			} else if (setValueMessage.controlIdentifiers.getValueIdentifier().equals("Playlists")) {
				int playlistId = Integer.parseInt(setValueMessage.value);
				try {
					if (this.ensureConnected()) {
						List<Item> playlists = this.mpd.getPlaylists(false);
						Item playlistItem = playlists.get(playlistId);
						List<Music> playlistContent = this.mpd.getPlaylistSongs(playlistItem.getName());
						this.mpd.getPlaylist().clear();
						this.mpd.getPlaylist().addAll(playlistContent);
						this.log.info("Set mopidy playlist to: " + playlistItem.getName());
					}
				} catch (Exception e) {
					this.log.error("Exception occured: ", e);
				}
			}
			return this.getPlayerStatus();
		} else if (message instanceof PlaylistsRequestMessage) {
			// Build return message
			PlaylistsMessage playlistsMessage = new PlaylistsMessage();
			playlistsMessage.controlIdentifiers = message.controlIdentifiers;

			try {
				if (this.ensureConnected()) {
					Collection<Item> playlists;
					int i = 0;
					playlists = this.mpd.getPlaylists(false);

					for (Item playlist : playlists) {
						PlaylistsMessage.PlaylistDescription playlistDescription = playlistsMessage.new PlaylistDescription(i, playlist.getName());
						playlistsMessage.playlists.add(playlistDescription);
						i++;
					}
				}
			} catch (Exception e) {
				this.log.error("Exception occured: ", e);
			}
			return playlistsMessage;
		}

		return null;

	}

	public double getTrackProgressPercentage() {

		if (this.ensureConnected()) {
			long totalTime = this.mpd.getStatus().getTotalTime();

			if (totalTime > 0) {
				double progressPercentage = this.mpd.getStatus().getElapsedTime() / (double) totalTime * 100.00;
				double roundedDouble = Util.round(progressPercentage, 4);
				return roundedDouble;
			}
		}
		return 0;
	}

	public PlaylistMessage getPlaylist(int maximumSongs) {

		PlaylistMessage playlistMessage = new PlaylistMessage();
		playlistMessage.controlIdentifiers = new ControlIdentifiers(this.getIdentifier());
		try {
			this.mpd.refreshDatabase();
		} catch (IOException e) {
			this.log.error("Exception occured: ", e);
		} catch (MPDException e) {
			this.log.error("Exception occured: ", e);
		}

		if (this.ensureConnected()) {
			MPDPlaylist currentPlaylist = this.mpd.getPlaylist();
			List<Music> tracks;
			tracks = currentPlaylist.getMusicList();
			for (int i = 0; i < tracks.size(); i++) {
				Music track = tracks.get(i);
				PlaylistMessage.PlaylistItem item = playlistMessage.new PlaylistItem();
				item.artist = track.getAlbumArtistOrArtist();
				item.title = track.getTitle();
				item.trackIndex = i;
				playlistMessage.playlist.add(item);
			}
		}
		return playlistMessage;
	}

	public JSONMessage playCurrentPlaylistItem(int trackIndex) {

		if (this.ensureConnected()) {
			// MPDPlaylist currentPlaylist = this.mpd.getPlaylist();
			// List<Music> songs = currentPlaylist.getMusicList();
			try {
				if (trackIndex < 0) {
					MusicPlayer.stopAllPlayers(this);
					this.mpd.skipToPosition(0);
				} else {
					MusicPlayer.stopAllPlayers(this);
					this.mpd.skipToPosition(trackIndex);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				this.log.error("Exception occured: ", e);
			} catch (MPDException e) {
				// TODO Auto-generated catch block
				this.log.error("Exception occured: ", e);
			}
		}
		return null;
	}

	public void pause() {
		try {
			if (this.ensureConnected() && this.isPlaying) {
				this.isPlaying = false;
				this.isStopped = false;
				this.isPaused = true;
				this.mpd.pause();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			this.log.error("Exception occured: ", e);
		} catch (MPDException e) {
			// TODO Auto-generated catch block
			this.log.error("Exception occured: ", e);
		}
	}

	public void resume() {
		try {

			if (this.ensureConnected()) {
				this.isPlaying = true;
				this.isPaused = false;
				this.isStopped = false;
				this.mpd.play();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			this.log.error("Exception occured: ", e);
		} catch (MPDException e) {
			// TODO Auto-generated catch block
			this.log.error("Exception occured: ", e);
		}
	}

	private boolean ensureConnected() {
		if (this.mpd == null | (this.mpd != null && !this.mpd.isConnected())) {
			this.init();
		}

		if (this.mpd != null && this.mpd.isConnected()) {
			return true;
		}

		return false;
	}

	public void next() {
		try {

			if (this.ensureConnected()) {
				this.mpd.next();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			this.log.error("Exception occured: ", e);
		} catch (MPDException e) {
			// TODO Auto-generated catch block
			this.log.error("Exception occured: ", e);
		}
	}

	public void previous() {
		try {

			if (this.ensureConnected()) {
				this.mpd.previous();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			this.log.error("Exception occured: ", e);
		} catch (MPDException e) {
			// TODO Auto-generated catch block
			this.log.error("Exception occured: ", e);
		}
	}

	public void setRandom(boolean onOff) {
		try {
			this.isRandom = onOff;
			if (this.ensureConnected()) {
				this.mpd.setRandom(onOff);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			this.log.error("Exception occured: ", e);
		} catch (MPDException e) {
			// TODO Auto-generated catch block
			this.log.error("Exception occured: ", e);
		}
	}

	public JSONMessage volumeUp() {
		int currentVolume = MusicPlayer.getVolume();
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
		int currentVolume = MusicPlayer.getVolume();

		if (currentVolume > 0) {
			if (currentVolume - 5 < 10) {
				MusicPlayer.setVolume(currentVolume - 1);
			} else {
				MusicPlayer.setVolume(currentVolume - 5);
			}
		}
		// }
		return MusicPlayer.getVolumeMessage();

	}

	@Override
	public String getIdentifier() {
		// TODO Auto-generated method stub
		return ControllerTypes.SPOTIFY.convert();
	}

	@Override
	public String getName() {
		return "Mopidy";
	}

	@Override
	public List<JSONMessage> initClient() {
		if (this.isInitialized()) {
			List<JSONMessage> returnList = new ArrayList<JSONMessage>();
			returnList.add(this.getPlayerStatus());
			returnList.add(this.getPlaylist(500));
			return returnList;
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		return this.isEnabled;
	}

	@Override
	public List<ControllerNode> getNodes() {

		List<ControllerNode> returnList = new ArrayList<ControllerNode>();
		ControllerNode commandsNode = new ControllerNode(this, "MopidyCommands", "Mopidy Commands", "");
		commandsNode.addValue(new ControllerValue("PlayPause", "Play / Pause", ValueTypes.MUSIC_PLAY_PAUSE));
		commandsNode.addValue(new ControllerValue("Stop", "Stop", ValueTypes.MUSIC_ACTION));
		commandsNode.addValue(new ControllerValue("Next", "Next", ValueTypes.MUSIC_ACTION));
		commandsNode.addValue(new ControllerValue("Previous", "Previous", ValueTypes.MUSIC_ACTION));
		commandsNode.addValue(new ControllerValue("Random", "Random", ValueTypes.MUSIC_RANDOM));
		commandsNode.addValue(new ControllerValue("VolumeUp", "Volume Up", ValueTypes.MUSIC_ACTION));
		commandsNode.addValue(new ControllerValue("VolumeDown", "Volume Down", ValueTypes.MUSIC_ACTION));
		commandsNode.addValue(new ControllerValue("AlbumImage", "Album Image", ValueTypes.MUSIC_ALBUM_IMAGE));
		commandsNode.addValue(new ControllerValue("Playlist", "Playlist", ValueTypes.MUSIC_PLAYLIST));
		commandsNode.addValue(new ControllerValue("Playlists", "Playlists", ValueTypes.MUSIC_PLAYLISTS));
		commandsNode.addValue(new ControllerValue("Progress", "Progress", ValueTypes.MUSIC_PROGRESS));
		commandsNode.addValue(new ControllerValue("TrackDisplay", "Title / Artist", ValueTypes.MUSIC_TRACK_DISPLAY));
		returnList.add(commandsNode);

		return returnList;
	}

	@Override
	public String getValueName(ControlIdentifiers valueIdentifier) {
		return "Unkown";
	}

	@Override
	public List<ControllerNode> getTriggers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getValue(ControlIdentifiers valueIdentifiers) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isInitialized() {
		boolean isNotConnected = this.mpd == null | (this.mpd != null && !this.mpd.isConnected());
		return !isNotConnected;
	}

	@Override
	public List<Setting> getSettings() {
		List<Setting> returnList = new ArrayList<Setting>();
		for (Settings s : Settings.values()) {
			returnList.add(s.setting);
		}
		return returnList;
	}

}
