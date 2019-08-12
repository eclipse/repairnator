package fr.inria.spirals.repairnator.scanner;

import fr.inria.spirals.repairnator.Listener;
import javax.jms.MessageListener;
import javax.jms.Message;

/**
 * This class fetch build ids from ActiveMQ queue and run the pipeline with it.
 */
public class ScannerBuildListener implements Listener,MessageListener{
    private static Launcher launcher;

    public ScannerBuildListener (Launcher launcher) {
        this.launcher = launcher;
    }
    
    public void runListenerServer() {}

    /**
     * Method implemented from MessageListener and is called 
     * each time this is done with the previous message
     *
     * @param message ActiveMQ message object containing a string message.
     */
    public void onMessage(Message message) {}
}
