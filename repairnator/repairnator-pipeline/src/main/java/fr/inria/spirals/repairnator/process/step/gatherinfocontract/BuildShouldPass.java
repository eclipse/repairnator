package fr.inria.spirals.repairnator.process.step.gatherinfocontract;

import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.process.step.GatherTestInformation;

/**
 * Created by fermadeiral.
 */
public class BuildShouldPass implements ContractForGatherTestInformation {

    @Override
    public boolean shouldBeStopped(GatherTestInformation gatherTestInformation) {
        if (gatherTestInformation.getState() == ProjectState.NOTFAILING) {
            return false;
        }
        return true;
    }

}
