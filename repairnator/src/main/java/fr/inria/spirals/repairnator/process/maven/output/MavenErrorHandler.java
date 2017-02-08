package fr.inria.spirals.repairnator.process.maven.output;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by urli on 09/01/2017.
 */
public class MavenErrorHandler implements InvocationOutputHandler {

    private final Logger logger = LoggerFactory.getLogger(MavenErrorHandler.class);
    private ProjectInspector inspector;
    private String name;

    public MavenErrorHandler(ProjectInspector inspector, String name) {
        this.inspector = inspector;
        this.name = name;
    }

    @Override
    public void consumeLine(String s) {
        this.logger.error(s);
        this.inspector.addStepError(name, s);
    }
}
