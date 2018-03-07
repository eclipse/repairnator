package fr.inria.spirals.repairnator;

/**
 * Created by fermadeiral
 */
public class InputBuildId {

    private int buggyBuildId;
    private int patchedBuildId;

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
