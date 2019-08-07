package fr.inria.spirals.repairnator.process.inspectors;

import java.util.Objects;

public class RepairPatch {
    /**
     * Name of the tool which produces the patch
     */
    private String toolname;

    /**
     * Path of the file to patch
     */
    private String filePath;

    /**
     * Diff of the file
     */
    private String diff;

    /**
     * Score of the overfitting likelihood
     */
    private float overfittingScore;

    public RepairPatch(String toolname, String filePath, String diff) {
        this.toolname = toolname;
        this.filePath = filePath;
        this.diff = diff;
        this.overfittingScore = computeOverfittingScore();
    }

    private float computeOverfittingScore() {
        // todo
        //  here we need two versions of code files
        //  then invoke functional module of Coming
        return 0;
    }

    public String getToolname() {
        return toolname;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getDiff() {
        return diff;
    }

    public float getOverfittingScore() {
        return overfittingScore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final RepairPatch that = (RepairPatch) o;
        return Objects.equals(toolname, that.toolname) &&
                Objects.equals(filePath, that.filePath) &&
                Objects.equals(diff, that.diff);
    }

    @Override
    public int hashCode() {

        return Objects.hash(toolname, filePath, diff);
    }
}
