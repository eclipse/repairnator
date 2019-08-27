package fr.inria.spirals.repairnator.process.step.repair.sequencer;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SequencerResult {
    private String buggyFilePath;
    private String outputDirPath;
    private String message;
    private String warning;
    private boolean success;
    private List<String> diffs;

    public SequencerResult(String buggyFilePath, String outputDirPath, String message, String warning) {
        this.buggyFilePath = buggyFilePath;
        this.outputDirPath = outputDirPath;
        this.message = message;
        this.warning = warning;
        File outputDir = new File(outputDirPath);
        if (outputDir.exists() && outputDir.isDirectory()) {
            List<File> patchDirs = Arrays.asList(outputDir.listFiles());
            success = patchDirs.size() > 0;
            diffs = new ArrayList<>();
            if (success) {
                for (File patchDir : patchDirs) {
                    for (File patchFile : patchDir.listFiles()) {
                        try {
                            List<String> diffList = Files.readLines(patchFile, Charsets.UTF_8);
                            String diff = diffList.stream().collect(Collectors.joining());
                            diffs.add(diff);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
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

    public String getWarning() {
        return this.warning;
    }

    public boolean isSuccess() {
        return this.success;
    }

    public List<String> getDiffs() {
        return this.diffs;
    }
}
