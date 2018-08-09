package fr.inria.spirals.repairnator.process.step.gatherinfo;

import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.states.PipelineState;

/**
 * Created by fermadeiral.
 */
public class BuildShouldFail implements ContractForGatherTestInformation {

    @Override
    public StepStatus shouldBeStopped(GatherTestInformation gatherTestInformation) {
        ProjectInspector inspector = gatherTestInformation.getInspector();
        if (gatherTestInformation.getNbErroringTests() + gatherTestInformation.getNbFailingTests() > 0) {
            inspector.getJobStatus().setReproducedAsFail(true);
            return StepStatus.buildSuccess(gatherTestInformation);
        } else {
            return StepStatus.buildError(gatherTestInformation, PipelineState.NOTFAILING);
        }
    }

}
