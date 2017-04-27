package fr.inria.spirals.repairnator.process.step.checkoutrepository;

import fr.inria.spirals.repairnator.states.PipelineState;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;

/**
 * Created by fermadeiral.
 */
public class CheckoutBuggyBuild extends CheckoutRepository {

    public CheckoutBuggyBuild(ProjectInspector inspector) {
        super(inspector);
    }

    protected void businessExecute() {
        this.getLogger().debug("Checking out previous build...");

        super.setCheckoutType(CheckoutType.CHECKOUT_BUGGY_BUILD);

        super.businessExecute();

        if (this.shouldStop) {
            this.setPipelineState(PipelineState.BUILDNOTCHECKEDOUT);
        } else {
            this.setPipelineState(PipelineState.BUILDCHECKEDOUT);
            inspector.setCheckoutType(getCheckoutType());
        }
    }

}
