package net.yourhome.server.net.rest;

import java.io.File;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;

import net.yourhome.server.base.SettingsManager;
import net.yourhome.server.base.Util;

@Path("/Logs")
public class Logs {

	private static Logger log = Logger.getLogger(Logs.class);

	// GET api/Logs/HomeServer/{bytes}
	@GET
	@Produces({ MediaType.APPLICATION_OCTET_STREAM })
	@Path("HomeServer/{bytes}")
	public String Get(@Context final UriInfo uriInfo, String bodyContent, @PathParam("bytes") final int bytes) {
		File logFile = new File(SettingsManager.getBasePath(), "/logs/HomeServer.txt");
		return Util.readBytesFromTextFile(logFile, bytes);
	}

	// GET api/Logs/ZWave/{bytes}
	@GET
	@Produces({ MediaType.APPLICATION_OCTET_STREAM })
	@Path("ZWave/{bytes}")
	public String getZwaveLog(@Context final UriInfo uriInfo, String bodyContent, @PathParam("bytes") final int bytes) {
		File logFile = new File(SettingsManager.getBasePath(), "/logs/OZW_Log.txt");
		return Util.readBytesFromTextFile(logFile, bytes);
	}
}
