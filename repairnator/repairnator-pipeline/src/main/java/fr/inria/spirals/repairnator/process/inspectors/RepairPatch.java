package fr.inria.spirals.repairnator.process.inspectors;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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
        // read from buggyFile
        List<String> buggyLines = new ArrayList<>();
        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.forEach(buggyLines::add);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // prepare patches
        List<String> diffLines = Arrays.asList(diff.split("\n"));
        Patch<String> patches = UnifiedDiffUtils.parseUnifiedDiff(diffLines);

        try {
            // create patchedFile
            File buggyFile = new File(filePath);
            String tmpName = buggyFile.getName();
            File patchedFile = Files.createTempFile(tmpName, ".java").toFile();
            // generate content of patchedFile by applying patches
            List<String> patchedLines = DiffUtils.patch(buggyLines, patches);
            // write to patchedFile
            Files.write(Paths.get(patchedFile.getPath()), patchedLines);
            return 0; // computeOverfittingScore(buggyFile, patchedFile);
        } catch (PatchFailedException | IOException e) {
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
