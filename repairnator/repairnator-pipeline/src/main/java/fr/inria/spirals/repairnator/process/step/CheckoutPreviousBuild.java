package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;

/**
 * Created by fermadeiral.
 */
public class CheckoutPreviousBuild extends CheckoutRepository {

    public CheckoutPreviousBuild(ProjectInspector inspector) {
        super(inspector);
    }

    protected void businessExecute() {
        this.getLogger().debug("Checking out previous build...");

        this.inspector.setPreviousBuildFlag(true);

        super.setCheckoutType(CheckoutType.CHECKOUT_PREVIOUS_BUILD);

        super.businessExecute();

        this.state = (this.shouldStop) ? ProjectState.PREVIOUSBUILDNOTCHECKEDOUT : ProjectState.PREVIOUSBUILDCHECKEDOUT;
    }

}
