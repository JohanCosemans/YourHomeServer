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
 * THIS SOFTWARE IS PROVIDED BY THE NETBSD FOUNDATION, INC. AND CONTRIBUTORS
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
