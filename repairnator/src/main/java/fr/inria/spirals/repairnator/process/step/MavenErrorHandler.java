package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.process.ProjectInspector;
import org.apache.maven.shared.invoker.InvocationOutputHandler;

/**
 * Created by urli on 09/01/2017.
 */
public class MavenErrorHandler implements InvocationOutputHandler {

    private ProjectInspector inspector;
    private String name;

    public MavenErrorHandler(ProjectInspector inspector, String name) {
        this.inspector = inspector;
        this.name = name;
    }

    @Override
    public void consumeLine(String s) {
        this.inspector.addStepError(name, s);
    }
}
