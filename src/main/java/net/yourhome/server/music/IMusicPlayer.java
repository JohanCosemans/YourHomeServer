package net.yourhome.server.music;

import net.yourhome.common.net.messagestructures.JSONMessage;

public interface IMusicPlayer {
	public void setVolume(int volume);

	public int getVolume();

	public JSONMessage stop();
}
