package fr.inria.repairnator;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

//Test server for BuildRainer
public class TestServer extends WebSocketServer {
	private static String host = "localhost";
	private static int port = 8887;
	private static TestServer server;
	private static String recentMessage;
	private BuildRainer buildRainer;


	public TestServer(InetSocketAddress address) {
		super(address);
	}

	public static TestServer getInstance() {
		if (server == null) {
			server = new TestServer(new InetSocketAddress(host, port));
			recentMessage = "";
		}
		return server;
	}

	public String getRecentMessage() {
		return recentMessage;
	}

	public void serverInit(String host , int port) {
		this.host = host;
		this.port = port;
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		conn.send("Server closed"); //This method sends a message to the new client
		System.out.println("new connection to " + conn.getRemoteSocketAddress());
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		System.out.println("closed " + conn.getRemoteSocketAddress() + " with exit code " + code + " additional info: " + reason);
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		System.out.println("received message from "	+ conn.getRemoteSocketAddress() + ": " + message);
		recentMessage = message;
		try {
			this.stop(); /*Done testing*/
		} catch(Exception e){
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onMessage( WebSocket conn, ByteBuffer message ) {
		System.out.println("received ByteBuffer from "	+ conn.getRemoteSocketAddress());
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		System.err.println("an error occured on connection " + conn.getRemoteSocketAddress()  + ":" + ex);
	}
	
	@Override
	public void onStart() {
		System.out.println("server started successfully");
		
		// Connect BuildRainer to server at server start.
		try {
			this.buildRainer = new BuildRainer( new URI( "ws://localhost:8887" ));
        	this.buildRainer.connect();
		} catch(Exception e){
			throw new RuntimeException(e);
		}
	}
}
