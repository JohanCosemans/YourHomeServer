package net.yourhome.server.net.rest.zwave;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.yourhome.server.zwave.ZWaveController;

@Path("/ZWave/Commands")
public class Commands {

	private ZWaveController controller;
	private static Logger log = Logger.getLogger(Commands.class);

	// The initialize method will only be called when the controllers are needed
	// (in this way, the controllers are not initialized during the network
	// startup)
	private void initialize() {
		controller = ZWaveController.getInstance();
	}

	@Produces({ MediaType.APPLICATION_JSON })
	@GET
	public String getCommands() {
		if (controller == null) {
			initialize();
		}
		JSONArray jsonArray = new JSONArray();
		for (ZWaveCommand command : controller.getCommandList(this)) {
			JSONObject jsonObject = new JSONObject();
			try {
				jsonObject.put("name", command.name);
				jsonObject.put("command", command.command);
			} catch (JSONException e) {
				log.error("Exception occured: ", e);
			}
			jsonArray.put(jsonObject);
		}

		return jsonArray.toString();
	}

	public class ZWaveCommand {
		public String name;
		public String command;

		public ZWaveCommand(String name, String command) {
			this.name = name;
			this.command = command;
		}
	}
}
