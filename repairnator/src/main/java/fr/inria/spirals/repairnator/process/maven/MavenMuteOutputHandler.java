package fr.inria.spirals.repairnator.process.maven;

import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by urli on 09/01/2017.
 */
public class MavenMuteOutputHandler implements InvocationOutputHandler  {

    private final Logger logger = LoggerFactory.getLogger(MavenMuteOutputHandler.class);

    public MavenMuteOutputHandler() {}

    @Override
    public void consumeLine(String s) {
        this.logger.debug(s);
    }
}
