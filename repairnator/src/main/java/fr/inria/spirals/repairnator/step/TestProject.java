package fr.inria.spirals.repairnator.step;

import fr.inria.spirals.repairnator.Launcher;
import fr.inria.spirals.repairnator.ProjectInspector;
import fr.inria.spirals.repairnator.ProjectState;

/**
 * Created by urli on 03/01/2017.
 */
public class TestProject extends BuildProject {

    public TestProject(ProjectInspector inspector) {
        super(inspector);
    }

    public void execute() {
        Launcher.LOGGER.debug("Start launching tests with maven.");
        int result = this.mavenBuild(true);

        if (result == 0) {
            Launcher.LOGGER.info("Repository "+this.inspector.getRepoSlug()+" has passing all tests: then no test can be fixed...");
        } else {
            this.state = ProjectState.HASTESTFAILURE;
            this.executeNextStep();
        }
    }
}
