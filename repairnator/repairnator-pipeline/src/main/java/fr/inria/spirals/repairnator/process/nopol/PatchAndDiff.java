package fr.inria.spirals.repairnator.process.nopol;

import fr.inria.lille.repair.common.patch.Patch;

public class PatchAndDiff {
    private Patch patch;
    private String diff;

    public PatchAndDiff(Patch patch, String diff) {
        this.patch = patch;
        this.diff = diff;
    }

    public Patch getPatch() {
        return patch;
    }

    public String getDiff() {
        return diff;
    }
}
