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
package net.yourhome.server.radio;

/**
 * BasicPlayer.
 *
 *-----------------------------------------------------------------------
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *----------------------------------------------------------------------
 */
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.StringTokenizer;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
//import javazoom.Util.Debug;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.log4j.Logger;

/**
 * BasicPlayer implements basics features of a player. The playback is done with
 * a thread. BasicPlayer is the result of jlGui 0.5 from JavaZOOM and
 * BaseAudioStream from Matthias Pfisterer JavaSound examples.
 *
 * @author E.B from JavaZOOM
 *
 *         Homepage : http://www.javazoom.net
 *
 */
public class BasicPlayer implements Runnable {

	private static final int EXTERNAL_BUFFER_SIZE = 4000 * 4;
	private static Logger log = Logger.getLogger(BasicPlayer.class);

	private Thread m_thread = null;
	private Object m_dataSource;
	private AudioInputStream m_audioInputStream;
	private AudioFileFormat m_audioFileFormat;
	private SourceDataLine m_line;
	private FloatControl m_gainControl;
	private FloatControl m_panControl;
	private BasicPlayerManager basicPlayerManager = null;
	private int volume = 100;

	/**
	 * These variables are used to distinguish stopped, paused, playing states.
	 * We need them to control Thread.
	 */
	public static final int PAUSED = 1;
	public static final int PLAYING = 0;
	public static final int STOPPED = 2;
	public static final int READY = 3;
	private int m_status = BasicPlayer.READY;
	private long doSeek = -1;
	private File _file = null;

	// private BasicPlayerListener m_bpl = null;

	/**
	 * Constructs a Basic Player.
	 */
	public BasicPlayer() {
		this.m_dataSource = null;
		this.m_audioInputStream = null;
		this.m_audioFileFormat = null;
		this.m_line = null;
		this.m_gainControl = null;
		this.m_panControl = null;
	}

	/**
	 * Constructs a Basic Player with a BasicPlayerListener.
	 */
	// public BasicPlayer(BasicPlayerListener bpl)
	// {
	// this();
	// m_bpl = bpl;
	// }

	/**
	 * Constructs a Basic Player with a BasicPlayerManager.
	 */
	public BasicPlayer(BasicPlayerManager bpm) {
		this();
		this.basicPlayerManager = bpm;
	}

	/**
	 * Returns BasicPlayer status.
	 */
	public int getStatus() {
		return this.m_status;
	}

	/**
	 * Sets the data source as a file.
	 */
	public void setDataSource(File file) throws UnsupportedAudioFileException, LineUnavailableException, IOException {
		if (file != null) {
			this.m_dataSource = file;
			this.initAudioInputStream();
		}
	}

	/**
	 * Sets the data source as an url.
	 */
	public void setDataSource(URL url) throws UnsupportedAudioFileException, LineUnavailableException, IOException {
		if (url != null) {
			this.m_dataSource = url;
			this.initAudioInputStream();
		}
	}

	/**
	 * Sets the data source as an InputStream
	 */
	// public void setDataSource(InputStream inputStream) throws
	// UnsupportedAudioFileException, LineUnavailableException, IOException
	// {
	// if (inputStream != null)
	// {
	// m_dataSource = inputStream;
	// initAudioInputStream();
	// }
	// }

	/**
	 * Inits Audio ressources from the data source.<br>
	 * - AudioInputStream <br>
	 * - AudioFileFormat
	 */
	private void initAudioInputStream() throws UnsupportedAudioFileException, LineUnavailableException, IOException {
		if (this.m_dataSource instanceof URL) {
			this.initAudioInputStream((URL) this.m_dataSource);
		} else if (this.m_dataSource instanceof File) {
			this.initAudioInputStream((File) this.m_dataSource);
		}
		// else if (m_dataSource instanceof InputStream)
		// {
		// initAudioInputStream( (InputStream) m_dataSource);
		// }
	}

	/**
	 * Inits Audio ressources from file.
	 */
	private void initAudioInputStream(File file) throws UnsupportedAudioFileException, IOException {
		this._file = file;
		this.m_audioInputStream = AudioSystem.getAudioInputStream(file);
		this.m_audioFileFormat = AudioSystem.getAudioFileFormat(file);
	}

	/**
	 * Inits Audio ressources from URL.
	 */
	private void initAudioInputStream(URL url) throws UnsupportedAudioFileException, IOException {
		// m_audioInputStream =
		// AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED,AudioSystem.getAudioInputStream(url));
		this.m_audioInputStream = AudioSystem.getAudioInputStream(url);
		this.m_audioFileFormat = AudioSystem.getAudioFileFormat(url);
	}

	/**
	 * Inits Audio ressources from InputStream.
	 */
	// private void initAudioInputStream(InputStream inputStream) throws
	// UnsupportedAudioFileException, IOException
	// {
	// m_audioInputStream = AudioSystem.getAudioInputStream(inputStream);
	// m_audioFileFormat = AudioSystem.getAudioFileFormat(inputStream);
	// }

	/**
	 * Inits Audio ressources from AudioSystem.<br>
	 * DateSource must be present.
	 */
	protected void initLine() throws LineUnavailableException {
		if (this.m_line == null) {
			this.createLine();
			this.trace(1, this.getClass().getName(), "Create Line OK ");
			this.openLine();
		} else {
			AudioFormat lineAudioFormat = this.m_line.getFormat();
			AudioFormat audioInputStreamFormat = this.m_audioInputStream == null ? null : this.m_audioInputStream.getFormat();
			if (!lineAudioFormat.equals(audioInputStreamFormat)) {
				this.m_line.close();
				this.openLine();
			}
		}
	}

	/**
	 * Inits a DateLine.<br>
	 *
	 * We check if the line supports Volume and Pan controls.
	 *
	 * From the AudioInputStream, i.e. from the sound file, we fetch information
	 * about the format of the audio data. These information include the
	 * sampling frequency, the number of channels and the size of the samples.
	 * There information are needed to ask JavaSound for a suitable output line
	 * for this audio file. Furthermore, we have to give JavaSound a hint about
	 * how big the internal buffer for the line should be. Here, we say
	 * AudioSystem.NOT_SPECIFIED, signaling that we don't care about the exact
	 * size. JavaSound will use some default value for the buffer size.
	 */
	private void createLine() throws LineUnavailableException {
		if (this.m_line == null) {
			AudioFormat sourceFormat = this.m_audioInputStream.getFormat();
			this.trace(1, this.getClass().getName(), "Source format : ", sourceFormat.toString());
			AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(), sourceFormat.getChannels() * 2, sourceFormat.getSampleRate(), false);

			this.trace(1, this.getClass().getName(), "Target format: " + targetFormat);
			this.m_audioInputStream = AudioSystem.getAudioInputStream(targetFormat, this.m_audioInputStream);
			AudioFormat audioFormat = this.m_audioInputStream.getFormat();
			this.trace(1, this.getClass().getName(), "Create Line : ", audioFormat.toString());
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);
			this.m_line = (SourceDataLine) AudioSystem.getLine(info);

			/*-- Display supported controls --*/
			Control[] c = this.m_line.getControls();
			for (Control element : c) {
				this.trace(2, this.getClass().getName(), "Controls : " + element.toString());
			}
			/*-- Is Gain Control supported ? --*/
			if (this.m_line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
				this.m_gainControl = (FloatControl) this.m_line.getControl(FloatControl.Type.MASTER_GAIN);
				this.trace(1, this.getClass().getName(), "Master Gain Control : [" + this.m_gainControl.getMinimum() + "," + this.m_gainControl.getMaximum() + "]", "" + this.m_gainControl.getPrecision());
			}

			/*-- Is Pan control supported ? --*/
			if (this.m_line.isControlSupported(FloatControl.Type.PAN)) {
				this.m_panControl = (FloatControl) this.m_line.getControl(FloatControl.Type.PAN);
				this.trace(1, this.getClass().getName(), "Pan Control : [" + this.m_panControl.getMinimum() + "," + this.m_panControl.getMaximum() + "]", "" + this.m_panControl.getPrecision());
			}
		}
	}

	/**
	 * Opens the line.
	 */
	private void openLine() throws LineUnavailableException {
		if (this.m_line != null) {
			AudioFormat audioFormat = this.m_audioInputStream.getFormat();
			this.trace(1, this.getClass().getName(), "AudioFormat : " + audioFormat);
			this.m_line.open(audioFormat, this.m_line.getBufferSize());
		}
	}

	/**
	 * Stops the playback.<br>
	 *
	 * Player Status = STOPPED.<br>
	 * Thread should free Audio ressources.
	 */
	public void stopPlayback() {
		// if ( (m_status == PLAYING) || (m_status == PAUSED))
		// {
		if (this.m_line != null) {
			this.m_line.flush();
			this.m_line.stop();
			this.m_line.close();
			this.m_line = null;
		}
		this.m_status = BasicPlayer.STOPPED;
		// Fix to unlock file.
		try {
			if (this.m_audioInputStream != null) {
				this.m_audioInputStream.close();
			}
			this.m_gainControl = null;
		} catch (IOException ioe) {
			this.trace(1, this.getClass().getName(), "Cannot close stream : " + ioe.getMessage());
		}
		this.trace(1, this.getClass().getName(), "Stop called");
		// }
	}

	/**
	 * Pauses the playback.<br>
	 *
	 * Player Status = PAUSED.
	 */
	public void pausePlayback() {
		if (this.m_line != null) {
			if (this.m_status == BasicPlayer.PLAYING) {
				this.m_line.flush();
				this.m_line.stop();
				this.m_status = BasicPlayer.PAUSED;
				this.trace(1, this.getClass().getName(), "Pause called");
			}
		}
	}

	/**
	 * Resumes the playback.<br>
	 *
	 * Player Status = PLAYING.
	 */
	public void resumePlayback() {
		if (this.m_line != null) {
			if (this.m_status == BasicPlayer.PAUSED) {
				this.m_line.start();
				this.m_status = BasicPlayer.PLAYING;
				this.trace(1, this.getClass().getName(), "Resume called");
			}
		}
	}

	/**
	 * Starts playback.
	 */
	public boolean startPlayback() {
		if ((this.m_status == BasicPlayer.STOPPED) || (this.m_status == BasicPlayer.READY)) {
			this.trace(1, this.getClass().getName(), "Start called");
			if (!(this.m_thread == null || !this.m_thread.isAlive())) {
				this.trace(1, this.getClass().getName(), "WARNING: old thread still running!!");
				int cnt = 0;
				while (this.m_status != BasicPlayer.READY) {
					try {
						if (this.m_thread != null) {
							cnt++;
							Thread.sleep(1000);
							if (cnt > 2) {
								this.m_thread.interrupt();
							}
						}
					} catch (Exception e) {
						this.trace(1, this.getClass().getName(), "Waiting Error : " + e.getMessage());
					}
					this.trace(1, this.getClass().getName(), "Waiting ... " + cnt);
				}
			}
			try {
				this.initLine();
			} catch (Exception e) {
				this.trace(0, this.getClass().getName(), "Cannot init Line", e.getMessage());
				BasicPlayer.log.error("Exception occured: ", e);
				return false;
			}
			this.trace(1, this.getClass().getName(), "Creating new thread");
			this.m_thread = new Thread(this);
			this.m_thread.start();
			if (this.m_line != null) {
				this.setVolume(this.volume);
				this.m_line.start();
			}
		}
		return true;
	}

	/**
	 * Main loop.
	 *
	 * Player Status == STOPPED => End of Thread + Freeing Audio Ressources.<br>
	 * Player Status == PLAYING => Audio stream data sent to Audio line.<br>
	 * Player Status == PAUSED => Waiting for another status.
	 */
	@Override
	public void run() {
		this.trace(1, this.getClass().getName(), "Thread Running");
		// if (m_audioInputStream.markSupported())
		// m_audioInputStream.mark(m_audioFileFormat.getByteLength());
		// else trace(1,getClass().getName(), "Mark not supported");
		int nBytesRead = 1;
		this.m_status = BasicPlayer.PLAYING;
		int nBytesCursor = 0;
		byte[] abData = new byte[BasicPlayer.EXTERNAL_BUFFER_SIZE];
		float nFrameSize = this.m_line.getFormat().getFrameSize();
		float nFrameRate = this.m_line.getFormat().getFrameRate();
		float bytesPerSecond = nFrameSize * nFrameRate;
		int secondsTotal = Math.round(this.m_audioFileFormat.getByteLength() / bytesPerSecond);
		// E.B.
		// secondsTotal = (int) Math.round(getTotalLengthInSeconds());
		while ((nBytesRead != -1) && (this.m_status != BasicPlayer.STOPPED)) {
			if (this.m_status == BasicPlayer.PLAYING) {
				try {
					nBytesRead = this.m_audioInputStream.read(abData, 0, abData.length);
				} catch (Exception e) {
					this.trace(1, this.getClass().getName(), "InputStream error : (" + nBytesRead + ")", e.getMessage());
					BasicPlayer.log.error("Exception occured: ", e);
					this.m_status = BasicPlayer.STOPPED;
				}
				if (nBytesRead >= 0) {
					// if (m_bpl != null)
					// {
					// m_bpl.updateMediaData(abData);
					// }
					if (this.m_line != null) {
						int nBytesWritten = this.m_line.write(abData, 0, nBytesRead);
						nBytesCursor = nBytesCursor + nBytesWritten;
					}
					// if (m_bpl != null)
					// {
					// m_bpl.updateCursor( (int) Math.round( (float)
					// nBytesCursor / bytesPerSecond), secondsTotal);
					// }
				}
			} else {
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
					this.trace(1, this.getClass().getName(), "Thread cannot sleep : ", e.getMessage());
				}
			}
		}
		if (this.m_line != null) {
			try {
				this.m_line.drain();
				this.m_line.stop();
				this.m_line.close();
			} catch (Exception e) {
				this.trace(1, this.getClass().getName(), "Cannot Free Audio ressources", e.getMessage());
			} finally {
				this.m_line = null;
			}
		}
		this.trace(1, this.getClass().getName(), "Thread Stopped");
		this.m_status = BasicPlayer.READY;
		if (this.basicPlayerManager != null && nBytesRead == -1) {
			this.basicPlayerManager.fileCompletedPlaying();
		}
	}

	/*----------------------------------------------*/
	/*--               Gain Control               --*/
	/*----------------------------------------------*/

	/**
	 * Returns true if Gain control is supported.
	 */
	public boolean hasGainControl() {
		return this.m_gainControl != null;
	}

	/**
	 * Sets Gain value. Linear scale 0.0 <--> 1.0 Threshold Coef. : 1/2 to avoid
	 * saturation.
	 */
	public void setVolume(int volume) {
		this.volume = volume;

		/*
		 * (float) (Math.log(volume / 100d) / Math.log(10.0) * 20.0) (float)
		 * ((Math.log(vol) / Math.log(10.0)) * 20.0); //See
		 * http://docs.oracle.com
		 * /javase/1.5.0/docs/api/javax/sound/sampled/FloatControl
		 * .Type.html#MASTER_GAIN float db =
		 * (float)(Math.log(volume/100)/Math.log(10.0)*20.0);
		 */
		// Check again if we have no gain control: the line has to be in use
		// before we can get gain control
		if (!this.hasGainControl()) {
			if (this.m_line != null && this.m_line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
				this.m_gainControl = (FloatControl) this.m_line.getControl(FloatControl.Type.MASTER_GAIN);
				this.trace(1, this.getClass().getName(), "Master Gain Control : [" + this.m_gainControl.getMinimum() + "," + this.m_gainControl.getMaximum() + "]", "" + this.m_gainControl.getPrecision());
			}
		}

		if (this.hasGainControl()) {
			/*
			 * double minGainDB = getMinimum(); double ampGainDB = ( (10.0f /
			 * 20.0f) * getMaximum()) - getMinimum(); double cste =
			 * Math.log(10.0) / 20; double valueDB = minGainDB + (1 / cste) *
			 * Math.log(1 + (Math.exp(cste * ampGainDB) - 1) * fGain);
			 * trace(1,getClass().getName(), "Gain : "+valueDB);
			 */
			float valueDB = (float) (Math.log(volume / 100.0) / Math.log(10.0) * 20.0);
			this.m_gainControl.setValue(valueDB);
			// m_gainControl.setValue( (float) fGain);
		}
	}

	/**
	 * Returns Gain value.
	 */
	public float getGain() {
		if (this.hasGainControl()) {
			return this.m_gainControl.getValue();
		} else {
			return 0.0F;
		}
	}

	public int getVolume() {
		return (int) net.yourhome.server.base.Util.round((Math.pow(10.0, this.getGain() / 20.0) * 100), 0);
	}

	/**
	 * Gets max Gain value.
	 */
	public float getMaximum() {
		if (this.hasGainControl()) {
			return this.m_gainControl.getMaximum();
		} else {
			return 0.0F;
		}
	}

	/**
	 * Gets min Gain value.
	 */
	public float getMinimum() {
		if (this.hasGainControl()) {
			return this.m_gainControl.getMinimum();
		} else {
			return 0.0F;
		}
	}

	/*----------------------------------------------*/
	/*--               Pan Control                --*/
	/*----------------------------------------------*/

	/**
	 * Returns true if Pan control is supported.
	 */
	public boolean hasPanControl() {
		return this.m_panControl != null;
	}

	/**
	 * Returns Pan precision.
	 */
	public float getPrecision() {
		if (this.hasPanControl()) {
			return this.m_panControl.getPrecision();
		} else {
			return 0.0F;
		}
	}

	/**
	 * Returns Pan value.
	 */
	public float getPan() {
		if (this.hasPanControl()) {
			return this.m_panControl.getValue();
		} else {
			return 0.0F;
		}
	}

	/**
	 * Sets Pan value. Linear scale : -1.0 <--> +1.0
	 */
	public void setPan(float fPan) {
		if (this.hasPanControl()) {
			// trace(1,getClass().getName(), "Pan : "+fPan);
			this.m_panControl.setValue(fPan);
		}
	}

	/*----------------------------------------------*/
	/*--                   Seek                   --*/
	/*----------------------------------------------*/

	/**
	 * Sets Seek value. Linear scale : 0.0 <--> +1.0
	 */
	public void setSeek(double seek) throws IOException {
		double length = -1;
		if ((this.m_audioFileFormat != null) && (this.m_audioFileFormat.getByteLength() != AudioSystem.NOT_SPECIFIED)) {
			length = this.m_audioFileFormat.getByteLength();
		}
		long newPos = Math.round(seek * length);
		this.doSeek = newPos;
	}

	/*----------------------------------------------*/
	/*--               Audio Format               --*/
	/*----------------------------------------------*/

	/**
	 * Returns source AudioFormat.
	 */
	public AudioFormat getAudioFormat() {
		if (this.m_audioFileFormat != null) {
			return this.m_audioFileFormat.getFormat();
		} else {
			return null;
		}
	}

	/**
	 * Returns source AudioFileFormat.
	 */
	public AudioFileFormat getAudioFileFormat() {
		if (this.m_audioFileFormat != null) {
			return this.m_audioFileFormat;
		} else {
			return null;
		}
	}

	/**
	 * Returns total length in seconds.
	 */
	// public double getTotalLengthInSeconds()
	// {
	// double lenghtInSecond = 0.0;
	// if (getAudioFileFormat() != null)
	// {
	// int FL = (getAudioFileFormat()).getFrameLength();
	// int FS = (getAudioFormat()).getFrameSize();
	// float SR = (getAudioFormat()).getSampleRate();
	// float FR = (getAudioFormat()).getFrameRate();
	// int TL = (getAudioFileFormat()).getByteLength();
	// String type = (getAudioFileFormat()).getType().toString();
	// String encoding = (getAudioFormat()).getEncoding().toString();
	// if ( (FL != -1) && ( (type.startsWith("MP3")) ||
	// (type.startsWith("VORBIS"))))
	// {
	// // No accurate formula :-(
	// // Alternative dirty solution with SPI
	// StringTokenizer st = new StringTokenizer(type, "x");
	// st.nextToken();
	// st.nextToken();
	// String totalMSStr = st.nextToken();
	// lenghtInSecond = Math.round( (Integer.parseInt(totalMSStr)) / 1000);
	// }
	// else
	// {
	// int br = getBitRate();
	// if (br > 0)
	// {
	// lenghtInSecond = TL * 8 / br;
	// }
	// else
	// {
	// lenghtInSecond = TL / (FS * SR);
	//
	// }
	// }
	// trace(2, getClass().getName(), "Type=" + type + " Encoding=" + encoding +
	// " FL=" + FL + " FS=" + FS + " SR=" + SR + " FR=" + FR + " TL=" + TL,
	// " lenghtInSecond=" + lenghtInSecond);
	// }
	// if (lenghtInSecond < 0.0)
	// {
	// lenghtInSecond = 0.0;
	// }
	// return lenghtInSecond;
	// }

	/**
	 * Returns bit rate.
	 */
	public int getBitRate() {
		int bitRate = 0;
		if (this.getAudioFileFormat() != null) {
			int FL = (this.getAudioFileFormat()).getFrameLength();
			int FS = (this.getAudioFormat()).getFrameSize();
			float SR = (this.getAudioFormat()).getSampleRate();
			float FR = (this.getAudioFormat()).getFrameRate();
			int TL = (this.getAudioFileFormat()).getByteLength();
			String type = (this.getAudioFileFormat()).getType().toString();
			String encoding = (this.getAudioFormat()).getEncoding().toString();
			// Assumes that type includes xBitRate string.
			if ((type != null) && ((type.startsWith("MP3")) || (type.startsWith("VORBIS")))) {
				// BitRate string appended to type.
				// Another solution ?
				StringTokenizer st = new StringTokenizer(type, "x");
				if (st.hasMoreTokens()) {
					st.nextToken();
					String bitRateStr = st.nextToken();
					bitRate = Math.round((Integer.parseInt(bitRateStr)));
				}
			} else {
				bitRate = Math.round(FS * FR * 8);
			}
			this.trace(2, this.getClass().getName(), "Type=" + type + " Encoding=" + encoding + " FL=" + FL + " FS=" + FS + " SR=" + SR + " FR=" + FR + " TL=" + TL, " bitRate=" + bitRate);
		}
		// N/A so computes bitRate for output.
		if ((bitRate <= 0) && (this.m_line != null)) {
			bitRate = Math.round(((this.m_line.getFormat()).getFrameSize()) * ((this.m_line.getFormat()).getFrameRate()) * 8);
		}
		return bitRate;
	}

	/**
	 * Gets an InputStream from File.
	 */
	protected InputStream openInput(File file) throws IOException {
		InputStream fileIn = new FileInputStream(file);
		BufferedInputStream bufIn = new BufferedInputStream(fileIn);
		return bufIn;
	}

	/*----------------------------------------------*/
	/*--                   Misc                   --*/
	/*----------------------------------------------*/

	/**
	 * Sends traces to Debug.
	 */
	private void trace(int level, String msg1, String msg2) {
		// Debug dbg = Debug.getInstance();
		// dbg.log(level, msg1 + ":" + msg2);
		System.out.println(level + ":" + msg1 + ":" + msg2);
	}

	private void trace(int level, String msg1, String msg2, String msg3) {
		// Debug dbg = Debug.getInstance();
		// dbg.log(level, msg1 + ":" + msg2 + "," + msg3);
		System.out.println(level + ":" + msg1 + ":" + msg2 + "," + msg3);
	}

}
