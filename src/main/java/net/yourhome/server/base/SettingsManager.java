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
package net.yourhome.server.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import net.yourhome.server.IController;
import net.yourhome.server.Main;

public class SettingsManager {
	// Initialize preferences
	private static Map<String, Map<Setting, String>> allSettings = new ConcurrentHashMap<>();
	private static Properties mySettings = new Properties() {
		@Override
		public synchronized Enumeration<Object> keys() {
			return Collections.enumeration(new TreeSet<Object>(super.keySet()));
		}
	};
	private static File configFile;
	// private static Logger log;

	private static String basePath = null;

	public static void initialize() {
		try {
			if (SettingsManager.basePath == null) {
				SettingsManager.basePath = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().getParent() + "/";
				System.out.println("[Settings] Base bath set as " + SettingsManager.basePath);
			}
		} catch (URISyntaxException e1) {
		}

		try {
			String hostname = InetAddress.getLocalHost().getHostName();
			SettingsManager.configFile = new File(SettingsManager.getBasePath(), "config/homeserver-" + hostname + ".cfg");
			FileInputStream fileInput = null;
			if (!SettingsManager.configFile.exists()) {
				SettingsManager.configFile = new File(SettingsManager.getBasePath(), "config/homeserver.cfg");
				if (SettingsManager.configFile.exists()) {
					System.out.println("[Settings] Specific config file " + new File(SettingsManager.getBasePath(), "config/homeserver-" + hostname + ".cfg").getAbsolutePath() + " does not exist. Reading homeserver.cfg");
				} else {
					SettingsManager.configFile = new File(SettingsManager.getBasePath(), "config/homeserver-default.cfg");
					System.out.println("[Settings] Config files " + new File(SettingsManager.getBasePath(), "config/homeserver-" + hostname + ".cfg").getAbsolutePath() + " or " + new File(SettingsManager.getBasePath(), "config/homeserver.cfg") + " do not exist. Reading homeserver-default.cfg");
				}
			} else {
				System.out.println("[Settings] Using config file " + SettingsManager.configFile.getAbsolutePath());
			}
			fileInput = new FileInputStream(SettingsManager.configFile);
			SettingsManager.mySettings.load(fileInput);

		} catch (FileNotFoundException e) {
			// log.error("Exception occured: ",e);
		} catch (IOException e) {
			// log.error("Exception occured: ",e);
		}
	}

	public static String getStringValue(String controllerIdentifier, Setting key) {
		Map<Setting, String> controllerSettings = SettingsManager.allSettings.get(controllerIdentifier);
		if (controllerSettings != null) {
			return controllerSettings.get(key);
		} else {
			return null;
		}
	}

	public static String getStringValue(String controllerIdentifier, Setting key, String defaultValue) {
		String returnValue = SettingsManager.getStringValue(controllerIdentifier, key);
		return returnValue == null ? defaultValue : returnValue;
	}

	public static void setStringValue(String controllerIdentifier, String key, String value) {
		Map<Setting, String> controllerSettings = SettingsManager.allSettings.get(controllerIdentifier);
		for (Entry<Setting, String> controllerSetting : controllerSettings.entrySet()) {
			if (controllerSetting.getKey().getName().equals(key)) {
				controllerSetting.setValue(value);
			}
		}
		SettingsManager.mySettings.setProperty(key, value);
	}

	public static boolean storeSettings() {
		try {
			FileOutputStream fos = new FileOutputStream(SettingsManager.configFile);
			SettingsManager.mySettings.store(fos, "");
		} catch (IOException e1) {
			return false;
		}
		return true;
	}

	public static void storeSettingsAndRestart() {
		SettingsManager.storeSettings();
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
				}
				Main.restart();
			}
		}.start();
	}

	public static void setBasePath(String customBasePath) {
		SettingsManager.basePath = customBasePath;
	}

	public static String getBasePath() {
		return SettingsManager.basePath;
	}

	public static String getTempPath() {
		File filePath = new File(System.getProperty("java.io.tmpdir"), "HomeServer");
		if (!filePath.exists()) {
			filePath.mkdirs();
		}
		return filePath.getAbsolutePath();
	}

	public static void readSettings(IController controller) {
		Collection<Setting> controllerSettings = controller.getSettings();
		if (controllerSettings != null) {
			// Check value
			for (Setting setting : controllerSettings) {
				String settingValue = SettingsManager.mySettings.getProperty(setting.convert());
				settingValue = settingValue == null ? "" : settingValue;
				Map<Setting, String> settingsForController = SettingsManager.allSettings.get(controller.getIdentifier());
				if (settingsForController == null) {
					settingsForController = new LinkedHashMap<>();
					SettingsManager.allSettings.put(controller.getIdentifier(), settingsForController);
				}
				settingsForController.put(setting, settingValue);
			}
		}
	}

	public static Map<String, Map<Setting, String>> getAllSettings() {
		return SettingsManager.allSettings;
	}
}
