package fr.inria.spirals.repairnator.process.step.repair;

import fr.inria.spirals.repairnator.process.step.StepStatus;

/**
 * Created by Benjamin Tellström on 14/06/2019
 *
 * Refactored by andre15silva on 16/01/2021
 */
public class NPERepairSafe extends AbstractNPERepairStep {
    public static final String TOOL_NAME = "NPEFixSafe";

    public NPERepairSafe() {
        super();
        this.selection = "safe-mono";
    }

    @Override
    public String getRepairToolName() {
        return TOOL_NAME;
    }

    @Override
    protected StepStatus businessExecute() {
        return super.businessExecute();
    }

}

