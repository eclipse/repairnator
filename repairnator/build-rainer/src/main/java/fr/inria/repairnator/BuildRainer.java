package fr.inria.repairnator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.HashMap;

import javax.jms.JMSException;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.io.FileWriter;
import org.json.*;
/**
 * This is a websocket, intended for listening to "tdurieux/travis-listener"
 * to fetch most recent builds from Travis in realtime.
 */
public class BuildRainer extends WebSocketClient {
    private static final BuildSubmitter submitter = new ActiveMQBuildSubmitter();
    private String recentMessage;

    public BuildRainer( URI serverURI ) {
        super( serverURI );
    }

    public String getRecentMessage() {
        return recentMessage;
    }

    public boolean isJSONValid(String test) {
    try {
        new JSONObject(test);
    } catch (JSONException ex) {
        try {
            new JSONArray(test);
        } catch (JSONException ex1) {
            return false;
        }
    }
        return true;
    }

    @Override
    public void onMessage( String message ) {
        if (isJSONValid(message)) {
            JSONObject obj = new JSONObject(message);
            String state = obj.getJSONObject("data").getString("state");
            String language = obj.getJSONObject("data").getJSONObject("config").getString("language");
            if (state.equals("failed") && language.equals("java")) {
                System.out.println("state: " + state + " language: " + language);
                int build_id = obj.getJSONObject("data").getInt("build_id");
                submitter.submit(Integer.toString(build_id));
            }
        }
        this.recentMessage = message;
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
