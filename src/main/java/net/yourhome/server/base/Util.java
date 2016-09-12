package net.yourhome.server.base;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class Util {

	private static Logger log = Logger.getLogger("net.yourhome.server.base.UTIL");

	/*
	 * Get the extension of a file.
	 */
	public static String getExtension(File f) {
		String ext = null;
		String s = f.getName();
		int i = s.lastIndexOf('.');

		if (i > 0 && i < s.length() - 1) {
			ext = s.substring(i + 1).toLowerCase();
		}
		return ext;
	}
	public static String MD5(String string) {
		byte[] bytesOfMessage;
		try {
			bytesOfMessage = string.getBytes("UTF-8");
			MessageDigest md = MessageDigest.getInstance("MD5");
			return new BigInteger(1,md.digest(bytesOfMessage)).toString(16);
		} catch (UnsupportedEncodingException e) {
			log.error("Exception occured: ", e);
		} catch (NoSuchAlgorithmException e) {
			log.error("Exception occured: ", e);
		}
		return null;
	}

	public static String getStringValue(InputStream input) throws IOException {
		String result = "";
		while (input.available() > 0) {
			result += input.read();
		}
		return result;
	}

	public static double round(double value, int places) throws NumberFormatException {
		if (places < 0)
			throw new IllegalArgumentException();
		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	public static String getStackTrace(Exception e) {
		StringWriter sWriter = new StringWriter();
		PrintWriter pWriter = new PrintWriter(sWriter);
		e.printStackTrace(pWriter);
		return sWriter.toString();
	}

	public static void writeToFile(InputStream in, File file) {
		try {
			if(!file.getParentFile().exists()) { file.getParentFile().mkdirs(); }
			OutputStream out = new FileOutputStream(file);
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			out.close();
			in.close();
		} catch (Exception e) {
			log.error("Exception occured: ", e);
		}
	}

	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			is.close();
		}
	}

	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	public static String readBytesFromTextFile(File logFile, int bytes) {
		int bytesInt = bytes;

		if (logFile.exists() & logFile.canRead()) {
			if (bytes == 0) {
				bytesInt = (int) logFile.length();
			}

			byte[] result = new byte[bytesInt];

			if (bytesInt > logFile.length()) {
				bytesInt = (int) logFile.length();
			}

			RandomAccessFile raf = null;
			try {
				raf = new RandomAccessFile(logFile, "r");
				raf.seek(logFile.length() - bytesInt);

				// Read it out.
				raf.read(result, 0, bytesInt);

			} catch (FileNotFoundException e) {
				log.error("Exception occured: ", e);
			} catch (IOException e) {
				log.error("Exception occured: ", e);
			} finally {
				try {
					if (raf != null) {
						raf.close();
					}
				} catch (IOException e) {
				}
			}
			return new String(result);
		}
		return "";
	}

	public static String readFile(File file) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
		return new String(encoded, StandardCharsets.UTF_8);
	}
}
