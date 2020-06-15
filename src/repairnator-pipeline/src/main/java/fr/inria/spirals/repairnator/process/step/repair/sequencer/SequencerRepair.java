package fr.inria.spirals.repairnator.process.step.repair.sequencer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.inria.astor.approaches._3sfix.SuspiciousFile;
import fr.inria.astor.approaches._3sfix.ZmEngine;
import fr.inria.spirals.repairnator.process.step.repair.sequencer.detection.ModificationPoint;
import fr.inria.main.CommandSummary;
import fr.inria.main.evolution.AstorMain;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.step.repair.AbstractRepairStep;
import fr.inria.spirals.repairnator.process.step.repair.sequencer.detection.AstorDetectionStrategy;
import fr.inria.spirals.repairnator.process.step.repair.sequencer.detection.DetectionStrategy;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.lang.Runtime;
import java.lang.Process;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
* SequencerRepair is one builtin repair tool. It generates
* patches by invoking SequenceR docker image.
* 
* SequencerRepair is dependent on Astor as it collects info
* about suspicious modification locations from the latter
* to feed SequenceR.
* 
* SequenceR is one seq2seq model designed to predict source
* code change on line level. Check its paper for more info:
* https://arxiv.org/abs/1901.01808
* 
* @author Jian GU
*/
public class SequencerRepair extends AbstractRepairStep {
    protected static final String TOOL_NAME = "SequencerRepair";
    private static final int TOTAL_TIME = 120; // 120 minutes //MAKE CONFIGURABLE
    private static final int SEQUENCER_THREADS = 4; // MAKE CONFIGURABLE

    public SequencerRepair(){}

    @Override
    public String getRepairToolName() {
        return TOOL_NAME;
    }

    @Override
    protected StepStatus businessExecute() {
        this.getLogger().info("Start SequencerRepair");
        String pathPrefix = ""; // for macOS: "/private";
        String imageTag = "repairnator/sequencer:2.0"; // MAKE CONFIGURABLE
        // initJobStatus
        JobStatus jobStatus = this.getInspector().getJobStatus();
        // initPatchDir
        Path patchDir = Paths.get(pathPrefix + this.getInspector().getRepoLocalPath() +
                "/repairnator." + TOOL_NAME + ".results");
        try {
            Files.createDirectory(patchDir);
        } catch (IOException e) {
            addStepError("Got exception when running SequencerRepair: ", e);
        }

        // check ...
        List<URL> classPath = this.getInspector().getJobStatus().getRepairClassPath();
        File[] sources = this.getInspector().getJobStatus().getRepairSourceDir();
        if (classPath == null || sources == null) {
            return StepStatus.buildSkipped(this,"Classpath or Sources not computed.");
        }

        DetectionStrategy detectionStrategy = new AstorDetectionStrategy();
        List<ModificationPoint> suspiciousPoints = detectionStrategy.detect(this);

        /// run Sequencer

        StringJoiner pullStringJoiner = new StringJoiner(";");
        pullStringJoiner
                .add("if [ -z `docker images " + imageTag + " -q` ]")
                .add("then docker pull " + imageTag)
                .add("fi");
        String pullCmd = pullStringJoiner.toString();

        try {
            Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", pullCmd});
            process.waitFor();
        } catch (Exception e) {
            addStepError("Got exception when running SequencerRepair: ", e);
        }

        final ExecutorService executor = Executors.newFixedThreadPool(SEQUENCER_THREADS);
        List<Future<SequencerResult>> allResults = new ArrayList<>();

        suspiciousPoints.forEach( smp -> allResults.add(executor.submit(() -> {
            try {
                int smpId = smp.hashCode();
                Path suspiciousFile = smp.getFilePath();
                Path buggyFilePath = suspiciousFile.toAbsolutePath();
                Path buggyParentPath = suspiciousFile.getParent();
                Path repoPath = Paths.get(getInspector().getRepoLocalPath()).toRealPath();
                Path relativePath = repoPath.relativize(suspiciousFile);
                int buggyLineNumber = smp.getSuspiciousLine();
                int beamSize = 50; // Sequencer paper https://arxiv.org/abs/1901.01808
                String buggyFileName = suspiciousFile.getFileName().toString();
                Path outputDirPath = patchDir.toAbsolutePath().resolve(buggyFileName + smpId);
                if ( !Files.exists(outputDirPath) || !Files.isDirectory(outputDirPath)) {
                    Files.createDirectory(outputDirPath);
                }


                // make sure that "privileged: true" in running container
                StringJoiner commandStringJoiner = new StringJoiner(";");

                commandStringJoiner.add("docker run --rm "
                    + "-v " + pathPrefix + buggyParentPath + pathPrefix + ":/tmp" + " "
                    + "-v " + pathPrefix + outputDirPath + pathPrefix + ":/out" + " "
                    + imageTag + " "
                    + "bash ./sequencer-predict.sh "
                    + "--buggy_file=" + "/tmp/" + buggyFileName + " "
                    + "--buggy_line=" + buggyLineNumber + " "
                    + "--beam_size=" + beamSize + " "
                    + "--real_file_path=" + relativePath + " "
                    + "--output=" + "/out");
//                        commandStringJoiner.add("docker stop $(docker ps -aq)");
//                        commandStringJoiner.add("docker rm $(docker ps -aq)");

                String commandStr = commandStringJoiner.toString();
                Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", commandStr});
                process.waitFor();

                String stdOut = HandleOutput(process.getInputStream());
                String stdErr = HandleOutput(process.getErrorStream());

                this.getLogger().debug("stdOut: \n" + stdOut);
                this.getLogger().debug("stdErr: \n" + stdErr);

                return new SequencerResult(buggyFilePath.toString(), outputDirPath.toString(),
                        stdOut, stdErr);
                  //       process.destroy();

            } catch (Throwable throwable) {
                addStepError("Got exception when running SequencerRepair: ", throwable);
                return null;
            }
        })));

        List<SequencerResult> sequencerResults = new ArrayList<>();
        try {
            executor.shutdown();
            executor.awaitTermination(TOTAL_TIME, TimeUnit.MINUTES);
            for (Future<SequencerResult> result : allResults){
                sequencerResults.add(result.get());
            }
        } catch (Exception e) {
            addStepError("Got exception when running SequencerRepair: ", e);
        }

        /// prepare results
        List<RepairPatch> listPatches = new ArrayList<>();
        JsonArray toolDiagnostic = new JsonArray();

        boolean success = false;
        for (SequencerResult result : sequencerResults) {
            JsonObject diag = new JsonObject();

            diag.addProperty("success", result.isSuccess());
            diag.addProperty("message", result.getMessage());
            diag.addProperty("warning", result.getWarning());
            toolDiagnostic.add(diag);

            if (result.isSuccess()) {
                success = true;
                List<String> diffs = result.getDiffs();
                for (String diff : diffs) {
                    RepairPatch patch = new RepairPatch(this.getRepairToolName(), result.getBuggyFilePath(), diff);
                    listPatches.add(patch);
                }
            }
        }

        this.recordPatches(listPatches,MAX_PATCH_PER_TOOL);
        this.recordToolDiagnostic(toolDiagnostic);

        try {
            Files.walk(patchDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            addStepError("Got exception when running SequencerRepair: ", e);
        }

        if (success) {
            jobStatus.setHasBeenPatched(true);
            return StepStatus.buildSuccess(this);
        } else {
            return StepStatus.buildPatchNotFound(this);
        }
    }

    private String HandleOutput(InputStream iStream){
        BufferedReader outputBufferReader = new BufferedReader(new InputStreamReader(iStream));
        StringJoiner outputStringJoiner = new StringJoiner("\n");
        outputBufferReader.lines().forEach(outputStringJoiner::add);
        String outputStr = outputStringJoiner.toString();
        return outputStr;
    }
}
