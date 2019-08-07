package fr.inria.spirals.repairnator.process.inspectors;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
    private double overfittingScore;

    public RepairPatch(String toolname, String filePath, String diff) {
        this.toolname = toolname;
        this.filePath = filePath;
        this.diff = diff;
        System.out.println(">>>>>>>>>>>>>>>>");
        System.out.println(diff);
        System.out.println("<<<<<<<<<<<<<<<<");
        this.overfittingScore = computeOverfittingScore();
    }

    private double computeOverfittingScore() {
        try {
            File buggyFile = new File(filePath);
            String tmpName = buggyFile.getName();
            // TODO
            File patchedFile = Files.createTempFile(tmpName, ".java").toFile();
            return 0;
//            return computeOverfittingScore(buggyFile, patchedFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public double getOverfittingScore() {
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
