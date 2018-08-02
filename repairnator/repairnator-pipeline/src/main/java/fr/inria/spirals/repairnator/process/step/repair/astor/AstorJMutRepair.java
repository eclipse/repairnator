package fr.inria.spirals.repairnator.process.step.repair.astor;

public class AstorJMutRepair extends AstorRepair {
    protected static final String TOOL_NAME = "AstorJMut";
    private static final int MAX_TIME_EXECUTION = 100; // in minutes

    public AstorJMutRepair() {}

    @Override
    public String getAstorMode() {
        return "jmutrepair";
    }

    @Override
    public String getRepairToolName() {
        return TOOL_NAME;
    }
}
