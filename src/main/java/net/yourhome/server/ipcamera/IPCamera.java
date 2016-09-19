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
package net.yourhome.server.ipcamera;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import net.yourhome.server.base.SettingsManager;
import net.yourhome.server.base.Util;
import net.yourhome.server.net.rest.view.ImageHelper;

public class IPCamera {

	private Logger log = Logger.getLogger("net.yourhome.server.ipcamera.IPCamera");

	private int id;
	private String name;
	private String snapshotUrl; // eg. http://192.168.1.108/cgi-bin/snapshot.cgi
	private String videoUrl; // eg. http://192.168.1.108/axis-cgi/mjpg/video.cgi

	public IPCamera(ResultSet result) throws SQLException {
		this.setId(result.getInt("id"));
		this.setName(result.getString("name"));
		this.setVideoUrl(result.getString("videoUrl"));
		this.setSnapshotUrl(result.getString("snapshotUrl"));
	}

	public File getSnapshotByName(String filename) {
		String snapshotPath = SettingsManager.getBasePath() + ImageHelper.IPCAMSNAPSHOTS + "/" + filename;
		final File snapshot = new File(snapshotPath);
		return snapshot;
	}

	public File saveAndGetSnapshot(boolean async, String filename) {

		this.log.debug("Saving snapshot of camera " + this.id + " into filename " + filename + ". Async: " + async);

		try {
			final URL url = new URL(this.snapshotUrl);

			// Make sure the image folder exists
			File IPCamSnapShotFolder = new File(SettingsManager.getTempPath() + ImageHelper.IPCAMSNAPSHOTS);
			if (!IPCamSnapShotFolder.exists()) {
				IPCamSnapShotFolder.mkdirs();
			}

			String newImagePath = SettingsManager.getBasePath() + ImageHelper.IPCAMSNAPSHOTS + "/" + filename + ".jpg";
			final File snapshot = new File(newImagePath);
			if (!snapshot.getParentFile().exists()) {
				snapshot.getParentFile().mkdirs();
			}

			if (async) {
				new Thread() {
					@Override
					public void run() {
						try {
							InputStream in = new BufferedInputStream(url.openStream());
							Util.writeToFile(in, snapshot);
							IPCamera.this.log.debug("Snapshot saved");
						} catch (IOException e) {
							IPCamera.this.log.error("Exception occured: ", e);
						}
					}
				}.start();
			} else {
				InputStream in = new BufferedInputStream(url.openStream());
				Util.writeToFile(in, snapshot);
				this.log.debug("Snapshot saved");
			}

			return snapshot;
		} catch (MalformedURLException e) {
			this.log.error("Exception occured: ", e);
		} catch (IOException e) {
			this.log.error("Exception occured: ", e);
		}
		return null;
	}

	public File saveAndGetSnapshot(boolean async) {
		return this.saveAndGetSnapshot(async, this.id + "");
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return this.id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the snapshotUrl
	 */
	public String getSnapshotUrl() {
		return this.snapshotUrl;
	}

	/**
	 * @param snapshotUrl
	 *            the snapshotUrl to set
	 */
	public void setSnapshotUrl(String snapshotUrl) {
		this.snapshotUrl = snapshotUrl;
	}

	/**
	 * @return the videoUrl
	 */
	public String getVideoUrl() {
		return this.videoUrl;
	}

	/**
	 * @param videoUrl
	 *            the videoUrl to set
	 */
	public void setVideoUrl(String videoUrl) {
		this.videoUrl = videoUrl;
	}

}
