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
package net.yourhome.server.net;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.BindException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.cxf.transport.servlet.CXFServlet;
import org.apache.log4j.Logger;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.json.JSONException;
import org.json.JSONObject;
import org.reflections.Reflections;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import net.yourhome.common.base.enums.MessageLevels;
import net.yourhome.common.base.enums.MessageTypes;
import net.yourhome.common.net.messagestructures.JSONMessage;
import net.yourhome.common.net.messagestructures.general.ClientMessageMessage;
import net.yourhome.common.net.messagestructures.general.ProtectedJSONMessage;
import net.yourhome.common.net.model.ServerInfo;
import net.yourhome.server.IController;
import net.yourhome.server.base.BuildConfig;
import net.yourhome.server.base.DatabaseConnector;
import net.yourhome.server.base.GeneralController;
import net.yourhome.server.base.SettingsManager;
import net.yourhome.server.base.UncaughtExceptionHandler;
import net.yourhome.server.base.rules.RuleManager;
import net.yourhome.server.net.rest.AppConfig;
import net.yourhome.server.net.rest.Info;
import net.yourhome.server.net.rest.view.ImageHelper;

/*
 * This class is the link between the NET (websocket, http) and all the controllers (general, zwave, mopidy, radio, http, ...)
 * 
 */

public class Server {
	public static final String FILESERVER_PATH = "/web";
	public static final String WEBSOCKET_PATH = "/websocket";

	private static Logger log = Logger.getLogger("net.yourhome.server.net.Net");

	// Keep reference to all controllers:
	private Map<String, IController> controllers = new ConcurrentHashMap<String, IController>();

	// Keep reference to all NET-controllers and clients
	private CopyOnWriteArrayList<WebSocketAdapter> connectedClients = new CopyOnWriteArrayList<WebSocketAdapter>();
	private org.eclipse.jetty.server.Server server;

	// Make sure there is only one (singleton)
	private static Server instance;
	private static Object lock = new Object();

	public static Server getInstance() {
		Server r = Server.instance;
		if (r == null) {
			synchronized (Server.lock) { // while we were waiting for the lock,
											// another
				r = Server.instance; // thread may have instantiated the object
				if (r == null) {
					r = new Server();
					Server.instance = r;
				}
			}
		}
		return Server.instance;
	}

	// Constructor: Initialize all the controllers

	private Server() {

		// Make sure that all exceptions are written to the log
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());

		new Thread() {
			@Override
			public void run() {

				/*
				 * Find all controllers and initialize them
				 * 
				 */
				Reflections reflections = new Reflections("net.yourhome.server");
				Set<Class<? extends IController>> controllerClasses = reflections.getSubTypesOf(IController.class);

				for (final Class<? extends IController> controllerClass : controllerClasses) {
					if (!Modifier.isAbstract(controllerClass.getModifiers())) {
						new Thread() {
							@Override
							public void run() {
								Method factoryMethod;
								try {
									factoryMethod = controllerClass.getDeclaredMethod("getInstance");
									IController controllerObject = (IController) factoryMethod.invoke(null, null);
									Server.this.controllers.put(controllerObject.getIdentifier(), controllerObject);
									SettingsManager.readSettings(controllerObject);
									controllerObject.init();
								} catch (Exception e) {
									Server.log.error("Could not instantiate " + controllerClass.getName() + " because getInstance is missing", e);
								}
							}
						}.start();
					}
				}
			}
		}.start();

	}

	public void init() {
		/*
		 * First: Initialize Web (HTTP+REST API) and websocket connection
		 */
		this.initWebAndSocketServer();

		/*
		 * Set/update server info
		 */
		try {
			ServerInfo serverInfo = Info.getServerInfo();
			serverInfo.setPort(Integer.parseInt(SettingsManager.getStringValue(GeneralController.getInstance().getIdentifier(), GeneralController.Settings.NET_HTTP_PORT.get())));
			serverInfo.setName(SettingsManager.getStringValue(GeneralController.getInstance().getIdentifier(), GeneralController.Settings.SERVER_NAME.get(), "Home server"));
			serverInfo.setVersion(BuildConfig.VERSION);
			Info.writeServerInfo(serverInfo);
		} catch (IOException | JSONException e1) {
			Server.log.error("Could not update server info. Designer might not work as expected: " + e1.getMessage());
		}

		RuleManager.init();
	}

	private void initWebAndSocketServer() {

		this.server = new org.eclipse.jetty.server.Server();
		// Port
		ServerConnector connector = new ServerConnector(this.server);
		connector.setPort(Integer.parseInt(SettingsManager.getStringValue(GeneralController.getInstance().getIdentifier(), GeneralController.Settings.NET_HTTP_PORT.get())));
		this.server.addConnector(connector);

		List<Handler> handlerList = new ArrayList<Handler>();

		// Create websocket context
		ServletContextHandler webSocketRestcontext = new ServletContextHandler(ServletContextHandler.SESSIONS);
		webSocketRestcontext.addServlet(new ServletHolder(new NetServer()), Server.WEBSOCKET_PATH);
		this.server.setHandler(webSocketRestcontext);

		// Attach REST api
		final ServletHolder servletHolder = new ServletHolder(new CXFServlet());
		webSocketRestcontext.addEventListener(new ContextLoaderListener());
		webSocketRestcontext.setInitParameter("contextClass", AnnotationConfigWebApplicationContext.class.getName());
		webSocketRestcontext.setInitParameter("contextConfigLocation", AppConfig.class.getName());
		webSocketRestcontext.addServlet(servletHolder, "/api/*");

		// Create fileserverContext
		ContextHandler fileServerContext = new ContextHandler();
		ResourceHandler resource_handler = new ResourceHandler();
		resource_handler.setDirectoriesListed(true);
		resource_handler.setResourceBase(SettingsManager.getBasePath() + Server.FILESERVER_PATH);
		resource_handler.setCacheControl("no-cache");
		resource_handler.setMinMemoryMappedContentLength(-1);
		fileServerContext.setHandler(resource_handler);
		fileServerContext.setContextPath("/");

		// Set security on /HomeDesigner/
		String username = SettingsManager.getStringValue(GeneralController.getInstance().getIdentifier(), GeneralController.Settings.NET_USERNAME.get(), "");
		String password = SettingsManager.getStringValue(GeneralController.getInstance().getIdentifier(), GeneralController.Settings.NET_PASSWORD.get(), "");

		if (!username.equals("") && !password.equals("")) {
			HashLoginService loginService = new HashLoginService();
			ConstraintSecurityHandler security = new ConstraintSecurityHandler();
			this.server.addBean(loginService);

			loginService.putUser(username, new Password(password), new String[] { "admin" });

			Constraint constraint = new Constraint();
			constraint.setName("auth");
			constraint.setAuthenticate(true);
			constraint.setRoles(new String[] { "admin" });

			ConstraintMapping mapping = new ConstraintMapping();
			mapping.setPathSpec(ImageHelper.HOMEDESIGNER_PATH + "/*");
			mapping.setConstraint(constraint);

			security.setConstraintMappings(Collections.singletonList(mapping));
			security.setAuthenticator(new BasicAuthenticator());
			security.setLoginService(loginService);
			security.setHandler(fileServerContext);
			handlerList.add(security);
		} else {
			handlerList.add(fileServerContext);
		}
		handlerList.add(webSocketRestcontext);

		ContextHandlerCollection contexts = new ContextHandlerCollection();
		// contexts.setHandlers(new Handler[] { security, webSocketRestcontext
		// });
		contexts.setHandlers(handlerList.toArray(new Handler[handlerList.size()]));

		this.server.setHandler(contexts);

		try {
			this.server.start();
			Server.log.info("Webserver initialized. Address: " + InetAddress.getLocalHost().getHostName() + ":" + connector.getPort() + " / " + InetAddress.getLocalHost().getHostAddress() + ":" + connector.getPort());
			// server.join();

		} catch (BindException e) {
			Server.log.error("Could not bind to port " + connector.getPort() + ". Is it already in use by another application? Please change the port in the configuration file and restart the server.");
		} catch (Exception e) {
			Server.log.error("Exception occured: ", e);
		}

		NetworkDiscoveryBroadcast discovery = new NetworkDiscoveryBroadcast();
		discovery.start();

	}

	public void destroy() {
		try {
			this.server.stop();
		} catch (Exception e) {
			Server.log.error("Exception occured: ", e);
		}
		this.server.destroy();

		for (IController controller : this.controllers.values()) {
			controller.destroy();
		}
		this.connectedClients.clear();
		this.controllers.clear();
		Server.instance = null;

		DatabaseConnector.getInstance().destroy();
	}

	public Map<String, IController> getControllers() {
		return this.controllers;
	}

	public JSONMessage processMessage(JSONMessage message) {
		if (message instanceof ProtectedJSONMessage) {
			ProtectedJSONMessage protectedJsonMessage = (ProtectedJSONMessage) message;
			if (protectedJsonMessage.isProtected) {
				String targetPasscode = SettingsManager.getStringValue(GeneralController.getInstance().getIdentifier(), GeneralController.Settings.PROTECTED_PASSCODE.get());
				if (!targetPasscode.equals(protectedJsonMessage.protectionCode)) {
					ClientMessageMessage wrongPasscodeMessage = new ClientMessageMessage();
					wrongPasscodeMessage.broadcast = false;
					wrongPasscodeMessage.messageContent = "Wrong passcode! Please try again";
					wrongPasscodeMessage.messageLevel = MessageLevels.ERROR;
					return wrongPasscodeMessage;
				}
			}
		}
		// Let controller process the message
		JSONMessage returningMessage = null;
		try {
			IController controller = this.controllers.get(message.controlIdentifiers.getControllerIdentifier().convert());
			if (controller != null && controller.isEnabled()) {
				returningMessage = controller.parseNetMessage(message);
			} else {
				throw new Exception("No controller found for " + message.controlIdentifiers.getControllerIdentifier().convert());
			}

			if (returningMessage != null && returningMessage.broadcast) {
				this.broadcast(returningMessage);
				returningMessage = null;
			}
		} catch (Exception e) {
			Server.log.error("Exception occured: ", e);
		}
		return returningMessage;

	}

	public JSONMessage processIncomingMessage(String json) {
		try {
			JSONObject jsonObject = new JSONObject(json);
			JSONMessage message = MessageTypes.getMessage(jsonObject);
			return this.processMessage(message);
		} catch (Exception e) {
			Server.log.error("Exception occured: ", e);
			Server.log.error("Incorrect json received: " + json);
		}
		return null;
	}

	public void addClient(WebSocketAdapter socketHandler) {
		this.connectedClients.add(socketHandler);
	}

	public void removeClient(WebSocketAdapter socketHandler) {
		this.connectedClients.remove(socketHandler);
	}

	public void broadcast(JSONMessage message) {
		String messageString = message.serialize().toString();
		Server.log.debug("Broadcast: " + messageString);
		for (WebSocketAdapter u : this.connectedClients) {
			u.getRemote().sendStringByFuture(messageString);
		}
	}

	public void initializeClient(WebSocketAdapter client) {
		// Send node information to client
		Server.log.debug("Send initial node info to " + client);

		for (IController controller : this.controllers.values()) {
			if (controller != null && controller.isEnabled() && controller.isInitialized()) {
				Collection<JSONMessage> messages = controller.initClient();
				if (messages != null) {
					for (JSONMessage message : messages) {
						client.getRemote().sendStringByFuture(message.serialize().toString());
					}
				}
			}
		}
	}

}
