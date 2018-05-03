package fr.inria.spirals.repairnator.process.step.checkoutrepository;

import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.states.PipelineState;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;

/**
 * Created by fermadeiral.
 */
public class CheckoutBuggyBuild extends CheckoutRepository {

    public CheckoutBuggyBuild(ProjectInspector inspector, boolean blockingStep) {
        super(inspector, blockingStep);
    }

    protected StepStatus businessExecute() {
        this.getLogger().debug("Checking out previous build...");

        super.setCheckoutType(CheckoutType.CHECKOUT_BUGGY_BUILD);
        this.getInspector().setCheckoutType(CheckoutType.CHECKOUT_BUGGY_BUILD);
        return super.businessExecute();
    }

}
