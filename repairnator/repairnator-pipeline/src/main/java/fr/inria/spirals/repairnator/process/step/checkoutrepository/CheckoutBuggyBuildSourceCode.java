package fr.inria.spirals.repairnator.process.step.checkoutrepository;

import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.states.PipelineState;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;

/**
 * Created by fermadeiral.
 */
public class CheckoutBuggyBuildSourceCode extends CheckoutRepository {

    public CheckoutBuggyBuildSourceCode(ProjectInspector inspector, boolean blockingStep) {
        super(inspector, blockingStep);
    }

    protected StepStatus businessExecute() {
        this.getLogger().debug("Checking out the source code of the previous build...");

        if (this.getInspector().getJobStatus().getRepairSourceDir() == null) {
            this.getLogger().error("Repair source dir is null: it is therefore impossible to continue.");
            return StepStatus.buildError("Repair source dir is null: it is therefore impossible to continue.");
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
