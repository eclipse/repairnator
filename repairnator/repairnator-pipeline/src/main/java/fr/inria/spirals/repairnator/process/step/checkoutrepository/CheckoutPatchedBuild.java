package fr.inria.spirals.repairnator.process.step.checkoutrepository;

import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.states.PipelineState;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;

/**
 * Created by fernanda on 02/03/17.
 */
public class CheckoutPatchedBuild extends CheckoutRepository {

    public CheckoutPatchedBuild(ProjectInspector inspector, boolean blockingStep) {
        super(inspector, blockingStep);
    }

    public CheckoutPatchedBuild(ProjectInspector inspector, boolean blockingStep, String stepName) {
        super(inspector, blockingStep, stepName);
    }

    protected StepStatus businessExecute() {
        this.getLogger().debug("Checking out patched build candidate...");

        if (this.getInspector().getPatchedBuild() != null) {
            super.setCheckoutType(CheckoutType.CHECKOUT_PATCHED_BUILD);

            StepStatus stepStatus = super.businessExecute();

            this.getInspector().setCheckoutType(CheckoutType.CHECKOUT_PATCHED_BUILD);
            return stepStatus;
        } else {
            this.addStepError("There is no patched build retrieved. This will stop now.");
            return StepStatus.buildError(this, PipelineState.BUILDNOTCHECKEDOUT);
        }
    }

}
