package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.process.ProjectState;

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
