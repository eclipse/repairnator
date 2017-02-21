package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.jtravis.entities.BuildStatus;
import fr.inria.spirals.repairnator.process.ProjectState;

/**
 * Created by fermadeiral.
 */
public class BuildShouldFail implements ContractForGatherTestInformation {

    @Override
    public void makeADecision(GatherTestInformation gatherTestInformation) {
        if (gatherTestInformation.getState() == ProjectState.HASTESTFAILURE) {
            gatherTestInformation.shouldStop = false;
            gatherTestInformation.inspector.setReproducedAsFail(true);
            if (gatherTestInformation.inspector.isAboutAPreviousBuild()) {
                if (gatherTestInformation.inspector.getPreviousBuild().getBuildStatus() == BuildStatus.FAILED) {
                    // So, 1) the current passing build can be reproduced and 2)
                    // its previous build is a failing build with failing tests
                    // and it can also be reproduced
                    gatherTestInformation.setState(ProjectState.FIXERBUILD_CASE1);
                } else {
                    // So, 1) the current passing build can be reproduced and 2)
                    // its previous build is a passing build that fails when
                    // tested with new tests and it can also be reproduced
                    gatherTestInformation.setState(ProjectState.FIXERBUILD_CASE2);
                }
            }
        } else {
            if (gatherTestInformation.getState() == ProjectState.HASTESTERRORS) {
                if (gatherTestInformation.inspector.isAboutAPreviousBuild()) {
                    gatherTestInformation.shouldStop = true;
                } else {
                    gatherTestInformation
                            .addStepError("Only get test errors, no failing tests. It will try to repair it.");
                    gatherTestInformation.shouldStop = false;
                    gatherTestInformation.inspector.setReproducedAsError(true);
                }
            } else {
                gatherTestInformation.shouldStop = true;
            }
        }
    }

}
