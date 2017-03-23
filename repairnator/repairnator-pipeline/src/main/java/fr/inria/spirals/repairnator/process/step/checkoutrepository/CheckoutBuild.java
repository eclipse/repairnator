package fr.inria.spirals.repairnator.process.step.checkoutrepository;

import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutRepository;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutType;

/**
 * Created by fernanda on 02/03/17.
 */
public class CheckoutBuild extends CheckoutRepository {

    public CheckoutBuild(ProjectInspector inspector) {
        super(inspector);
    }

    protected void businessExecute() {
        this.getLogger().debug("Checking out build...");

        super.setCheckoutType(CheckoutType.CHECKOUT_BUILD);

        super.businessExecute();

        this.setState((this.shouldStop) ? ProjectState.BUILDNOTCHECKEDOUT : ProjectState.BUILDCHECKEDOUT);
    }

}
