package fr.inria.spirals.repairnator.buildrainer;

import fr.inria.spirals.repairnator.TravisInputBuild;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.InputBuild;

import java.net.URI;
import java.net.URISyntaxException;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.FlaggedOption;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import org.json.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * This is a websocket, intended for listening to "tdurieux/travis-listener"
 * to fetch most recent builds from Travis in realtime.
 */
public class BuildRainer extends WebSocketClient implements BuildSubmitter{
    private static Logger LOGGER = LoggerFactory.getLogger(BuildRainer.class);
    private static final BuildSubmitter submitter = new ActiveMQBuildSubmitter();
    private static final RepairnatorConfig config = RepairnatorConfig.getInstance();
    private static BuildRainer buildRainer;
    private String recentMessage;

    private static JSAP defineArgs() throws JSAPException{
        JSAP jsap = new JSAP();


        return jsap;
    }

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
                LOGGER.info("state: " + state + " language: " + language);
                int build_id = obj.getJSONObject("data").getInt("build_id");
                this.submitBuild(new TravisInputBuild(build_id));
            }
        }
        this.recentMessage = message;
    }

    public static BuildRainer getInstance() {
        LOGGER.warn("Build Rainer is now running");
        if (buildRainer == null) {
            try {
                ((ActiveMQBuildSubmitter) submitter).initBroker();
                buildRainer = new BuildRainer( new URI( config.getWebSocketUrl() ));
            } catch(URISyntaxException e) {
                throw new RuntimeException("Invalid websocket URI");
            }
        }
        return buildRainer;
    }

    @Override
    public void submitBuild(InputBuild b) {
        submitter.submitBuild(b);
    }


    @Override
    public void onOpen( ServerHandshake handshakedata ) {
        LOGGER.warn( "opened connection" );
    }

    @Override
    public void onClose( int code, String reason, boolean remote ) {
        LOGGER.warn( "Connection closed by " + ( remote ? "remote peer" : "us" ) + " Code: " + code + " Reason: " + reason );
    }

    @Override
    public void onError( Exception ex ) {
        ex.printStackTrace();
    }
} 
