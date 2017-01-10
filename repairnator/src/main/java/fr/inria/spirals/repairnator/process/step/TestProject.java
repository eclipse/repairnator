package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.Launcher;
import fr.inria.spirals.repairnator.process.ProjectInspector;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;

/**
 * Created by urli on 03/01/2017.
 */
public class TestProject extends AbstractStep {
    public TestProject(ProjectInspector inspector) {
        super(inspector);
    }

    protected void businessExecute() {
        this.getLogger().debug("Start launching tests with maven.");

        MavenHelper helper = new MavenHelper(this.getPom(), "test", null, this.getClass().getName(), this.inspector, false);

        int result = helper.run();

        if (result == MavenHelper.MAVEN_SUCCESS) {
            this.getLogger().info("Repository "+this.inspector.getRepoSlug()+" has passing all tests: then no test can be fixed...");
            this.shouldStop = true;
        } else {
            this.state = ProjectState.TESTABLE;
        }
    }
}
