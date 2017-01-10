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
package net.yourhome.server.http;

import net.yourhome.common.base.enums.ValueTypes;
import net.yourhome.common.net.messagestructures.JSONMessage;
import net.yourhome.common.net.messagestructures.general.ActivationMessage;
import net.yourhome.common.net.messagestructures.http.HttpCommand;
import net.yourhome.common.net.messagestructures.http.HttpCommandMessage;
import net.yourhome.common.net.messagestructures.http.HttpNode;
import net.yourhome.common.net.messagestructures.http.HttpNodeMessage;
import net.yourhome.common.net.model.binding.ControlIdentifiers;
import net.yourhome.server.AbstractController;
import net.yourhome.server.ControllerNode;
import net.yourhome.server.ControllerValue;
import net.yourhome.server.base.DatabaseConnector;
import net.yourhome.server.base.Setting;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class HttpCommandController extends AbstractController {
	private static volatile HttpCommandController httpCommandController;
	private static Object lock = new Object();

	private HttpCommandController() {
		this.log = Logger.getLogger("net.yourhome.server.http.Http");
	}

	public static HttpCommandController getInstance() {
		HttpCommandController r = HttpCommandController.httpCommandController;
		if (r == null) {
			synchronized (HttpCommandController.lock) { // while we were waiting
														// for the lock, another
				r = HttpCommandController.httpCommandController; // thread may
																	// have
																	// instantiated
																	// the
				// object
				if (r == null) {
					r = new HttpCommandController();
					HttpCommandController.httpCommandController = r;
				}
			}
		}

		return HttpCommandController.httpCommandController;
	}

	@Override
	public JSONMessage parseNetMessage(JSONMessage message) {
		if (message instanceof ActivationMessage) {
			try {
				Integer commandId = Integer.parseInt(((ActivationMessage) message).controlIdentifiers.getValueIdentifier());
				HttpCommand command = this.getCommand(commandId);
				this.sendHttpCommand(command);
			} catch (Exception e) {
				this.log.error("Exception occured: ", e);
			}
		}
		/*
		 * // Old messages else if(message.type.equals("HttpCommand")){
		 * HttpCommandMessage httpCommandMessage = new
		 * HttpCommandMessage(message); httpCommandMessage.httpCommand = new
		 * HttpCommand(jsonObject);
		 * 
		 * try { return sendHttpCommand(httpCommandMessage.httpCommand); } catch
		 * (Exception e) { log.error("Exception occured: ",e); }
		 * 
		 * }else if(message.type.equals("ActivateHttpCommand")) { // Get command
		 * details ActivateHttpCommandMessage activateCommandMessage = new
		 * ActivateHttpCommandMessage(message); activateCommandMessage.commandId
		 * = jsonObject.getInt("commandId");
		 * 
		 * 
		 * HttpCommandMessage httpCommandMessage2 = new HttpCommandMessage();
		 * 
		 * 
		 * HttpCommand command; try { command =
		 * this.getCommand(activateCommandMessage.commandId);
		 * httpCommandMessage2.httpCommand = command; return
		 * parseNetMessage(httpCommandMessage2.serialize()); } catch
		 * (SQLException e) { return new ClientMessageMessage("Command " +
		 * activateCommandMessage.commandId + " does not exist"); } }
		 */
		return null;
	}
	/*
	 * public JSONObject getCommandList() { List<HttpNode> allNodes =
	 * this.getAllNodes();
	 * 
	 * for(int i=0;i<allNodes.size();i++) { HttpNode thisNode = allNodes.get(i);
	 * for(HttpCommand c : this.getAllCommands(thisNode)) {
	 * thisNode.addCommand(c); } } JSONObject completeObject = new JSONObject();
	 * try { completeObject.put("nodeStructure", allNodes); } catch
	 * (JSONException e) { } return completeObject; }
	 */

	public HttpCommandMessage sendHttpCommand(HttpCommand command) throws Exception {

		this.log.debug("[" + Thread.currentThread().getId() + "] Executing http command ");
		HttpClient client = this.getHttpClient();
		HttpRequestBase request = null;

		if (command.getHttpMethod().equals("POST")) {
			request = new HttpPost();
			StringEntity entity = new StringEntity(command.getMessageBody());
			entity.setContentType(command.getMessageType());
			((HttpPost) request).setEntity(entity);
		} else if (command.getHttpMethod().equals("GET")) {
			request = new HttpGet();
		} else if (command.getHttpMethod().equals("PUT")) {
			request = new HttpPut();
			StringEntity entity2 = new StringEntity(command.getMessageBody());
			((HttpPut) request).setEntity(entity2);
		} else if (command.getHttpMethod().equals("DELETE")) {
			request = new HttpDelete();
		} else {
			return null;
		}

		request.setURI(new URI(command.getUrl()));

		Map<String, String> headers = new HashMap<String, String>();
		if (command.getMessageType() != null && !command.getMessageType().equals("")) {
			headers.put("Content-Type", command.getMessageType());
		}
		if (command.getHeaders() != null) {
			for (Map.Entry<String, String> entry : command.getHeaders().entrySet()) {
				headers.put(entry.getKey(), entry.getValue());
			}
		}
		if (headers != null) {
			for (Entry<String, String> s : headers.entrySet()) {
				request.setHeader(s.getKey(), s.getValue());
			}
		}
		this.log.debug("Out: " + request.toString());
		HttpResponse response = client.execute(request);
		String responseBody = EntityUtils.toString(response.getEntity());
		HttpCommandMessage responseMessage = new HttpCommandMessage();
		responseMessage.response = responseBody;

		return responseMessage;
	}

	private HttpClient getHttpClient() {

		try {
			SSLContext sslContext = SSLContext.getInstance("SSL");

			sslContext.init(null, new TrustManager[] { new X509TrustManager() {
				@Override
				public X509Certificate[] getAcceptedIssuers() {

					return null;
				}

				@Override
				public void checkClientTrusted(X509Certificate[] certs, String authType) {

				}

				@Override
				public void checkServerTrusted(X509Certificate[] certs, String authType) {

				}
			} }, new SecureRandom());

			org.apache.http.conn.ssl.SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslContext);
			HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
			httpClientBuilder.setSSLSocketFactory(factory);
			HttpClient httpClient = httpClientBuilder.build();

			return httpClient;

		} catch (Exception e) {
			this.log.error("Exception occured: ", e);
			return HttpClientBuilder.create().build();
		}
	}

	public String saveHttpNode(HttpNodeMessage message) throws SQLException {
		String sql = "insert into main.Http_Nodes ('name', 'parentId')" + " VALUES ('" + message.name + "','" + message.parentId + "')";
		int id = DatabaseConnector.getInstance().insertConfigurationValue(sql);
		return "{ \"id\" : " + id + " }";
	}

	public String saveHttpCommand(HttpCommandMessage message) throws SQLException {
		String sql = "insert into main.Http_Commands ('parentNodeId','json') VALUES (?,?)";
		PreparedStatement preparedInsert = DatabaseConnector.getInstance().prepareStatement(sql);
		preparedInsert.setInt(1, message.httpCommand.getParentNodeId());
		preparedInsert.setString(2, message.httpCommand.serialize().toString());
		Integer id = DatabaseConnector.getInstance().executePreparedUpdate(preparedInsert);
		return "{ \"id\" : " + id + " }";
	}

	public String changeHttpCommand(HttpCommandMessage changedHttpCommand) throws SQLException {

		String sql = "UPDATE main.Http_Commands SET 'json'=? WHERE id = ?";
		PreparedStatement preparedUpdate = DatabaseConnector.getInstance().prepareStatement(sql);
		preparedUpdate.setString(1, changedHttpCommand.httpCommand.serialize().toString());
		preparedUpdate.setInt(2, changedHttpCommand.httpCommand.getId());
		Integer id = DatabaseConnector.getInstance().executePreparedUpdate(preparedUpdate);

		return "{ \"result\" : " + (id == null) + " }";
	}

	public HttpCommand getCommand(int commandId) throws SQLException {
		HttpCommand returnCommand = null;

		String sql = "SELECT * from main.Http_Commands where id = '" + commandId + "'";
		ResultSet allCommandsResult = null;
		try {
			allCommandsResult = DatabaseConnector.getInstance().executeSelect(sql);
			returnCommand = null;
			while (allCommandsResult.next()) {

				try {
					String jsonString = allCommandsResult.getString("json");
					if (jsonString != null) {
						returnCommand = new HttpCommand(new JSONObject(jsonString));
						returnCommand.setId(allCommandsResult.getInt("id"));
						returnCommand.setParentNodeId(allCommandsResult.getInt("parentNodeId"));
					}
				} catch (SQLException e) {
				}

				if (returnCommand == null) {
					returnCommand = new HttpCommand(allCommandsResult);
				}

			}
		} catch (SQLException e) {
			this.log.error("Exception occured: ", e);
		} finally {
			try {
				if (allCommandsResult != null) {
					allCommandsResult.getStatement().close();
					allCommandsResult.close();
				}
			} catch (SQLException e) {
				this.log.error("Exception occured: ", e);
			}
		}

		if (returnCommand == null) {
			throw new SQLException("Command " + commandId + " not found!");
		}

		return returnCommand;
	}

	public List<HttpCommand> getAllCommands(HttpNode node) {

		String sql = "SELECT * from main.Http_Commands where parentNodeId = '" + node.getId() + "'";
		ResultSet allCommandsResult = null;
		List<HttpCommand> commandsList = new ArrayList<HttpCommand>();
		try {
			allCommandsResult = DatabaseConnector.getInstance().executeSelect(sql);

			while (allCommandsResult.next()) {
				HttpCommand command = null;
				try {
					String jsonString = allCommandsResult.getString("json");
					if (jsonString != null) {
						command = new HttpCommand(new JSONObject(jsonString));
						command.setId(allCommandsResult.getInt("id"));
						command.setParentNodeId(allCommandsResult.getInt("parentNodeId"));
					}
				} catch (SQLException e) {
				}

				// To be removed when other columns are removed
				if (command == null) {
					command = new HttpCommand(allCommandsResult);
				}

				commandsList.add(command);
			}
		} catch (SQLException e) {
			this.log.error("Exception occured: ", e);
		} finally {
			try {
				if (allCommandsResult != null) {
					allCommandsResult.getStatement().close();
					allCommandsResult.close();
				}
			} catch (SQLException e) {
				this.log.error("Exception occured: ", e);
			}
		}

		return commandsList;

	}

	public List<HttpNode> getAllNodes() {
		String sql = "SELECT * from main.Http_Nodes";
		ResultSet allNodesResult = null;
		List<HttpNode> nodesArray = new ArrayList<HttpNode>();
		try {
			allNodesResult = DatabaseConnector.getInstance().executeSelect(sql);
			while (allNodesResult.next()) {
				HttpNode node = new HttpNode(allNodesResult);
				for (HttpCommand c : this.getAllCommands(node)) {
					node.addCommand(c);
				}
				nodesArray.add(node);
			}
		} catch (SQLException e) {
			this.log.error("Exception occured: ", e);
		} finally {
			try {
				if (allNodesResult != null) {
					allNodesResult.getStatement().close();
					allNodesResult.close();
				}
			} catch (SQLException e) {
				this.log.error("Exception occured: ", e);
			}
		}
		return nodesArray;
	}

	public HttpNode getNode(int nodeId) {
		String sql = "SELECT * from main.Http_Nodes WHERE id = '" + nodeId + "'";
		ResultSet allNodesResult = null;
		HttpNode thisNode = null;
		try {
			allNodesResult = DatabaseConnector.getInstance().executeSelect(sql);

			while (allNodesResult.next()) {
				thisNode = new HttpNode(allNodesResult);
				for (HttpCommand c : this.getAllCommands(thisNode)) {
					thisNode.addCommand(c);
				}
			}
		} catch (SQLException e) {
			this.log.error("Exception occured: ", e);
		} finally {
			try {
				if (allNodesResult != null) {
					allNodesResult.getStatement().close();
					allNodesResult.close();
				}
			} catch (SQLException e) {
				this.log.error("Exception occured: ", e);
			}
		}
		return thisNode;
	}

	public boolean deleteNode(int nodeId) throws SQLException {
		String sql = "DELETE from main.Http_Commands where parentNodeId = '" + nodeId + "'";
		if (DatabaseConnector.getInstance().executeQuery(sql)) {
			sql = "DELETE from main.Http_Nodes where id = '" + nodeId + "'";
			return DatabaseConnector.getInstance().executeQuery(sql);
		}
		return false;
	}

	public boolean deleteCommand(int commandId) throws SQLException {
		String sql = "DELETE from main.Http_Commands WHERE id = '" + commandId + "'";
		boolean result = DatabaseConnector.getInstance().executeQuery(sql);
		return result;
	}

	@Override
	public String getIdentifier() {
		return "http";
	}

	@Override
	public String getName() {
		return "Http";
	}

	@Override
	public void init() {
		super.init();
		this.log.info("Initialized");
	}

	@Override
	public List<JSONMessage> initClient() {
		return null;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public List<ControllerNode> getNodes() {
		List<ControllerNode> controllerNodes = new ArrayList<ControllerNode>();
		for (HttpNode node : this.getAllNodes()) {
			ControllerNode commandsNode = new ControllerNode(this, "" + node.getId(), node.getName(), "http_node");
			for (HttpCommand command : node.getCommands()) {
				commandsNode.addValue(new ControllerValue("" + command.getId(), command.getName(), ValueTypes.HTTP_COMMAND));
			}
			controllerNodes.add(commandsNode);
		}
		return controllerNodes;
	}

	@Override
	public String getValueName(ControlIdentifiers valueIdentifier) {
		try {
			HttpCommand command = this.getCommand(Integer.parseInt(valueIdentifier.getValueIdentifier()));
			if (command != null) {
				return command.getName();
			}
		} catch (NumberFormatException e) {
		} catch (SQLException e) {
		}

		return "Unkown";
	}

	@Override
	public List<ControllerNode> getTriggers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getValue(ControlIdentifiers valueIdentifiers) {
		return null;
	}

	@Override
	public boolean isInitialized() {
		return true;
	}

	@Override
	public List<Setting> getSettings() {
		// TODO Auto-generated method stub
		return null;
	}

}
