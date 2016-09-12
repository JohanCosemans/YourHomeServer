package net.yourhome.server.music;

import java.util.ArrayList;
import java.util.List;

import net.yourhome.common.base.enums.ControllerTypes;
import net.yourhome.common.net.messagestructures.JSONMessage;
import net.yourhome.common.net.messagestructures.general.ClientMessageMessage;
import net.yourhome.common.net.model.binding.ControlIdentifiers;
import net.yourhome.server.base.GeneralController;
import net.yourhome.server.net.Server;

public class MusicPlayer {

	public final static int DEFAULT_VOLUME = 50;
	private static int volume;
	private static List<IMusicPlayer> musicPlayers = new ArrayList<IMusicPlayer>();

	// private static List<MusicStartedTrigger> musicStartedListeners = new
	// ArrayList<MusicStartedTrigger>();
	static {
		volume = DEFAULT_VOLUME;
	}

	public static int getVolume() {
		return volume;
	}

	public static void setVolume(int newVolume) {
		volume = newVolume;
		for (IMusicPlayer player : musicPlayers) {
			player.setVolume(volume);
		}
	}

	public static void registerPlayer(IMusicPlayer player) {
		if (!musicPlayers.contains(player)) {
			musicPlayers.add(player);
			player.setVolume(getVolume());
		}
	}

	public static void stopAllPlayers(IMusicPlayer except) {
		for (IMusicPlayer player : musicPlayers) {
			if (except != player) {
				JSONMessage stopMessage = player.stop();
				if (stopMessage != null && stopMessage.broadcast == true) {
					Server.getInstance().broadcast(player.stop());
				}
			}
		}
		GeneralController.getInstance().triggerMusicStopped();
	}

	public static void playBackStarted() {
		GeneralController.getInstance().triggerMusicStarted();
		/*
		 * for(MusicStartedTrigger m : musicStartedListeners) { m.trigger(); }
		 */
	}

	public static ClientMessageMessage getVolumeMessage() {
		ClientMessageMessage message = new ClientMessageMessage();
		message.broadcast = true;
		message.controlIdentifiers = new ControlIdentifiers(ControllerTypes.GENERAL.convert());
		message.messageContent = "Volume changed to " + getVolume();
		return message;
	}

	public static void stopAllPlayers() {
		stopAllPlayers(null);
	}
	/*
	 * public static void addMusicStartedListener(MusicStartedTrigger m) {
	 * musicStartedListeners.add(m); } public static void
	 * removeMusicStartedListener(MusicStartedTrigger m) {
	 * musicStartedListeners.remove(m); }
	 */

}
