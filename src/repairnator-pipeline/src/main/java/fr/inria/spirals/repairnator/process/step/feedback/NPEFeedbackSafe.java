package fr.inria.spirals.repairnator.process.step.feedback;

import fr.inria.spirals.repairnator.process.step.StepStatus;

/**
 * Created by Benjamin Tellström on 14/06/2019
 *
 * Refactored by andre15silva on 16/01/2021
 */
public class NPEFeedbackSafe extends AbstractNPEFeedbackStep {
    public static final String TOOL_NAME = "NPEFixSafe";

    public NPEFeedbackSafe() {
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

