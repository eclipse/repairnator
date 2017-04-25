package fr.inria.spirals.repairnator.process.step.checkoutrepository;

import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;

/**
 * Created by fernanda on 02/03/17.
 */
public class CheckoutPatchedBuild extends CheckoutRepository {

    public CheckoutPatchedBuild(ProjectInspector inspector) {
        super(inspector);
    }

    protected void businessExecute() {
        this.getLogger().debug("Checking out build...");

        super.setCheckoutType(CheckoutType.CHECKOUT_PATCHED_BUILD);

        super.businessExecute();

        if (this.shouldStop) {
            this.setState(ProjectState.BUILDNOTCHECKEDOUT);
        } else {
            this.setState(ProjectState.BUILDCHECKEDOUT);
            inspector.setCheckoutType(getCheckoutType());
        }
    }

}
