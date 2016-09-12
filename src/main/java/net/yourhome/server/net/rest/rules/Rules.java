package net.yourhome.server.net.rest.rules;

import java.sql.SQLException;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import org.json.JSONException;
import org.json.JSONObject;

import net.yourhome.server.base.rules.Rule;
import net.yourhome.server.base.rules.RuleManager;

@Path("/Rules")
public class Rules {

	private static Logger log = Logger.getLogger(Rules.class);

	// POST api/Rules
	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	public Response Post(@Context final UriInfo uriInfo, String bodyContent) throws SQLException {
		// A. Parse body
		try {
			JSONObject ruleObject = new JSONObject(bodyContent);
			Rule rule = new Rule(ruleObject);
			rule.setActive(true);
			RuleManager.save(rule);
		} catch (JSONException e) {
			log.error("Exception occured: ", e);
			return Response.serverError().build();
		}
		return Response.ok().build();
	}

	@PUT
	@Path("/{ruleId}")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response Put(@Context final UriInfo uriInfo, @PathParam("ruleId") final int ruleId, String bodyContent) throws SQLException {

		try {
			Rule originalRule = RuleManager.getRule(ruleId);

			JSONObject ruleObject = new JSONObject(bodyContent);
			Rule ruleToUpdate = new Rule(ruleObject);
			ruleToUpdate.setId(originalRule.getId());
			ruleToUpdate.setActive(originalRule.getActive());

			RuleManager.save(ruleToUpdate);

		} catch (JSONException e) {
			log.error("Exception occured: ", e);
			return Response.serverError().build();
		}

		return Response.ok().build();
	}

	// GET api/Rules
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public String Get(@Context final UriInfo uriInfo, String bodyContent) throws SQLException {
		JSONArray resultArray = new JSONArray();
		List<Rule> allRules = RuleManager.getAllRules();
		for (Rule rule : allRules) {
			resultArray.put(rule.getSourceJsonObject());
		}
		return resultArray.toString();

	}

	// GET api/Rules/12
	@GET
	@Path("/{id}")
	@Produces({ MediaType.APPLICATION_JSON })
	public String Get(@Context final UriInfo uriInfo, @PathParam("id") final int id, @PathParam("function") final String function, String bodyContent) throws SQLException, JSONException {
		return RuleManager.getRule(id).getSourceJsonObject().toString();
	}

	// DELETE api/Rules/12
	@DELETE
	@Path("/{id}")
	public Response Delete(@Context final UriInfo uriInfo, @PathParam("id") final int ruleId, @PathParam("function") final String function, String bodyContent) throws SQLException, JSONException {
		Rule rule = RuleManager.getRule(ruleId);
		RuleManager.delete(rule);
		return Response.ok().build();
	}

	// PUT api/Rules/12/true
	@PUT
	@Path("/{id}/{status}")
	public Response setActive(@Context final UriInfo uriInfo, @PathParam("id") final int ruleId, @PathParam("status") final boolean status, String bodyContent) throws SQLException, JSONException {
		Rule ruleToChange = RuleManager.getRule(ruleId);
		RuleManager.setActive(ruleToChange, status);
		return Response.ok().build();
	}
}
