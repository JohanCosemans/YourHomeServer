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
package net.yourhome.server.net.rest;

import net.yourhome.server.base.GeneralController;
import net.yourhome.server.base.SettingsManager;
import net.yourhome.server.base.Util;
import net.yourhome.server.net.Server;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

@Path("/public/Info")
public class ServerInfoController {
	private static Logger log = Logger.getLogger(ServerInfoController.class);
	public static final String SERVER_INFO_FILE = "/serverinfo.json";

	// The initialize method will only be called when the controllers are needed
	// (in this way, the controllers are not initialized during the network
	// startup)
	private void initialize() {
	}

	// GET public/Info/
	@Produces({ MediaType.APPLICATION_JSON })
	@GET
	public net.yourhome.common.net.model.ServerInfo getInfo() throws IOException, JSONException {
		return ServerInfoController.getServerInfo();
	}


	public static net.yourhome.common.net.model.ServerInfo getServerInfo() throws IOException, JSONException {
		File serverInfoFile = new File(SettingsManager.getBasePath(), Server.FILESERVER_PATH + ServerInfoController.SERVER_INFO_FILE);
		net.yourhome.common.net.model.ServerInfo serverInfo;
		if (serverInfoFile.exists()) {
			String serverInfoString = Util.readFile(serverInfoFile);
			JSONObject serverInfoObject = new JSONObject(serverInfoString);
            serverInfo = new net.yourhome.common.net.model.ServerInfo(serverInfoObject);
		} else {
            serverInfo = new net.yourhome.common.net.model.ServerInfo();
		}
        String username = SettingsManager.getStringValue(GeneralController.getInstance().getIdentifier(), GeneralController.Settings.NET_USERNAME.get());
        String password = SettingsManager.getStringValue(GeneralController.getInstance().getIdentifier(), GeneralController.Settings.NET_PASSWORD.get());
        if (username != null && password != null) {
            serverInfo.setSecured(true);
        }else {
            serverInfo.setSecured(false);
        }
		return serverInfo;
	}

	public static boolean writeServerInfo(net.yourhome.common.net.model.ServerInfo info) throws IOException, JSONException {
		File serverInfoFile = new File(SettingsManager.getBasePath(), Server.FILESERVER_PATH + ServerInfoController.SERVER_INFO_FILE);
		String serverInfoString = info.serialize().toString();
		Util.writeToFile(new ByteArrayInputStream(serverInfoString.getBytes()), serverInfoFile);
		return true;
	}

}