package fr.inria.spirals.repairnator.process.maven.output;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;

/**
 * Created by urli on 09/01/2017.
 */
public class MavenFilterOutputHandler extends MavenOutputHandler {

    public MavenFilterOutputHandler(ProjectInspector inspector, String name) {
        super(inspector, name);
    }

    @Override
    public void consumeLine(String s) {
        super.consumeLine(s);

        this.getLogger().debug(s);
        if (s.contains("[ERROR]")) {
            this.inspector.addStepError(name, s);
        }
    }
}
