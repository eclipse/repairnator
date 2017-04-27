package fr.inria.spirals.repairnator.process.step.gatherinfo;

import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.states.PipelineState;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector4Bears;

/**
 * Created by fermadeiral.
 */
public class BuildShouldPass implements ContractForGatherTestInformation {

    @Override
    public boolean shouldBeStopped(GatherTestInformation gatherTestInformation) {
        ProjectInspector inspector = gatherTestInformation.getInspector();
        if (gatherTestInformation.getPipelineState() == PipelineState.NOTFAILING) {
            if (RepairnatorConfig.getInstance().getLauncherMode() == LauncherMode.BEARS) {
                if (inspector.getBuildToBeInspected().getStatus() == ScannedBuildStatus.FAILING_AND_PASSING) {
                    // So, 1) the current passing build can be reproduced and 2) its previous build is a failing build
                    // with failing tests and it can also be reproduced
                    gatherTestInformation.setPipelineState(PipelineState.FIXERBUILDCASE1);
                    if (inspector instanceof ProjectInspector4Bears) {
                        ((ProjectInspector4Bears) inspector).setFixerBuildCase1(true);
                    }
                } else if (inspector.getBuildToBeInspected().getStatus() == ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES) {
                    // So, 1) the current passing build can be reproduced and 2) its previous build is a passing build
                    // that fails when tested with new tests and it can also be reproduced
                    gatherTestInformation.setPipelineState(PipelineState.FIXERBUILDCASE2);
                    if (inspector instanceof ProjectInspector4Bears) {
                        ((ProjectInspector4Bears) inspector).setFixerBuildCase2(true);
                    }
                } else {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

}
