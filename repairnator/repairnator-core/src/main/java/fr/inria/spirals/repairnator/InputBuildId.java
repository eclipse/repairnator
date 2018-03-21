package fr.inria.spirals.repairnator;

/**
 * Created by fermadeiral
 */
public class InputBuildId {

    public static final int NO_PATCH = -1;

    private int buggyBuildId;
    private int patchedBuildId = NO_PATCH;

    public InputBuildId(int buggyBuildId) {
        this.buggyBuildId = buggyBuildId;
    }

    public InputBuildId(int buggyBuildId, int patchedBuildId) {
        this(buggyBuildId);
        this.patchedBuildId = patchedBuildId;
    }

    public int getBuggyBuildId() {
        return this.buggyBuildId;
    }

    public int getPatchedBuildId() {
        return this.patchedBuildId;
    }

}
