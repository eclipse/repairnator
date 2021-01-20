package fr.inria.spirals.repairnator;

import fr.inria.spirals.repairnator.states.LauncherMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class represents a Build input for both Repairnator and BEARS
 * For repairnator it's only a buggy build id, but for BEARS it's a pair of buggy build id and patched build id.
 */
public class TravisInputBuild implements InputBuild {

    public static final int NO_PATCH = -1;

    private long buggyBuildId;
    private long patchedBuildId = NO_PATCH;

    public TravisInputBuild(long buggyBuildId) {
        this.buggyBuildId = buggyBuildId;
    }

    public TravisInputBuild(long buggyBuildId, long patchedBuildId) {
        this(buggyBuildId);
        this.patchedBuildId = patchedBuildId;
    }

    public long getBuggyBuildId() {
        return this.buggyBuildId;
    }

    public long getPatchedBuildId() {
        return this.patchedBuildId;
    }

    @Override
    public List<String> getEnvVariables() {
        List<String> r = new ArrayList<String>();

        r.add("BUILD_ID=" + buggyBuildId);
        if(patchedBuildId != NO_PATCH){
            r.add("NEXT_BUILD_ID=" + buggyBuildId);
        }
        return r;
    }

    @Override
    public String toString() {
        return String.valueOf(buggyBuildId);
    }
}
