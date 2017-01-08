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
package net.yourhome.server.net.rest.view;

import net.yourhome.common.net.model.viewproperties.ViewGroup;
import net.yourhome.server.base.DatabaseConnector;
import org.apache.log4j.Logger;
import org.json.JSONArray;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.sql.SQLException;
import java.util.List;

@Path("/Views")
public class Views {

	private static Logger log = Logger.getLogger(Views.class);
	private ImageHelper imageHelper = ImageHelper.getInstance();

	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	// GET /api/Views
	public String get() {
		// Get images array
		// JSONArray imageArray = imageHelper.getImagesJSON();
		List<ViewGroup> viewGroups = this.imageHelper.getViewGroups();

		// Build return message
		return new JSONArray(viewGroups).toString();
	}

	@PUT
	@Path("/Value/{controllerIdentifier}/{nodeIdentifier}/{valueIdentifier}/{alias}")
	public Response setValueAlias(@Context final UriInfo uriInfo, @PathParam("controllerIdentifier") final String controllerIdentifier, @PathParam("nodeIdentifier") final String nodeIdentifier, @PathParam("valueIdentifier") final String valueIdentifier, @PathParam("alias") final String alias, String bodyContent) throws SQLException {
		if (DatabaseConnector.getInstance().setAlias(controllerIdentifier, nodeIdentifier, valueIdentifier, alias)) {
			return Response.ok().build();
		} else {
			return Response.serverError().build();
		}
	}

	@PUT
	@Path("/Node/{controllerIdentifier}/{nodeIdentifier}/{alias}")
	public Response setNodeAlias(@Context final UriInfo uriInfo, @PathParam("controllerIdentifier") final String controllerIdentifier, @PathParam("nodeIdentifier") final String nodeIdentifier, @PathParam("alias") final String alias, String bodyContent) throws SQLException {
		if (DatabaseConnector.getInstance().setAlias(controllerIdentifier, nodeIdentifier, alias)) {
			return Response.ok().build();
		} else {
			return Response.serverError().build();
		}
	}

	@PUT
	@Path("/Controller/{controllerIdentifier}/{nodeIdentifier}/{valueIdentifier}/{alias}")
	public Response setControllerAlias(@Context final UriInfo uriInfo, @PathParam("controllerIdentifier") final String controllerIdentifier, @PathParam("alias") final String alias, String bodyContent) throws SQLException {
		if (DatabaseConnector.getInstance().setAlias(controllerIdentifier, alias)) {
			return Response.ok().build();
		} else {
			return Response.serverError().build();
		}
	}
}
