package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.ProjectState;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import fr.inria.spirals.repairnator.process.maven.output.MavenFilterTestOutputHandler;

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

        MavenFilterTestOutputHandler outputTestFilter = new MavenFilterTestOutputHandler(this.inspector, this.getClass().getName());
        helper.setOutputHandler(outputTestFilter);

        int result = helper.run();

        // in both case we want to gather test information, then we process to the next step.
        if (result == MavenHelper.MAVEN_SUCCESS) {
            if (outputTestFilter.getRunningTests() > 0) {
                this.getLogger().debug(outputTestFilter.getRunningTests()+" tests has been launched but none failed.");
            } else {
                this.addStepError("No test recorded.");
            }
            this.state = ProjectState.NOTFAILING;
        } else {
            if (outputTestFilter.isFailingWithTest()) {
                this.getLogger().debug(outputTestFilter.getFailingTests()+" tests failed, go to next step.");
                this.state = ProjectState.TESTABLE;
            } else {
                this.addStepError("Error while testing the project.");
                this.shouldStop = true;
                this.state = ProjectState.NOTTESTABLE;
            }
        }
    }
}
