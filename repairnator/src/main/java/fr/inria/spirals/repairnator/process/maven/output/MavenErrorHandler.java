package fr.inria.spirals.repairnator.process.maven.output;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by urli on 09/01/2017.
 */
public class MavenErrorHandler extends MavenOutputHandler {

    public MavenErrorHandler(ProjectInspector inspector, String name) {
        super(inspector, name);
    }

    @Override
    public void consumeLine(String s) {
        super.consumeLine(s);

        this.getLogger().error(s);
        this.inspector.addStepError(name, s);
    }
}
