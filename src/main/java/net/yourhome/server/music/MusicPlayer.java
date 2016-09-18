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
		MusicPlayer.volume = MusicPlayer.DEFAULT_VOLUME;
	}

	public static int getVolume() {
		return MusicPlayer.volume;
	}

	public static void setVolume(int newVolume) {
		MusicPlayer.volume = newVolume;
		for (IMusicPlayer player : MusicPlayer.musicPlayers) {
			player.setVolume(MusicPlayer.volume);
		}
	}

	public static void registerPlayer(IMusicPlayer player) {
		if (!MusicPlayer.musicPlayers.contains(player)) {
			MusicPlayer.musicPlayers.add(player);
			player.setVolume(MusicPlayer.getVolume());
		}
	}

	public static void stopAllPlayers(IMusicPlayer except) {
		for (IMusicPlayer player : MusicPlayer.musicPlayers) {
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
		message.messageContent = "Volume changed to " + MusicPlayer.getVolume();
		return message;
	}

	public static void stopAllPlayers() {
		MusicPlayer.stopAllPlayers(null);
	}
	/*
	 * public static void addMusicStartedListener(MusicStartedTrigger m) {
	 * musicStartedListeners.add(m); } public static void
	 * removeMusicStartedListener(MusicStartedTrigger m) {
	 * musicStartedListeners.remove(m); }
	 */

}
