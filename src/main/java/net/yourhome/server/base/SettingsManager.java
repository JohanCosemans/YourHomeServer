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
			if (basePath == null) {
				basePath = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().getParent() + "/";
				System.out.println("[Settings] Base bath set as " + basePath);
			}
		} catch (URISyntaxException e1) {
		}

		try {
			String hostname = InetAddress.getLocalHost().getHostName();
			configFile = new File(getBasePath(), "config/homeserver-" + hostname + ".cfg");
			FileInputStream fileInput = null;
			if (!configFile.exists()) {
				configFile = new File(getBasePath(), "config/homeserver.cfg");
				if(configFile.exists()) {
					System.out.println("[Settings] Specific config file " + new File(getBasePath(), "config/homeserver-" + hostname + ".cfg").getAbsolutePath() + " does not exist. Reading homeserver.cfg");					
				}else {
					configFile = new File(getBasePath(), "config/homeserver-default.cfg");
					System.out.println("[Settings] Config files " + new File(getBasePath(), "config/homeserver-" + hostname + ".cfg").getAbsolutePath() + " or "+new File(getBasePath(), "config/homeserver.cfg") + " do not exist. Reading homeserver-default.cfg");	
				}
			} else {
				System.out.println("[Settings] Using config file " + configFile.getAbsolutePath());
			}
			fileInput = new FileInputStream(configFile);
			mySettings.load(fileInput);

		} catch (FileNotFoundException e) {
			// log.error("Exception occured: ",e);
		} catch (IOException e) {
			// log.error("Exception occured: ",e);
		}
	}

	public static String getStringValue(String controllerIdentifier, Setting key) {
		Map<Setting, String> controllerSettings = allSettings.get(controllerIdentifier);
		if (controllerSettings != null) {
			return controllerSettings.get(key);
		} else {
			return null;
		}
	}

	public static String getStringValue(String controllerIdentifier, Setting key, String defaultValue) {
		String returnValue = getStringValue(controllerIdentifier, key);
		return returnValue == null ? defaultValue : returnValue;
	}

	public static void setStringValue(String controllerIdentifier, String key, String value) {
		Map<Setting, String> controllerSettings = allSettings.get(controllerIdentifier);
		for (Entry<Setting, String> controllerSetting : controllerSettings.entrySet()) {
			if (controllerSetting.getKey().getName().equals(key)) {
				controllerSetting.setValue(value);
			}
		}
		mySettings.setProperty(key, value);
	}

	public static boolean storeSettings() {
		try {
			FileOutputStream fos = new FileOutputStream(configFile);
			mySettings.store(fos, "");
		} catch (IOException e1) {
			return false;
		}
		return true;
	}
	
	public static void storeSettingsAndRestart() {
		storeSettings();
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
		basePath = customBasePath;
	}

	public static String getBasePath() {
		return basePath;
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
				String settingValue = mySettings.getProperty(setting.convert());
				settingValue = settingValue == null ? "" : settingValue;
				Map<Setting, String> settingsForController = allSettings.get(controller.getIdentifier());
				if (settingsForController == null) {
					settingsForController = new LinkedHashMap<>();
					allSettings.put(controller.getIdentifier(), settingsForController);
				}
				settingsForController.put(setting, settingValue);
			}
		}
	}

	public static Map<String, Map<Setting, String>> getAllSettings() {
		return allSettings;
	}
}
