package fr.inria.spirals.repairnator.process.step.checkoutrepository;

import fr.inria.spirals.repairnator.states.PipelineState;
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

        if (this.getInspector().getPatchedBuild() != null) {
            super.setCheckoutType(CheckoutType.CHECKOUT_PATCHED_BUILD);

            super.businessExecute();

            if (this.shouldStop) {
                this.setPipelineState(PipelineState.PATCHEDBUILDNOTCHECKEDOUT);
            } else {
                this.setPipelineState(PipelineState.PATCHEDBUILDCHECKEDOUT);
                inspector.setCheckoutType(getCheckoutType());
            }
        } else {
            this.addStepError("There is no patched build retrieved. This will stop now.");
            this.shouldStop = true;
        }


    }

}
