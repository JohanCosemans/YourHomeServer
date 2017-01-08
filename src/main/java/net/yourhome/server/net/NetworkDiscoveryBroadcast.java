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

import net.yourhome.common.net.Discovery;
import net.yourhome.server.net.rest.Info;
import org.apache.log4j.Logger;
import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class NetworkDiscoveryBroadcast {

	private static final long BROADCAST_INTERVAL = 2000;
	private static Logger log = Logger.getLogger("net.yourhome.server.net.Discovery");

	private Thread t;

	public void start() {
		this.t = new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					NetworkDiscoveryBroadcast.this.listenForDiscoveryRequests();
				}
			}
		});
		this.t.start();
	}

	private final void listenForDiscoveryRequests() {
		DatagramSocket socket = null;
		try {
			int packetLength = 1024;

			socket = new DatagramSocket(null);
			socket.setReuseAddress(true);
			socket.setBroadcast(true);
			socket.bind(new InetSocketAddress(Discovery.BROADCAST_PORT));

			DatagramPacket packet = new DatagramPacket(new byte[packetLength], 0, packetLength);
			socket.setSoTimeout(3000);

			for (int i = 0; i < 50; i++) {
				try {
					socket.receive(packet);
					ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength());
					int n = bais.available();
					byte[] bytes = new byte[n];
					bais.read(bytes, 0, n);
					String message = new String(bytes, StandardCharsets.UTF_8);
					if (message.equals(Discovery.BROADCAST_SERVICE)) {
						NetworkDiscoveryBroadcast.log.debug("Received discovery request from " + packet.getAddress() + ":" + packet.getPort() + ": " + message);
						this.sendReply(socket, packet.getAddress(), packet.getPort());
					}
				} catch (SocketTimeoutException exc) {
				} catch (IOException exc) {
					exc.printStackTrace();
				}
			}
		} catch (SocketException e) {
			NetworkDiscoveryBroadcast.log.error("Exception occured: ", e);
		} finally {
			if (socket != null) {
				socket.close();
			}
		}
	}

	private final void sendReply(DatagramSocket socket, InetAddress client, int port) {
		try {
			String serverInfoString = Info.getServerInfo().serialize().toString();
			DatagramPacket packet = new DatagramPacket(serverInfoString.getBytes(), serverInfoString.length(), new InetSocketAddress(client, port));
			NetworkDiscoveryBroadcast.log.debug("Received discovery request from " + packet.getAddress() + ":" + packet.getPort() + ": " + serverInfoString);
			socket.send(packet);
		} catch (IOException e) {
			NetworkDiscoveryBroadcast.log.error("Exception occured: ", e);
		} catch (JSONException e) {
			NetworkDiscoveryBroadcast.log.error("Exception occured: ", e);
		}
	}
}
