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
	private String videoUrl;    // eg. http://192.168.1.108/axis-cgi/mjpg/video.cgi

	public IPCamera(ResultSet result) throws SQLException {
		setId(result.getInt("id"));
		setName(result.getString("name"));
		setVideoUrl(result.getString("videoUrl"));
		setSnapshotUrl(result.getString("snapshotUrl"));
	}

	public File getSnapshotByName(String filename) {
		String snapshotPath = SettingsManager.getBasePath() + ImageHelper.IPCAMSNAPSHOTS + "/" + filename;
		final File snapshot = new File(snapshotPath);
		return snapshot;
	}

	public File saveAndGetSnapshot(boolean async, String filename) {

		log.debug("Saving snapshot of camera " + id + " into filename " + filename + ". Async: " + async);

		try {
			final URL url = new URL(snapshotUrl);

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
							log.debug("Snapshot saved");
						} catch (IOException e) {
							log.error("Exception occured: ", e);
						}
					}
				}.start();
			} else {
				InputStream in = new BufferedInputStream(url.openStream());
				Util.writeToFile(in, snapshot);
				log.debug("Snapshot saved");
			}

			return snapshot;
		} catch (MalformedURLException e) {
			log.error("Exception occured: ", e);
		} catch (IOException e) {
			log.error("Exception occured: ", e);
		}
		return null;
	}

	public File saveAndGetSnapshot(boolean async) {
		return saveAndGetSnapshot(async, this.id + "");
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
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
		return name;
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
		return snapshotUrl;
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
		return videoUrl;
	}

	/**
	 * @param videoUrl
	 *            the videoUrl to set
	 */
	public void setVideoUrl(String videoUrl) {
		this.videoUrl = videoUrl;
	}

}
