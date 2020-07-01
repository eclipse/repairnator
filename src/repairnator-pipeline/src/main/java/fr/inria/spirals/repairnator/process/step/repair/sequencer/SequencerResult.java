package fr.inria.spirals.repairnator.process.step.repair.sequencer;

import java.nio.file.Files;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
* SequencerResult is SequencerRepair's data class, and is
* mainly used to generate corresponding RepairPatch data.
* SequencerRepair's execution info, namely CLI's output
* message and error message, are also stored for inquiry.
* 
* @author Jian GU
*/
public class SequencerResult {
    private String buggyFilePath;
    private String outputDirPath;
    private String message;
    private String warning;
    private List<String> diffs;

    public SequencerResult(String buggyFilePath, String outputDirPath, String message, String warning)
            throws Exception {

        this.buggyFilePath = buggyFilePath;
        this.outputDirPath = outputDirPath;
        this.message = message;
        this.warning = warning;
        Path outputDir = Paths.get(outputDirPath);

        if (!Files.exists(outputDir) || !Files.isDirectory(outputDir))
            throw new Exception("Error while reading sequencer result files: " +
                    "Output path does not exist or is not a directory");

        List<Path> patchDirs = Files.walk(outputDir)
                .filter(file -> Files.isRegularFile(file) && file.getFileName().toString().equals("diff"))
                .collect(Collectors.toList());

        this.diffs = new ArrayList<>();

        for (Path file : patchDirs){
            List<String> diffLines = Files.readAllLines(file, Charset.forName("UTF-8"));
            String diff = String.join("\n", diffLines) + "\n";
            diffs.add(diff);
        }
    }

    public String getBuggyFilePath() {
        return this.buggyFilePath;
    }

    public String getOutputDirPath() {
        return this.outputDirPath;
    }

    public String getMessage() {
        return this.message;
    }

    public String getWarning() {
        return this.warning;
    }

    public boolean isSuccess() {
        return diffs.size() > 0;
    }

    public List<String> getDiffs() {
        return this.diffs;
    }
}
