package fr.inria.spirals.repairnator.process.step.repair.astor;

/**
 * Created by urli on 17/08/2017.
 */
public class AstorJGenProgRepair extends AstorRepair {
    protected static final String TOOL_NAME = "AstorJGenProg";
    private static final int MAX_TIME_EXECUTION = 100; // in minutes

    public AstorJGenProgRepair() {}

    @Override
    public String getAstorMode() {
        return "jgenprog";
    }

    @Override
    public String getRepairToolName() {
        return TOOL_NAME;
    }
}
