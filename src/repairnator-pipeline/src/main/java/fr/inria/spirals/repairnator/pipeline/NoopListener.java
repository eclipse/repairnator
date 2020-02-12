package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.Listener;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * A listener which does nothing.
 */
public class NoopListener implements Listener{
    private static final Logger LOGGER = LoggerFactory.getLogger(NoopListener.class);
    private Launcher launcher;

    private static RepairnatorConfig getConfig() {
        return RepairnatorConfig.getInstance();
    }

    public NoopListener(Launcher launcher){
        this.launcher = launcher;
        LOGGER.warn("NOOP MODE");
    }

    /**
     * Run launcher mainProcess . 
     */
    public void runListenerServer() {
        if (this.getConfig().isNoTravisRepair()) {
            LOGGER.info("No travis repair mode for reparinator");
            JenkinsLauncher noTravisLauncher = new JenkinsLauncher();
            noTravisLauncher.noTravisMain();
        }else {
            this.launcher.mainProcess();
        }
        /*launcher.mainProcess();*/
    }

    public void submitBuild(String buildStr){}
}
