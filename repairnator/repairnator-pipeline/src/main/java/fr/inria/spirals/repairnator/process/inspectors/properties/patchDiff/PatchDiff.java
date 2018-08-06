package fr.inria.spirals.repairnator.process.inspectors.properties.patchDiff;

public class PatchDiff {

    private Files files;
    private Lines lines;

    public PatchDiff() {
        this.files = new Files();
        this.lines = new Lines();
    }

    public Files getFiles() {
        return files;
    }

    public Lines getLines() {
        return lines;
    }
}
