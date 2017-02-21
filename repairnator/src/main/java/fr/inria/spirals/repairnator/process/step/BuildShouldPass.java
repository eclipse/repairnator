package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.process.ProjectState;

/**
 * Created by fermadeiral.
 */
public class BuildShouldPass implements ContractForGatherTestInformation {

    @Override
    public boolean shouldBeStopped(GatherTestInformation gatherTestInformation) {
        if (gatherTestInformation.getState() == ProjectState.HASTESTFAILURE
                || gatherTestInformation.getState() == ProjectState.HASTESTERRORS) {
            return true;
        }
        return false;
    }

}
