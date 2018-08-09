package fr.inria.spirals.repairnator.process.step.gatherinfo;

import fr.inria.spirals.repairnator.process.step.StepStatus;
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
    public StepStatus shouldBeStopped(GatherTestInformation gatherTestInformation) {
        ProjectInspector inspector = gatherTestInformation.getInspector();

        if (gatherTestInformation.getNbFailingTests() + gatherTestInformation.getNbErroringTests() == 0 && gatherTestInformation.getNbRunningTests() > 0) {
            if (RepairnatorConfig.getInstance().getLauncherMode() == LauncherMode.BEARS && inspector instanceof ProjectInspector4Bears) {
                if (inspector.getBuildToBeInspected().getStatus() == ScannedBuildStatus.FAILING_AND_PASSING) {
                    // So, 1) the current passing build can be reproduced and 2) its previous build is a failing build
                    // with failing tests and it can also be reproduced
                    ((ProjectInspector4Bears) inspector).setBug(true, PipelineState.BUG_FAILING_PASSING.name());
                } else if (inspector.getBuildToBeInspected().getStatus() == ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES) {
                    // So, 1) the current passing build can be reproduced and 2) its previous build is a passing build
                    // that fails when tested with new tests and it can also be reproduced
                    ((ProjectInspector4Bears) inspector).setBug(true, PipelineState.BUG_PASSING_PASSING.name());
                }
            }
            return StepStatus.buildSuccess(gatherTestInformation);
        }

        if (gatherTestInformation.getNbRunningTests() == 0) {
            return StepStatus.buildError(gatherTestInformation, PipelineState.TESTERRORS);
        } else {
            return StepStatus.buildError(gatherTestInformation, PipelineState.TESTFAILURES);
        }
    }

}
