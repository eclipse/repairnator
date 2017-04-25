package fr.inria.spirals.repairnator.process.step.gatherinfo;

import fr.inria.spirals.repairnator.LauncherMode;
import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;

/**
 * Created by fermadeiral.
 */
public class BuildShouldFail implements ContractForGatherTestInformation {

    @Override
    public boolean shouldBeStopped(GatherTestInformation gatherTestInformation) {
        ProjectInspector inspector = gatherTestInformation.getInspector();
        if (gatherTestInformation.getState() == ProjectState.HASTESTFAILURE) {
            inspector.getJobStatus().setReproducedAsFail(true);
            return false;
        } else {
            if (gatherTestInformation.getState() == ProjectState.HASTESTERRORS) {
                if (RepairnatorConfig.getInstance().getLauncherMode() == LauncherMode.BEARS) {
                    return true;
                } else {
                    gatherTestInformation.addStepError("Only get test errors, no failing tests. It will try to repair it.");
                    inspector.getJobStatus().setReproducedAsError(true);
                    return false;
                }
            } else {
                return true;
            }
        }
    }

}
