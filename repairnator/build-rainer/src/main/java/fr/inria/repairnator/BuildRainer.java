package fr.inria.repairnator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.HashMap;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;

import 
import java.io.IOException;
import java.io.FileWriter;
/**
 * This is a websocket, intended for listening to "tdurieux/travis-listener"
 * to fetch most recent builds from Travis in realtime.
 */
public class BuildRainer extends WebSocketClient {

    public BuildRainer( URI serverURI ) {
        super( serverURI );
    }

    @Override
    public void onMessage( String message ) {
        System.out.println( "received: " + message);
    }

    @Override
    public void onOpen( ServerHandshake handshakedata ) {
        System.out.println( "opened connection" );
    }

    @Override
    public void onClose( int code, String reason, boolean remote ) {
        System.out.println( "Connection closed by " + ( remote ? "remote peer" : "us" ) + " Code: " + code + " Reason: " + reason );
    }

    @Override
    public void onError( Exception ex ) {
        ex.printStackTrace();
    }

    public static void main( String[] args )  throws URISyntaxException{
    	BuildRainer buildRainer = new BuildRainer( new URI( "ws://localhost:9080" )); // more about drafts here: http://github.com/TooTallNate/Java-WebSocket/wiki/Drafts
        buildRainer.connect();
    }
} 
