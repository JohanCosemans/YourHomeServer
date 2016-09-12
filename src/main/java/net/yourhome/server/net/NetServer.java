package net.yourhome.server.net;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

public class NetServer extends WebSocketServlet {

	private static Logger log = Logger.getLogger(NetServer.class);

	private static final long serialVersionUID = 1L;

	public NetServer() {
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		getServletContext().getNamedDispatcher("default").forward(request, response);
	}
	/*
	 * public WebSocket doWebSocketConnect(HttpServletRequest request, String
	 * protocol) { return new SocketHandler(); }
	 */

	@Override
	public void configure(WebSocketServletFactory factory) {
		factory.getPolicy().setIdleTimeout(Long.MAX_VALUE);
		factory.register(NetSocket.class);
	}
}
