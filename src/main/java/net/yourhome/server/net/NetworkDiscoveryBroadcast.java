package net.yourhome.server.net;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import org.apache.log4j.Logger;
import org.json.JSONException;

import net.yourhome.common.net.Discovery;
import net.yourhome.server.net.rest.Info;

public class NetworkDiscoveryBroadcast {

	private static final long BROADCAST_INTERVAL = 2000;
	private static Logger log = Logger.getLogger("net.yourhome.server.net.Discovery");

	private Thread t;

	public void start() {
		t = new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					listenForDiscoveryRequests();
				}
			}
		});
		t.start();
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
						log.debug("Received discovery request from " + packet.getAddress() + ":" + packet.getPort() + ": " + message);
						sendReply(socket, packet.getAddress(), packet.getPort());
					}
				} catch (SocketTimeoutException exc) {
				} catch (IOException exc) {
					exc.printStackTrace();
				}
			}
		} catch (SocketException e) {
			log.error("Exception occured: ", e);
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
			log.debug("Received discovery request from " + packet.getAddress() + ":" + packet.getPort() + ": " + serverInfoString);
			socket.send(packet);
		} catch (IOException e) {
			log.error("Exception occured: ", e);
		} catch (JSONException e) {
			log.error("Exception occured: ", e);
		}
	}
}
