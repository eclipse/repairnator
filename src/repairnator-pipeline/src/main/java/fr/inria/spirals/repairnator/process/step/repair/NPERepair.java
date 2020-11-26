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

	@Override
	public String getRepairToolName() {
		return TOOL_NAME;
	}

}
