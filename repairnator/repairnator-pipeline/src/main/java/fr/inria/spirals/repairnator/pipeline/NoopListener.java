package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * A listener which does nothing.
 */
public class NoopListener implements Listener{
    private static final Logger LOGGER = LoggerFactory.getLogger(NoopListener.class);
    private Launcher launcher;

    public NoopListener(Launcher launcher){
        this.launcher = launcher;
        LOGGER.warn("NOOP MODE");
    }

    /**
     * Run this as a listener server and fetch one message as a time
     */
    public void runListenerServer() {
        launcher.mainProcess();
    }

    public void submitBuild(String buildStr){}
}
