package fr.inria.spirals.repairnator.process.inspectors;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import fr.inria.spirals.repairnator.process.inspectors.properties.features.Features;
import fr.inria.spirals.repairnator.process.inspectors.properties.features.Overfitting;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
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
    private Map<Features, Double> overfittingScores;

    public RepairPatch(String toolname, String filePath, String diff) {
        this.toolname = toolname;
        this.filePath = filePath;
        this.diff = diff;
        this.overfittingScores = new HashMap<>();
    }

    private Double computeOverfittingScores(Features feature) {
        File buggyFile = new File(filePath);
        double score = Double.POSITIVE_INFINITY;
        if (!buggyFile.isFile()) {
            return score;
        }

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
            String tmpName = buggyFile.getName();
            File patchedFile = Files.createTempFile(tmpName, ".java").toFile();
            // generate content of patchedFile by applying patches
            List<String> patchedLines = DiffUtils.patch(buggyLines, patches);
            // write to patchedFile
            Files.write(Paths.get(patchedFile.getPath()), patchedLines);

            Overfitting overfitting = new Overfitting(feature);
            score = overfitting.computeScore(buggyFile, patchedFile);

        } catch (PatchFailedException | IOException e) {
            throw new RuntimeException(e);
        }

        return score;
    }

    public Double getOverfittingScore(Features features) {
        if(overfittingScores.containsKey(features)){
            return overfittingScores.get(features);
        }

        double score = computeOverfittingScores(features);
        overfittingScores.put(features, score);

        return score;
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

    //ranking algorithms
    public static Comparator<RepairPatch> rankByOverfittingWithFeatures(Features features){
        return (x, y) -> overfittingSort(x, y, features);
    }

    private static int overfittingSort(RepairPatch patch1, RepairPatch patch2, Features features) { // ascending
        double score1 = patch1.getOverfittingScore(features);
        double score2 = patch2.getOverfittingScore(features);
        double diff = score1 - score2;
        if (diff < 0) {
            return -1;
        } else if (diff > 0) {
            return 1;
        }
        return 0;
    }
}
