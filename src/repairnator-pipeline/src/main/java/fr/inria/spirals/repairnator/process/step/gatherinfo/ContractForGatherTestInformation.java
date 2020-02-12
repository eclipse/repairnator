package fr.inria.spirals.repairnator.process.step.gatherinfo;

import fr.inria.spirals.repairnator.process.step.StepStatus;

/**
 * Created by fermadeiral.
 */
public interface ContractForGatherTestInformation {

    StepStatus shouldBeStopped(GatherTestInformation gatherTestInformation);
}
