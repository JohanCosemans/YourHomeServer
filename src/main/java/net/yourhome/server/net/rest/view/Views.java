package net.yourhome.server.net.rest.view;

import java.sql.SQLException;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.json.JSONArray;

import net.yourhome.common.net.model.viewproperties.ViewGroup;
import net.yourhome.server.base.DatabaseConnector;

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
		List<ViewGroup> viewGroups = imageHelper.getViewGroups();

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
