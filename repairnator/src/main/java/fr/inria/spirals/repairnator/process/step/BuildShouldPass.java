package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.process.ProjectState;

/**
 * Created by fermadeiral.
 */
public class BuildShouldPass implements ContractForGatherTestInformation {

    @Override
    public void shouldBeStopped(GatherTestInformation gatherTestInformation) {
        if (gatherTestInformation.getState() == ProjectState.HASTESTFAILURE
                || gatherTestInformation.getState() == ProjectState.HASTESTERRORS) {
            gatherTestInformation.shouldStop = true;
        }
    }

}
