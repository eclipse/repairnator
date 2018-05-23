package fr.inria.spirals.repairnator;

/**
 * Created by fermadeiral
 */
public class InputBuildId {

    public static final int NO_PATCH = -1;

    private long buggyBuildId;
    private long patchedBuildId = NO_PATCH;

    public InputBuildId(long buggyBuildId) {
        this.buggyBuildId = buggyBuildId;
    }

    public InputBuildId(long buggyBuildId, long patchedBuildId) {
        this(buggyBuildId);
        this.patchedBuildId = patchedBuildId;
    }

    public long getBuggyBuildId() {
        return this.buggyBuildId;
    }

    public long getPatchedBuildId() {
        return this.patchedBuildId;
    }

}
