package fr.inria.spirals.repairnator.process.step.checkoutrepository;

import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.states.PipelineState;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;

public class CheckoutBuggyBuildTestCode extends CheckoutRepository {

    public CheckoutBuggyBuildTestCode(ProjectInspector inspector, boolean blockingStep) {
        super(inspector, blockingStep);
    }

    protected StepStatus businessExecute() {
        this.getLogger().debug("Checking out the test code of the buggy build candidate...");

        if (this.getInspector().getJobStatus().getTestDir() == null) {
            this.addStepError("Test code dir is null: it is therefore impossible to continue.");
            return StepStatus.buildError(this, PipelineState.TESTDIRNOTCOMPUTED);
        }

        super.setCheckoutType(CheckoutType.CHECKOUT_BUGGY_BUILD_TEST_CODE);

        StepStatus stepStatus = super.businessExecute();

        this.getInspector().setCheckoutType(CheckoutType.CHECKOUT_BUGGY_BUILD_TEST_CODE);

        return stepStatus;
    }

}
