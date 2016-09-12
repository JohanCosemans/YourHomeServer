/*
 * Copyright (C) 2010-2014 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.yourhome.server.music;

import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.a0z.mpd.exception.MPDException;
import org.a0z.mpd.item.Music;

public class AlbumCache {

	static final boolean GZIP = false;

	private static final String TAG = "AlbumCache";

	protected static AlbumCache sInstance = null;

	protected Map<String, AlbumDetails> mAlbumDetails; // "artist///album" ->

	// list of albumname, artist, albumartist including ""
	protected Set<List<String>> mAlbumSet;

	protected boolean mEnabled = true;

	protected File mFilesDir;

	protected Date mLastUpdate = null;

	protected CachedMPD mMPD;

	protected int mPort;

	protected String mServer;

	// albums that have an albumartist get an empty artist:
	protected Set<List<String>> mUniqueAlbumSet;

	protected AlbumCache(final CachedMPD mpd) {
		super();
		this.setMPD(mpd);
	}
	// details

	public static String albumCode(final String artist, final String album, final boolean isAlbumArtist) {
		return (artist != null ? artist : "") + "//" + (isAlbumArtist ? "AA" : "A") + "//" + (album != null ? album : "");
	}

	public static AlbumCache getInstance(final CachedMPD mpd) {
		if (AlbumCache.sInstance == null) {
			AlbumCache.sInstance = new AlbumCache(mpd);
		} else {
			AlbumCache.sInstance.setMPD(mpd);
		}
		return AlbumCache.sInstance;
	}

	protected static Set<String> getKeysByValue(final Map<String, Set<String>> map, final String val) {
		final Set<String> result = new HashSet<String>();

		for (final Map.Entry<String, Set<String>> stringSetEntry : map.entrySet()) {
			final Set<String> values = stringSetEntry.getValue();
			if (val != null && val.isEmpty() || values.contains(val)) {
				result.add(stringSetEntry.getKey());
			}
		}
		return result;
	}

	public String cacheInfo() {
		return "AlbumCache: " + this.mAlbumSet.size() + " album/artist combinations, " + this.mUniqueAlbumSet.size() + " unique album/artist combinations, " + "Date: " + this.mLastUpdate;
	}

	protected synchronized void deleteFile() {
		final File file = new File(this.mFilesDir, this.getFilename());
		if (file.exists()) {
			file.delete();
		}
	}

	public Set<String> getAlbumArtists(final String album, final String artist) {
		final Set<String> aartists = new HashSet<String>();
		for (final List<String> ai : this.mAlbumSet) {
			if (ai.get(0).equals(album) && ai.get(1).equals(artist)) {
				aartists.add(ai.get(2));
			}
		}
		return aartists;
	}

	public AlbumDetails getAlbumDetails(final String artist, final String album, final boolean isAlbumArtist) {
		return this.mAlbumDetails.get(AlbumCache.albumCode(artist, album, isAlbumArtist));
	}

	public Set<List<String>> getAlbumSet() {
		return this.mAlbumSet;
	}

	public Set<String> getAlbums(final String artist, final boolean albumArtist) {
		final Set<String> albums = new HashSet<String>();
		for (final List<String> ai : this.mAlbumSet) {
			if (albumArtist && ai.get(2).equals(artist) || !albumArtist && ai.get(1).equals(artist)) {
				albums.add(ai.get(0));
			}
		}
		return albums;
	}

	public List<String> getArtistsByAlbum(final String album, final boolean albumArtist) {
		final Set<String> artists = new HashSet<String>();
		for (final List<String> ai : this.mAlbumSet) {
			if (ai.get(0).equals(album)) {
				if (albumArtist) {
					artists.add(ai.get(2));
				} else {
					artists.add(ai.get(1));
				}
			}
		}
		final List<String> result;
		if (artists != null && !artists.isEmpty()) {
			result = new ArrayList<String>(artists);
			Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
		} else {
			result = new ArrayList<String>();
		}
		return result;
	}

	public String getDirByArtistAlbum(final String artist, final String album, final boolean isAlbumArtist) {
		final String albumCode = AlbumCache.albumCode(artist, album, isAlbumArtist);
		final String result = this.mAlbumDetails.get(albumCode).mPath;
		return result;
	}

	protected String getFilename() {
		return this.mServer + '_' + this.mPort;
	}

	public Set<List<String>> getUniqueAlbumSet() {
		return this.mUniqueAlbumSet;
	}

	protected synchronized boolean isUpToDate() {
		final Date mpdlast = this.mMPD.getStatistics().getDbUpdate();
		return (null != this.mLastUpdate && null != mpdlast && this.mLastUpdate.after(mpdlast));
	}

	protected synchronized boolean load() {
		final File file = new File(this.mFilesDir, this.getFilename() + (AlbumCache.GZIP ? ".gz" : ""));
		if (!file.exists()) {
			return false;
		}
		final ObjectInputStream restore;
		boolean loadedOk = false;
		try {
			if (AlbumCache.GZIP) {
				restore = new ObjectInputStream(new GZIPInputStream(new FileInputStream(file)));
			} else {
				restore = new ObjectInputStream(new FileInputStream(file));
			}
			this.mLastUpdate = (Date) restore.readObject();
			this.mAlbumDetails = (Map<String, AlbumDetails>) restore.readObject();
			this.mAlbumSet = (Set<List<String>>) restore.readObject();
			restore.close();
			this.makeUniqueAlbumSet();
			loadedOk = true;
		} catch (final FileNotFoundException ignored) {
		} catch (final Exception e) {
		}
		if (loadedOk) {
		} else {
		}
		return loadedOk;
	}

	protected void makeUniqueAlbumSet() {
		this.mUniqueAlbumSet = new HashSet<List<String>>(this.mAlbumSet.size());
		for (final List<String> ai : this.mAlbumSet) {
			final String album = ai.get(2);
			if (album != null && album.isEmpty()) { // no albumartist
				this.mUniqueAlbumSet.add(Arrays.asList(ai.get(0), ai.get(1), ""));
			} else { // with albumartist set artist to ""
				this.mUniqueAlbumSet.add(Arrays.asList(ai.get(0), "", ai.get(2)));
			}
		}
	}

	/*
	 * reloads info from MPD if it is not up to date
	 */
	public synchronized boolean refresh() {
		return this.refresh(false);
	}

	/*
	 * reloads info from MPD if it is not up to date or if forced
	 */
	public synchronized boolean refresh(final boolean force) {
		if (!this.mEnabled) {
			return false;
		}
		if (!this.updateConnection()) {
			return false;
		}

		if (!force && this.isUpToDate()) {
			return true;
		}
		this.mLastUpdate = Calendar.getInstance().getTime();

		final Date oldUpdate = this.mLastUpdate;
		this.mAlbumDetails = new HashMap<String, AlbumDetails>();
		this.mAlbumSet = new HashSet<List<String>>();

		final List<Music> allmusic;
		try {
			allmusic = this.mMPD.listAllInfo();
		} catch (final IOException e) {
			this.mEnabled = false;
			this.mLastUpdate = null;
			this.updateConnection();
			return false;
		} catch (MPDException e) {
			this.mEnabled = false;
			this.mLastUpdate = null;
			this.updateConnection();
			return false;
		}

		try {
			for (final Music music : allmusic) {
				final String albumArtist = music.getAlbumArtist();
				final String artist = music.getArtist();
				String album = music.getAlbum();
				if (album == null) {
					album = "";
				}
				final List<String> albumInfo = Arrays.asList(album, artist == null ? "" : artist, albumArtist == null ? "" : albumArtist);
				this.mAlbumSet.add(albumInfo);

				final boolean isAlbumArtist = albumArtist != null && !albumArtist.isEmpty();
				final String thisAlbum = AlbumCache.albumCode(isAlbumArtist ? albumArtist : artist, album, isAlbumArtist);
				final AlbumDetails details;
				if (this.mAlbumDetails.containsKey(thisAlbum)) {
					details = this.mAlbumDetails.get(thisAlbum);
				} else {
					details = new AlbumDetails();
					this.mAlbumDetails.put(thisAlbum, details);
				}
				if (details.mPath == null) {
					details.mPath = music.getPath();
				}
				// if (details.times == null)
				// details.times = new ArrayList<Long>();
				// details.times.add((Long)m.getTime());
				details.mNumTracks += 1;
				details.mTotalTime += music.getTime();
				if (details.mDate == 0) {
					details.mDate = music.getDate();
				}
			}
			this.makeUniqueAlbumSet();
			if (!this.save()) {
				this.mLastUpdate = oldUpdate;
				return false;
			}
		} catch (final Exception e) {
			this.mLastUpdate = oldUpdate;
			return false;
		}
		return true;
	}

	protected synchronized boolean save() {
		final File file = new File(this.mFilesDir, this.getFilename() + (AlbumCache.GZIP ? ".gz" : ""));
		final File backupfile = new File(file.getAbsolutePath() + ".bak");
		if (file.exists()) {
			if (backupfile.exists()) {
				backupfile.delete();
			}
			file.renameTo(backupfile);
		}
		final ObjectOutputStream save;
		boolean error = false;
		try {
			if (AlbumCache.GZIP) {
				save = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(file)));
			} else {
				save = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
			}
			save.writeObject(this.mLastUpdate);
			save.writeObject(this.mAlbumDetails);
			save.writeObject(this.mAlbumSet);
			save.close();
		} catch (final Exception e) {
			error = true;
		}
		if (error) {
			file.delete();
			backupfile.renameTo(file);
		}
		return !error;
	}

	protected void setMPD(final CachedMPD mpd) {
		this.mEnabled = true;
		try {
			this.mMPD = mpd;
		} catch (final Exception e) {
		}
		this.updateConnection();
	}

	protected synchronized boolean updateConnection() {
		// get server/port from mpd
		if (!this.mEnabled) {
			return false;
		}
		if (this.mMPD == null) {
			return false;
		}

		if (!this.mMPD.isConnected()) {
			return false;
		}
		if (this.mServer == null) {
			this.mServer = this.mMPD.getHostAddress().getHostName();
			this.mPort = this.mMPD.getHostPort();
			if (!this.load()) {
				this.refresh(true);
			}
		}
		return true;
	}

	static class AlbumDetails implements Serializable {

		private static final long serialVersionUID = 2465675380232237273L;

		long mDate = 0;

		// List<Long> times = null;
		long mNumTracks = 0;

		String mPath = null;

		long mTotalTime = 0;

		private void readObject(final DataInput in) throws IOException, ClassNotFoundException {
			this.mPath = in.readUTF();
			// times = (List<Long>)in.readObject();
			this.mNumTracks = in.readLong();
			this.mTotalTime = in.readLong();
			this.mDate = in.readLong();
		}

		private void writeObject(final DataOutput out) throws IOException {
			out.writeUTF(this.mPath);
			// out.writeObject(times);
			out.writeLong(this.mNumTracks);
			out.writeLong(this.mTotalTime);
			out.writeLong(this.mDate);
		}
	}

}
