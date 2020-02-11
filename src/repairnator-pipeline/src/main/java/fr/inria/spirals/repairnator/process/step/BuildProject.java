package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import fr.inria.spirals.repairnator.states.PipelineState;

import java.util.Properties;

/**
 * Created by urli on 03/01/2017.
 */
public class BuildProject extends AbstractStep {

    public BuildProject(ProjectInspector inspector) {
        super(inspector, true);
    }

    public BuildProject(ProjectInspector inspector, boolean blockingStep, String stepName) {
        super(inspector, blockingStep, stepName);
    }

    protected StepStatus businessExecute() {
        this.getLogger().debug("Building project by installing artifacts with maven (skip test execution)...");

        Properties properties = new Properties();
        properties.setProperty(MavenHelper.SKIP_TEST_PROPERTY, "true");

        MavenHelper helper = new MavenHelper(this.getPom(), "install", properties, this.getClass().getSimpleName(), this.getInspector(), true);

        int result;
        try {
            result = helper.run();
        } catch (InterruptedException e) {
            this.addStepError("Error while building", e);
            result = MavenHelper.MAVEN_ERROR;
        }

        if (result == MavenHelper.MAVEN_SUCCESS) {
            return StepStatus.buildSuccess(this);
        } else {
            this.addStepError("Repository " + this.getInspector().getRepoSlug() + " cannot be built.");
            return StepStatus.buildError(this, PipelineState.NOTBUILDABLE);
        }
    }

}
