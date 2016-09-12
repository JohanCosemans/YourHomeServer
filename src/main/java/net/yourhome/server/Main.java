package net.yourhome.server;

import java.io.File;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import net.yourhome.server.base.SettingsManager;
import net.yourhome.server.net.Server;

/**
 * @author johan
 */
public class Main {

	private static Logger log;

	public static void main(String[] args) {

		try {
			if (args.length > 0 && new File(args[0]).exists()) {
				System.out.println("[Settings] Custom base folder provided: " + args[0]);
				SettingsManager.setBasePath(args[0]);
			}

			SettingsManager.initialize();

			// Set console log path
			System.setProperty("HOMESERVER_LOG", new File(SettingsManager.getBasePath(), "logs/HomeServer.txt").getAbsolutePath());
			PropertyConfigurator.configure(new File(SettingsManager.getBasePath(), "config/log4j.properties").getAbsolutePath());
			log = Logger.getLogger(Main.class);

			// Initialize server: Websocket + HTTP fileserver + API
			Server netWebSocketServer = Server.getInstance();

		} catch (Exception e) {
			if(log != null) {
				log.error("Exception occured: ", e);
			}else{
				e.printStackTrace();
			}
		}
	}

	public static void restart() {
		Server.getInstance().destroy();
		System.exit(121);
	}
}
