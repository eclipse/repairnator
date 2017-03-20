package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutRepository;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutType;

/**
 * Created by fermadeiral.
 */
public class CheckoutPreviousBuildSourceCode extends CheckoutRepository {

    public CheckoutPreviousBuildSourceCode(ProjectInspector inspector) {
        super(inspector);
    }

    protected void businessExecute() {
        this.getLogger().debug("Checking out the source code of the previous build...");

        super.setCheckoutType(CheckoutType.CHECKOUT_BUILD);

        super.businessExecute();

        super.setCheckoutType(CheckoutType.CHECKOUT_PREVIOUS_BUILD_SOURCE_CODE);

        super.businessExecute();

        this.setState((this.shouldStop) ? ProjectState.PREVIOUSBUILDCODENOTCHECKEDOUT : ProjectState.PREVIOUSBUILDCODECHECKEDOUT);
    }

}
