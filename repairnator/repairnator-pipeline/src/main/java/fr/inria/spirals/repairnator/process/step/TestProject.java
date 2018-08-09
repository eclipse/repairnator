package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import fr.inria.spirals.repairnator.process.maven.output.MavenFilterOutputHandler;
import fr.inria.spirals.repairnator.states.PipelineState;

/**
 * This step only launch mvn test. IT DOES NOT PROCESS THE RESULTS OF THE TEST.
 * See {@link fr.inria.spirals.repairnator.process.step.gatherinfo.GatherTestInformation} for the process of the tests.
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

        // we cannot rely on the bash result of the command here: it is erroring (!= 0) if the mvn test fail
        // but it might mean a success for us
        // so we consider this step is always successful unless it has been interrupted.
        try {
            helper.run();
        } catch (InterruptedException e) {
            this.addStepError("Error while testing the project.", e);
            return StepStatus.buildError(this, PipelineState.NOTTESTABLE);
        }
        return StepStatus.buildSuccess(this);
    }

}
