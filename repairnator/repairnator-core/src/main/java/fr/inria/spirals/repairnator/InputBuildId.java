package fr.inria.spirals.repairnator;

/**
 * This class represents a Build input for both Repairnator and BEARS
 * For repairnator it's only a buggy build id, but for BEARS it's a pair of buggy build id and patched build id.
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
