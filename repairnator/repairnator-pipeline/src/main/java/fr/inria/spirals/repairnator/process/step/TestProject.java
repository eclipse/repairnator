package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import fr.inria.spirals.repairnator.process.maven.output.MavenFilterOutputHandler;
import fr.inria.spirals.repairnator.states.PipelineState;

/**
 * Created by urli on 03/01/2017.
 */
public class TestProject extends AbstractStep {

    public TestProject(ProjectInspector inspector) {
        super(inspector, true);
    }

    public TestProject(ProjectInspector inspector, boolean blockingStep, String stepName) {
        super(inspector, blockingStep, stepName);
    }

    protected StepStatus businessExecute() {
        this.getLogger().debug("Launching tests with maven...");

        MavenHelper helper = new MavenHelper(this.getPom(), "test", null, this.getClass().getSimpleName(), this.getInspector(), false);

        MavenFilterOutputHandler outputFilter = new MavenFilterOutputHandler(helper);
        helper.setOutputHandler(outputFilter);

        try {
            helper.run();
        } catch (InterruptedException e) {
            this.addStepError("Error while testing the project.", e);
            return StepStatus.buildError(this, PipelineState.NOTTESTABLE);
        }
        return StepStatus.buildSuccess(this);
    }

}
