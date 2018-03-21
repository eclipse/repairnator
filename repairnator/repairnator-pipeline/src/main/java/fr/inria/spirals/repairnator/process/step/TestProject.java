package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.states.PipelineState;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import fr.inria.spirals.repairnator.process.maven.output.MavenFilterTestOutputHandler;

/**
 * Created by urli on 03/01/2017.
 */
public class TestProject extends AbstractStep {

    public TestProject(ProjectInspector inspector) {
        super(inspector);
    }

    public TestProject(ProjectInspector inspector, String stepName) {
        super(inspector, stepName);
    }

    protected void businessExecute() {
        this.getLogger().debug("Launching tests with maven...");

        MavenHelper helper = new MavenHelper(this.getPom(), "test", null, this.getClass().getSimpleName(),
                this.inspector, false);

        MavenFilterTestOutputHandler outputTestFilter = new MavenFilterTestOutputHandler(helper);
        helper.setOutputHandler(outputTestFilter);

        int result = MavenHelper.MAVEN_ERROR;
        try {
            result = helper.run();
        } catch (InterruptedException e) {
            this.addStepError("Error while executing maven goal", e);
        }

        if (result == MavenHelper.MAVEN_SUCCESS) {
            if (outputTestFilter.getRunningTests() > 0) {
                this.getLogger().debug(outputTestFilter.getRunningTests() + " tests has been launched but none failed.");
            } else {
                this.addStepError("No test recorded.");
            }
            this.setPipelineState(PipelineState.NOTFAILING);
        } else {
            if (outputTestFilter.isFailingWithTest()) {
                this.getLogger().debug(outputTestFilter.getFailingTests() + " tests failed, go to next step.");
                this.setPipelineState(PipelineState.TESTABLE);
            } else {
                this.addStepError("Error while testing the project.");
                this.setPipelineState(PipelineState.NOTTESTABLE);
                this.shouldStop = true;
            }
        }
    }

}
