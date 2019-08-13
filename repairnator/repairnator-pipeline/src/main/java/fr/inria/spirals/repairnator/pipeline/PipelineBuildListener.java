package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * This class fetch build ids from ActiveMQ queue and run the pipeline with it.
 */
public class PipelineBuildListener implements Listener,MessageListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineBuildListener.class);
    private static final RepairnatorConfig config = RepairnatorConfig.getInstance();
    private Launcher launcher;

    public PipelineBuildListener(Launcher launcher){
        this.launcher = launcher;
        LOGGER.warn("KUBERNETES MODE");
    }

    /**
     * Run this as a listener server and fetch one message as a time
     */
    public void runListenerServer() {}

    /**
     * Method implemented from MessageListener and is called 
     * each time this is done with the previous message
     *
     * @param message ActiveMQ message object containing a string buildId.
     */
    public void onMessage(Message message) {}

    public void submitBuild(String buildStr){}
}
