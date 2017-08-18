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

import net.yourhome.common.Util;
import net.yourhome.common.net.messagestructures.JSONMessage;
import net.yourhome.server.base.GeneralController;
import net.yourhome.server.base.SettingsManager;
import org.apache.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import java.io.IOException;

public class NetSocket extends WebSocketAdapter {

	private static Logger log = Logger.getLogger(NetSocket.class);

	private Session session;
	private Server netWebSocketServer;

	public NetSocket() {
		this.netWebSocketServer = Server.getInstance();
	}

	@Override
	public void onWebSocketConnect(Session session) {
		super.onWebSocketConnect(session);

		NetSocket.log.debug("[Net] Connect: " + session.getRemoteAddress().getHostString());
		this.session = session;
		this.netWebSocketServer.addClient(this);
		this.netWebSocketServer.initializeClient(this);
	}

	@Override
	public void onWebSocketText(String possiblyEncryptedJson) {
		super.onWebSocketText(possiblyEncryptedJson);
		NetSocket.log.debug("[Net] Receive: " + possiblyEncryptedJson);
        try {
            String returningMessage = this.netWebSocketServer.processIncomingMessage(possiblyEncryptedJson); // This
            if (returningMessage != null) {
                try {
                    this.session.getRemote().sendString(returningMessage);
                } catch (IOException e) {
                    NetSocket.log.error("Exception occured: ", e);
                }
            }
        } catch (Exception e) {
            log.error("Could not decrypt message. Wrong password??");
        }
	}

	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		super.onWebSocketClose(statusCode, reason);
		NetSocket.log.debug("[Net] Disconnect: " + this.session.getRemoteAddress().getHostString());
		this.netWebSocketServer.removeClient(this);
	}

	@Override
	public void onWebSocketError(Throwable cause) {
		super.onWebSocketError(cause);
		cause.printStackTrace(System.err);
	}

	@Override
	public Session getSession() {
		return this.session;
	}

}
