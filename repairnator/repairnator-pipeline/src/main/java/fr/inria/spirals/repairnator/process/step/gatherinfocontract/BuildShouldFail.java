package fr.inria.spirals.repairnator.process.step.gatherinfocontract;

import fr.inria.spirals.repairnator.process.ProjectState;
import fr.inria.spirals.repairnator.scanner.ScannedBuildStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector4Bears;
import fr.inria.spirals.repairnator.process.step.GatherTestInformation;

/**
 * Created by fermadeiral.
 */
public class BuildShouldFail implements ContractForGatherTestInformation {

    @Override
    public boolean shouldBeStopped(GatherTestInformation gatherTestInformation) {
        ProjectInspector inspector = gatherTestInformation.getInspector();
        if (gatherTestInformation.getState() == ProjectState.HASTESTFAILURE) {
            inspector.setReproducedAsFail(true);
            if (inspector.isAboutAPreviousBuild()) {
                if (inspector.getBuildToBeInspected().getStatus() == ScannedBuildStatus.PASSING_AND_FAIL) {
                    // So, 1) the current passing build can be reproduced and 2)
                    // its previous build is a failing build with failing tests
                    // and it can also be reproduced
                    gatherTestInformation.setState(ProjectState.FIXERBUILD_CASE1);
                    if (inspector instanceof ProjectInspector4Bears) {
                        ((ProjectInspector4Bears) inspector).setFixerBuildCase1(true);
                    }
                } else {
                    if (inspector.getBuildToBeInspected().getStatus() == ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES) {
                        // So, 1) the current passing build can be reproduced and 2)
                        // its previous build is a passing build that fails when
                        // tested with new tests and it can also be reproduced
                        gatherTestInformation.setState(ProjectState.FIXERBUILD_CASE2);
                        if (inspector instanceof ProjectInspector4Bears) {
                            ((ProjectInspector4Bears) inspector).setFixerBuildCase2(true);
                        }
                    } else {
                        return true;
                    }
                }
            }
            return false;
        } else {
            if (gatherTestInformation.getState() == ProjectState.HASTESTERRORS) {
                if (inspector.isAboutAPreviousBuild()) {
                    return true;
                } else {
                    gatherTestInformation.addStepError("Only get test errors, no failing tests. It will try to repair it.");
                    inspector.setReproducedAsError(true);
                    return false;
                }
            } else {
                return true;
            }
        }
    }

}
