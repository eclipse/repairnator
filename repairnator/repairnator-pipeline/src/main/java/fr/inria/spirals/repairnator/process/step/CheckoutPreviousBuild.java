package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutRepository;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutType;

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

        this.setState((this.shouldStop) ? ProjectState.PREVIOUSBUILDNOTCHECKEDOUT : ProjectState.PREVIOUSBUILDCHECKEDOUT);
    }

}
