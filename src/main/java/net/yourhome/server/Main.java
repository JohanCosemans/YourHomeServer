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
			Main.log = Logger.getLogger(Main.class);

			// Initialize server: Websocket + HTTP fileserver + API
			Server netWebSocketServer = Server.getInstance();

		} catch (Exception e) {
			if (Main.log != null) {
				Main.log.error("Exception occured: ", e);
			} else {
				e.printStackTrace();
			}
		}
	}

	public static void restart() {
		Server.getInstance().destroy();
		System.exit(121);
	}
}
