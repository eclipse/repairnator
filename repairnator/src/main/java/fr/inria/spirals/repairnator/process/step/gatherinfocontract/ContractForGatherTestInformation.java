package fr.inria.spirals.repairnator.process.step.gatherinfocontract;

import fr.inria.spirals.repairnator.process.step.GatherTestInformation;

/**
 * Created by fermadeiral.
 */
public interface ContractForGatherTestInformation {

    boolean shouldBeStopped(GatherTestInformation gatherTestInformation);

}
