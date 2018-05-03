package fr.inria.spirals.repairnator.process.step.gatherinfo;

import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;

/**
 * Created by fermadeiral.
 */
public class BuildShouldFail implements ContractForGatherTestInformation {

    @Override
    public StepStatus shouldBeStopped(GatherTestInformation gatherTestInformation) {
        ProjectInspector inspector = gatherTestInformation.getInspector();
        if (gatherTestInformation.getNbErroringTests() + gatherTestInformation.getNbFailingTests() > 0) {
            inspector.getJobStatus().setReproducedAsFail(true);
            return StepStatus.buildSuccess();
        } else {
            return StepStatus.buildError("No failing test recorded on " + gatherTestInformation.getNbRunningTests() + " running tests");
        }
    }

}
