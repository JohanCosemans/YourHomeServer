package net.yourhome.server.radio;

import java.util.EventListener;
import java.util.List;
import java.util.ListIterator;

public class BasicPlayerManager implements EventListener {
	private static BasicPlayerManager basicPlayerManager;
	private List playList;
	// private IMP3File currentlyPlaying;
	private ListIterator listIterator;
	private BasicPlayer basicPlayer;

	private BasicPlayerManager() {
		super();
		basicPlayer = new BasicPlayer(this);
	}

	public static BasicPlayerManager getPlayerManager() {
		if (basicPlayerManager == null) {
			basicPlayerManager = new BasicPlayerManager();
		}
		return basicPlayerManager;
	}

	public List getPlaylist() {
		return playList;
	}

	public synchronized void playNext() {
		// if currently playing a song, stop playing the current song
		/*
		 * if(basicPlayer.getStatus()==BasicPlayer.PLAYING ||
		 * basicPlayer.getStatus()==BasicPlayer.PAUSED){
		 * basicPlayer.stopPlayback(); } // if we're at the end of the list loop
		 * around to the front by getting a new iterator
		 * if(!listIterator.hasNext()){ listIterator = playList.listIterator();
		 * } // iterate to the next song and assign it to currentlyPlaying
		 * currentlyPlaying = (IMP3File)listIterator.next(); try {
		 * basicPlayer.setDataSource(new File(currentlyPlaying.getFile()));
		 * basicPlayer.startPlayback(); } catch (UnsupportedAudioFileException
		 * e) { // TODO Auto-generated catch block log.error("Exception occured: ", e); } catch
		 * (LineUnavailableException e) { // TODO Auto-generated catch block
		 * log.error("Exception occured: ", e); } catch (IOException e) { // TODO Auto-generated
		 * catch block log.error("Exception occured: ", e); }
		 */
	}

	public synchronized void playPrevious() {
		// stop playing the current song
		// if we're at the begining get a new iterator starting from the end of
		// the list
		// assign the song to currentlyPlaying
		// start playing the song
	}

	public synchronized void setPlaylist(List playlist) {
		/*
		 * if(playlist != null && playlist.size()>0){ // update playList with
		 * new list this.playList = playlist; // get a new list iterator
		 * this.listIterator = this.playList.listIterator(); // prime
		 * currentlyPlaying with the first song in the list. currentlyPlaying =
		 * (IMP3File)listIterator.next(); // prime the player with the first
		 * song try { basicPlayer.setDataSource(new
		 * File(currentlyPlaying.getFile())); } catch
		 * (UnsupportedAudioFileException e) { // TODO Auto-generated catch
		 * block log.error("Exception occured: ", e); } catch (LineUnavailableException e) { //
		 * TODO Auto-generated catch block log.error("Exception occured: ", e); } catch
		 * (IOException e) { // TODO Auto-generated catch block
		 * log.error("Exception occured: ", e); } }
		 */
	}

	public synchronized void togglePlaying() {
		// if isPlaying is true, song is curently playing
		switch (basicPlayer.getStatus()) {
		// if stopped or ready start playing
		case BasicPlayer.STOPPED:
		case BasicPlayer.READY:
			basicPlayer.stopPlayback();
			basicPlayer.startPlayback();
			break;
		// if paused resume playing
		case BasicPlayer.PAUSED:
			basicPlayer.resumePlayback();
			break;
		// if playing then pause
		case BasicPlayer.PLAYING:
			basicPlayer.pausePlayback();
			break;
		}
	}

	public boolean isCurrentlyPlaying() {
		return basicPlayer.getStatus() == BasicPlayer.PLAYING;
	}

	/*
	 * public IMP3File getCurrentlyPlaying() { return currentlyPlaying; }-
	 */

	public void fileCompletedPlaying() {
		playNext();
	}

}
