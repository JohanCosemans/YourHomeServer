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
 * THIS SOFTWARE IS PROVIDED BY THE NETBSD FOUNDATION, INC. AND CONTRIBUTORS
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
		this.basicPlayer = new BasicPlayer(this);
	}

	public static BasicPlayerManager getPlayerManager() {
		if (BasicPlayerManager.basicPlayerManager == null) {
			BasicPlayerManager.basicPlayerManager = new BasicPlayerManager();
		}
		return BasicPlayerManager.basicPlayerManager;
	}

	public List getPlaylist() {
		return this.playList;
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
		 * e) { // TODO Auto-generated catch block
		 * log.error("Exception occured: ", e); } catch
		 * (LineUnavailableException e) { // TODO Auto-generated catch block
		 * log.error("Exception occured: ", e); } catch (IOException e) { //
		 * TODO Auto-generated catch block log.error("Exception occured: ", e);
		 * }
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
		 * block log.error("Exception occured: ", e); } catch
		 * (LineUnavailableException e) { // TODO Auto-generated catch block
		 * log.error("Exception occured: ", e); } catch (IOException e) { //
		 * TODO Auto-generated catch block log.error("Exception occured: ", e);
		 * } }
		 */
	}

	public synchronized void togglePlaying() {
		// if isPlaying is true, song is curently playing
		switch (this.basicPlayer.getStatus()) {
		// if stopped or ready start playing
		case BasicPlayer.STOPPED:
		case BasicPlayer.READY:
			this.basicPlayer.stopPlayback();
			this.basicPlayer.startPlayback();
			break;
		// if paused resume playing
		case BasicPlayer.PAUSED:
			this.basicPlayer.resumePlayback();
			break;
		// if playing then pause
		case BasicPlayer.PLAYING:
			this.basicPlayer.pausePlayback();
			break;
		}
	}

	public boolean isCurrentlyPlaying() {
		return this.basicPlayer.getStatus() == BasicPlayer.PLAYING;
	}

	/*
	 * public IMP3File getCurrentlyPlaying() { return currentlyPlaying; }-
	 */

	public void fileCompletedPlaying() {
		this.playNext();
	}

}
