package fr.inria.spirals.repairnator.process.step.checkoutrepository;

import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.states.PipelineState;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;

/**
 * Created by fermadeiral.
 */
public class CheckoutBuggyBuildSourceCode extends CheckoutRepository {

    public CheckoutBuggyBuildSourceCode(ProjectInspector inspector, boolean blockingStep) {
        super(inspector, blockingStep);
    }

    public CheckoutBuggyBuildSourceCode(ProjectInspector inspector, boolean blockingStep, String stepName) {
        super(inspector, blockingStep, stepName);
    }

    protected StepStatus businessExecute() {
        this.getLogger().debug("Checking out the source code of the buggy build candidate...");

        if (this.getInspector().getJobStatus().getRepairSourceDir() == null) {
            this.addStepError("Source code dir is null: it is therefore impossible to continue.");
            return StepStatus.buildError(this, PipelineState.SOURCEDIRNOTCOMPUTED);
        }

        super.setCheckoutType(CheckoutType.CHECKOUT_PATCHED_BUILD);

        StepStatus stepStatus = super.businessExecute();

        if (stepStatus.isSuccess()) {
            super.setCheckoutType(CheckoutType.CHECKOUT_BUGGY_BUILD_SOURCE_CODE);
            stepStatus = super.businessExecute();
        }

        this.getInspector().setCheckoutType(getCheckoutType());
        return stepStatus;
    }

}
