package fr.inria.spirals.repairnator.process.step.feedback;

/**
 * Created by urli on 10/07/2017.
 *
 * Refactored by andre15silva on 16/01/2021
 */
public class NPEFeedback extends AbstractNPEFeedbackStep {
    public static final String TOOL_NAME = "NPEFix";

    public NPEFeedback() {
    }

	@Override
	public String getRepairToolName() {
		return TOOL_NAME;
	}

}
