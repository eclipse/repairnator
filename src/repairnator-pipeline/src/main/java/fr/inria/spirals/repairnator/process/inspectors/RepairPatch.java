package fr.inria.spirals.repairnator.process.inspectors;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import fr.inria.coming.codefeatures.RepairnatorFeatures;
import fr.inria.coming.codefeatures.RepairnatorFeatures.ODSLabel;
import fr.inria.spirals.repairnator.config.SequencerConfig;
import fr.inria.spirals.repairnator.process.inspectors.properties.features.Features;
import fr.inria.spirals.repairnator.process.inspectors.properties.features.Overfitting;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

public class RepairPatch {

	static final String ODSPath = SequencerConfig.getInstance().ODSPath;
	protected static Logger log = Logger.getLogger(Thread.currentThread().getName());

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

	/**
	 * correctness label predicted by ODS
	 */
	private ODSLabel odsLabel;

	public RepairPatch(String toolname, String filePath, String diff) {
		this.toolname = toolname;
		this.filePath = filePath;
		this.diff = diff;
		this.overfittingScores = new HashMap<>();
		this.odsLabel = ODSLabel.UNKNOWN;
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
		if (overfittingScores.containsKey(features)) {
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
	
	public void setODSLabel(ODSLabel label) {
		this.odsLabel  = label;
	}
	
	public ODSLabel getODSLabel() {
		return odsLabel;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		final RepairPatch that = (RepairPatch) o;
		return Objects.equals(toolname, that.toolname) && Objects.equals(filePath, that.filePath)
				&& Objects.equals(diff, that.diff);
	}

	@Override
	public int hashCode() {

		return Objects.hash(toolname, filePath, diff);
	}

	// ranking algorithms
	public static Comparator<RepairPatch> rankByOverfittingWithFeatures(Features features) {
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

	// ODS classification
	public static List<RepairPatch> classifyByODSWithFeatures(List<RepairPatch> allPatches, Long buildId) {

		File f = new File(ODSPath);
		f.mkdir();

		int len = allPatches.size();

		for (int patchID = 0; patchID < len; patchID++) {
			RepairPatch repairPatch = allPatches.get(patchID);
			ODSLabel label = repairPatch.computeODSLabel(patchID, buildId);
			repairPatch.setODSLabel(label);
		}

		String ODSSummary = ODSPath + "/" + buildId + "_summary";

		try {
			FileWriter writer = new FileWriter(ODSSummary);
			for (int i = 0; i < len; i++) {
				writer.write(String.format("%s-%s %s\n", buildId, i, allPatches.get(i).getODSLabel()));
			}
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException("RepairPatch error: Unable to write to ODS summary file:" + e);
		}

		return allPatches;
	}

	private ODSLabel computeODSLabel(int patchId, Long buildId) {
		
		ODSLabel label = ODSLabel.UNKNOWN;

		try {		
			File buggyFile = new File(filePath);
			// if no buggy file available, we provide the unknown label for the patches.
			if (!buggyFile.isFile()) {
				log.error("The buggy file path not exists: "+ filePath);
				return label;
			}
	
			// read from buggyFile
			List<String> buggyLines = new ArrayList<>();
			try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
				stream.forEach(buggyLines::add);
			} catch (IOException e) {
				e.printStackTrace();
				return label;
			}
	
			// prepare patches
			List<String> diffLines = Arrays.asList(diff.split("\n"));
			Patch<String> patches = UnifiedDiffUtils.parseUnifiedDiff(diffLines);
	
			// create a directory to store the patch: "patches/"+buildId+patchId
			String buggyClassName = buggyFile.getName().replace(".java", "");
			String odsFilesPath = System.getProperty("user.home") + "/ODSPatches";
	
			String patchPath = odsFilesPath + "/" + buildId + "-" + patchId;
			Path path = Paths.get(patchPath + '/' + buggyClassName);
			Files.createDirectories(path);
	
			// create buggy file and patchedFile that follows Coming structure
			File newBuggyFile = new File(path + "/" + buildId + "-" + patchId + "_" + buggyClassName + "_s.java");
			File patchedFile = new File(path + "/" + buildId + "-" + patchId + "_" + buggyClassName + "_t.java");
	
			// copy the buggy file under the patch folder
			Files.write(Paths.get(newBuggyFile.getPath()), buggyLines);
			// generate content of patchedFile by applying patches
			List<String> patchedLines = DiffUtils.patch(buggyLines, patches);
			Files.write(Paths.get(patchedFile.getPath()), patchedLines);
	
			log.info("The patchPath file passed to ODS: "+patchPath);
	
			 label = new RepairnatorFeatures().getLabel(new File(patchPath));						

		} catch (Exception e) {
			log.error("Exception caused in the method of computeODSLabel: "+e);
			return label;
		}

		return label;

	}

	
}
