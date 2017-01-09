package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.Launcher;
import fr.inria.spirals.repairnator.process.ProjectInspector;

/**
 * Created by urli on 03/01/2017.
 */
public class TestProject extends BuildProject {
    public TestProject(ProjectInspector inspector) {
        super(inspector);
    }

    protected void businessExecute() {
        this.getLogger().debug("Start launching tests with maven.");

        System.setProperty("maven.surefire.timeout","1");
        int result = this.mavenBuild(true);
        System.clearProperty("maven.surefire.timeout");

        if (result == 0) {
            this.getLogger().info("Repository "+this.inspector.getRepoSlug()+" has passing all tests: then no test can be fixed...");
            this.shouldStop = true;
        } else {
            this.state = ProjectState.TESTABLE;
        }
    }
}
