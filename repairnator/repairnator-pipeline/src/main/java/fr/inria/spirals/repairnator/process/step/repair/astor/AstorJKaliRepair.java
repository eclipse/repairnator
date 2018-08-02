package fr.inria.spirals.repairnator.process.step.repair.astor;

/**
 * Created by urli on 17/08/2017.
 */
public class AstorJKaliRepair extends AstorRepair {
    protected static final String TOOL_NAME = "AstorJKali";
    private static final int MAX_TIME_EXECUTION = 100; // in minutes

    public AstorJKaliRepair() {}

    @Override
    public String getAstorMode() {
        return "jkali";
    }

    @Override
    public String getRepairToolName() {
        return TOOL_NAME;
    }

}
