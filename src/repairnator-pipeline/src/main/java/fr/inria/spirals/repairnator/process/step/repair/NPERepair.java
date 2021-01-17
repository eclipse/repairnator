package fr.inria.spirals.repairnator.process.step.repair;

/**
 * Created by urli on 10/07/2017.
 *
 * Refactored by andre15silva on 16/01/2021
 */
public class NPERepair extends AbstractNPERepairStep {
    public static final String TOOL_NAME = "NPEFix";

    public NPERepair() {
    }

    public NPERepair(String selection, int nbIteration, String scope, String repairStrategy) {
        super(selection, nbIteration, scope, repairStrategy);
    }

    @Override
    public String getRepairToolName() {
        return TOOL_NAME;
    }

}
