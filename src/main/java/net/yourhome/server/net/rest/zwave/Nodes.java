package net.yourhome.server.net.rest.zwave;

import java.math.BigInteger;
import java.util.ArrayList;
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

import net.yourhome.common.base.enums.ControllerTypes;
import net.yourhome.server.base.DatabaseConnector;
import net.yourhome.server.zwave.Association;
import net.yourhome.server.zwave.AssociationGroup;
import net.yourhome.server.zwave.ControllerTransaction;
import net.yourhome.server.zwave.Node;
import net.yourhome.server.zwave.ZWaveController;

@Path("/ZWave/Nodes")
public class Nodes {

	private ZWaveController controller;
	private DatabaseConnector dbController;
	private static Logger log = Logger.getLogger(Nodes.class);

	// The initialize method will only be called when the controllers are needed
	// (in this way, the controllers are not initialized during the network
	// startup)
	public void initialize() {
		controller = ZWaveController.getInstance();
		dbController = DatabaseConnector.getInstance();
	}

	// GET api/ZWave/Nodes
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public String get(@Context final UriInfo uriInfo) {
		if (controller == null) {
			initialize();
		}
/*
		List<Node> textNodeList = new ArrayList<Node>();

		for (Node node : controller.getNodeList()) {
			textNodeList.add(controller.getNodeInformation(node, true));
		}*/

		JSONArray convertedObject = new JSONArray(controller.getNodeList());

		return convertedObject.toString();

	}

	// GET api/ZWave/Nodes/5
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("/{nodeId}")
	public String get(@Context final UriInfo uriInfo, @PathParam("nodeId") final short nodeId) {
		if (controller == null) {
			initialize();
		}

		String returnString = "";
		Node node = controller.getNode(nodeId);
		if (node != null) {
			//Node textNode = controller.getNodeInformation(node, true);
			JSONObject jsonObject = new JSONObject(node);
			returnString = jsonObject.toString();
		}
		return returnString;
	}

	// DELETE api/ZWave/Nodes/5
	@DELETE
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("/{homeId}/{nodeId}")
	public String delete(@Context final UriInfo uriInfo, @PathParam("homeId") final long homeId, @PathParam("nodeId") final short nodeId) {

		if (controller == null) {
			initialize();
		}

		controller.removeNode(homeId, nodeId);

		return "{ \"status\" : \"OK\"}";
	}

	// GET api/ZWave/Nodes/5/Associations
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("/{nodeId}/Associations")
	public String getAssociations(@Context final UriInfo uriInfo, @PathParam("nodeId") final short nodeId) {
		if (controller == null) {
			initialize();
		}

		String returnString = "";
		Node node = controller.getNode(nodeId);
		if (node != null) {
			List<Association> associations = controller.getAssociations(node.getHomeId(), node.getId());
			List<AssociationGroup> associationGroups = controller.getAssociationGroups(node.getHomeId(), node.getId());

			JSONObject jsonObject = new JSONObject();
			try {
				jsonObject.put("associations", associations);
				jsonObject.put("associationGroups", associationGroups);
				jsonObject.put("homeId", node.getHomeId());
				returnString = jsonObject.toString();
			} catch (JSONException e) {
				log.error("Exception occured: ", e);
			}

		}

		return returnString;
	}

	@DELETE
	@Path("/{homeId}/{fromNodeId}/Associations/{targetNodeId}/{associationClass}")
	public Response deleteAssociation(@Context final UriInfo uriInfo, @PathParam("homeId") final long homeId, @PathParam("fromNodeId") final short fromNodeId, @PathParam("targetNodeId") final short targetNodeId, @PathParam("associationClass") final int associationClass, String bodyContent) {
		if (controller == null) {
			initialize();
		}
		this.controller.removeAssociation(homeId, fromNodeId, associationClass, targetNodeId);

		// Build return message
		return Response.ok().build();

	}

	@PUT
	@Path("/{nodeId}/{function}/")
	public Response put(@Context final UriInfo uriInfo, @PathParam("nodeId") final int nodeId, @PathParam("function") final String function, String bodyContent) {
		if (controller == null) {
			initialize();
		}

		JSONObject jsonObject;
		try {
			jsonObject = new JSONObject(bodyContent);

			if (function.equals("Subscriptions")) {
				// Set subscriptions
				return setSubscriptions(jsonObject);
			} else if (function.equals("Configurations")) {
				// Set configuration
				return setValueSettings(jsonObject);
			} else if (function.equals("Aliases")) {
				// Set configuration
				return setAliases(jsonObject);
			} else if (function.equals("Associations")) {
				// Parse request
				jsonObject = new JSONObject(bodyContent);

				// Process request
				long homeId = jsonObject.getLong("homeId");
				short fromNodeId = (short) jsonObject.getInt("fromNodeId");
				int targetnodeId = jsonObject.getInt("toNodeId");
				int groupID = jsonObject.getInt("associationClass");
				this.controller.addAssociation(homeId, fromNodeId, groupID, targetnodeId);

				// Build return message
				return Response.ok().build();
			}

		} catch (JSONException e1) {
			e1.printStackTrace();
		} catch (Exception e) {
			log.error("Exception occured: ", e);
		}
		return null;
	}

	// POST api/ZWave/Nodes/Include
	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("Include")
	public net.yourhome.server.zwave.ControllerTransaction includeNode() {
		if (controller == null) {
			initialize();
		}
		net.yourhome.server.zwave.ControllerTransaction transaction = controller.addNode();
		return transaction;
	}
	@DELETE
	@Path("Transaction/{transactionId}")
	public Response stopTransaction(@PathParam("transactionId") final Integer transactionId) {
		if (controller == null) {
			initialize();
		}
		if(controller.cancelTransaction(transactionId)) {
			return Response.ok().build();
		}else {
			return Response.serverError().build();
		}
	}
	// GET api/ZWave/Nodes/Transaction/123
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("Transaction/{transactionId}")
	public ControllerTransaction getTransactionStatus(@PathParam("transactionId") final Integer transactionId) {
		if (controller == null) {
			initialize();
		}
		ControllerTransaction transaction = controller.getTransactionStatus(transactionId);
		return transaction;
	}

	// POST api/ZWave/Nodes/Remove
	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	@Path("Remove")
	public ControllerTransaction removeNode() {
		if (controller == null) {
			initialize();
		}
		ControllerTransaction transaction = controller.removeNode();
		return transaction;
	}

	// POST api/ZWave/Nodes/Heal
	@POST
	@Path("Heal")
	public Response healNetwork() {
		if (controller == null) {
			initialize();
		}
		controller.healNetwork();
		return Response.ok().build();
	}

	// POST api/ZWave/Nodes/Heal/<homeid>/<nodeId>
	@POST
	@Path("Heal/{homeId}/{nodeId}")
	public Response healNetwork(@PathParam("homeId") final long homeId, @PathParam("nodeId") final short nodeId) {
		if (controller == null) {
			initialize();
		}
		controller.healNetworkNode(homeId, nodeId);
		return Response.ok().build();
	}

	// POST api/ZWave/Nodes/Heal
	@POST
	@Path("Reset")
	public Response resetNetwork() {
		if (controller == null) {
			initialize();
		}

		controller.resetNetwork();
		return Response.ok().build();
	}

	private Response setSubscriptions(JSONObject bodyContent) {
		// Parse request
		if (controller == null) {
			initialize();
		}

		// {"node":"10","subscriptions":[{"valueId":"72057594206224380","subscribed":true},{"valueId":"72057594210517020","subscribed":true},{"valueId":"72057594210517020","subscribed":false},{"valueId":"72057594210517000","subscribed":false}]}

		// Process request
		try {
			short nodeId = (short) bodyContent.getInt("nodeId");
			long homeId = bodyContent.getLong("homeId");
			JSONArray subscriptions = (JSONArray) bodyContent.get("subscriptions");

			for (int i = 0; i < subscriptions.length(); i++) {
				JSONObject subscription = subscriptions.getJSONObject(i);
				BigInteger valueId = BigInteger.valueOf(subscription.getLong("valueId"));
				short nodeInstance = (short) subscription.getInt("instance");
				Boolean subscribedValue = subscription.getBoolean("subscribed");
				Boolean polledValue = subscription.getBoolean("polled");

				// Process value in zwave manager
				controller.setValuePolled(nodeId, nodeInstance, homeId, valueId, polledValue);

				// Save values in DB
				// int subscribedflag = (subscribedValue) ? 1 : 0;
				// String updateString = "INSERT or REPLACE INTO ValueSettings
				// (valueId,subscribed,nodeId,nodeInstance) VALUES (" + valueId
				// + "," + subscribedflag + "," + nodeId + "," + nodeInstance +
				// ")"; // TODO: Add polled in db
				// this.dbController.executeQuery(updateString);
				DatabaseConnector.ValueSettings valueSettings = dbController.getZWaveValueSettings(homeId, nodeId, valueId, nodeInstance);
				valueSettings.polled = polledValue;
				valueSettings.subscribed = subscribedValue;
				dbController.insertOrUpdateZWaveValueSettings(valueSettings);

			}
		} catch (Exception e) {
			return Response.serverError().build();
		}
		// Build response
		return Response.ok().build();
	}

	private Response setValueSettings(JSONObject bodyContent) {
		if (controller == null) {
			initialize();
		}

		// Process request
		JSONArray configurations;
		try {
			configurations = bodyContent.getJSONArray("configurations");

			for (int i = 0; i < configurations.length(); i++) {
				JSONObject configuration = configurations.getJSONObject(i);

				BigInteger valueId = BigInteger.valueOf(configuration.getLong("valueId"));
				short nodeId = (short) configuration.getInt("nodeId");
				long homeId = configuration.getLong("homeId");
				short instance = (short) configuration.getInt("nodeInstance");
				byte param = (byte) configuration.getInt("valueIndex");
				String val = configuration.getString("value");
				try {
					int valueInt = Integer.valueOf(val);
					controller.setConfiguration(homeId, nodeId, param, valueInt);
				} catch (Exception e) {
					controller.setValue(homeId, nodeId, instance, valueId, val);
				}

			}

			// Build response
			return Response.ok().build();
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return Response.serverError().build();
		}
	}

	private Response setAliases(JSONObject bodyContent) {
		if (controller == null) {
			initialize();
		}

		// Process request
		try {
			short nodeId = (short) bodyContent.getInt("nodeId");
			long homeId = bodyContent.getLong("homeId");
			String alias = bodyContent.getString("alias");
			try {
				// Update value settings
				String valueIdentifier = bodyContent.getString("valueIdentifier");
				// Update value alias
				dbController.setAlias(ControllerTypes.ZWAVE.convert(), ZWaveController.getNodeIdentifier(nodeId, homeId), valueIdentifier, alias);
			} catch (Exception e) {
				// Update node alias
				dbController.setAlias(ControllerTypes.ZWAVE.convert(), ZWaveController.getNodeIdentifier(nodeId, homeId), alias);
			}

		} catch (Exception e) {
			return Response.serverError().build();
		}

		// Build response
		return Response.ok().build();
	}

}
