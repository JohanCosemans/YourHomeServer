package net.yourhome.server.net;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import net.yourhome.common.net.messagestructures.JSONMessage;

public class NetSocket extends WebSocketAdapter {

	private static Logger log = Logger.getLogger(NetSocket.class);

	private Session session;
	private Server netWebSocketServer;

	public NetSocket() {
		netWebSocketServer = Server.getInstance();
	}

	@Override
	public void onWebSocketConnect(Session session) {
		super.onWebSocketConnect(session);

		log.debug("[Net] Connect: " + session.getRemoteAddress().getHostString());
		this.session = session;
		netWebSocketServer.addClient(this);
		netWebSocketServer.initializeClient(this);
	}

	@Override
	public void onWebSocketText(String json) {
		super.onWebSocketText(json);
		log.debug("[Net] Receive: " + json);
		JSONMessage returningMessage = netWebSocketServer.processIncomingMessage(json); // This
																						// will
																						// be
																						// done
																						// async

		if (returningMessage != null) {
			try {
				session.getRemote().sendString(returningMessage.serialize().toString());
			} catch (IOException e) {
				log.error("Exception occured: ", e);
			}
		}
	}

	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		super.onWebSocketClose(statusCode, reason);
		log.debug("[Net] Disconnect: " + session.getRemoteAddress().getHostString());
		netWebSocketServer.removeClient(this);
	}

	@Override
	public void onWebSocketError(Throwable cause) {
		super.onWebSocketError(cause);
		cause.printStackTrace(System.err);
	}

	// public void sendMessage(String data) throws IOException {
	// } connection.sendMessage(data);
	@Override
	public Session getSession() {
		return this.session;
	}

}
