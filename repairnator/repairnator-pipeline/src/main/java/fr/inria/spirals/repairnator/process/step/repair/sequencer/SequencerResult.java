package fr.inria.spirals.repairnator.process.step.repair.sequencer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.file.Files.readAllBytes;

public class SequencerResult {
    private String buggyFilePath;
    private String outputDirPath;
    private String message;
    private boolean success;
    private List<String> diffs;

    public SequencerResult(String buggyFilePath, String outputDirPath, String message) {
        this.buggyFilePath = buggyFilePath;
        this.outputDirPath = outputDirPath;
        this.message = message;
        File outputDir = new File(outputDirPath);
        if (outputDir.exists() && outputDir.isDirectory()) {
            List<File> patchFiles = Arrays.asList(outputDir.listFiles());
            success = patchFiles.size() > 0;
            diffs = new ArrayList<>();
            if (success) {
                for (File patchFile : patchFiles) {
                    try {
                        Path path = Paths.get(patchFile.getAbsolutePath());
                        String diff = new String(readAllBytes((path)));
                        diffs.add(diff);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
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

    public boolean isSuccess() {
        return this.success;
    }

    public List<String> getDiffs() {
        return this.diffs;
    }
}
