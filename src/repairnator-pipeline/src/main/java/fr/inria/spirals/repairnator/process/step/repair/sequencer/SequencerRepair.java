package fr.inria.spirals.repairnator.process.step.repair.sequencer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.Image;
import fr.inria.spirals.repairnator.docker.DockerHelper;
import fr.inria.spirals.repairnator.config.SequencerConfig;
import fr.inria.spirals.repairnator.process.step.repair.sequencer.detection.ModificationPoint;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.step.repair.AbstractRepairStep;
import fr.inria.spirals.repairnator.process.step.repair.sequencer.detection.AstorDetectionStrategy;
import fr.inria.spirals.repairnator.process.step.repair.sequencer.detection.DetectionStrategy;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

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
 * @author Javier Ron
*/
public class SequencerRepair extends AbstractRepairStep {
    protected static final String TOOL_NAME = "SequencerRepair";
    private final SequencerConfig config;
    private final DockerClient docker;

    public SequencerRepair(){
        config = SequencerConfig.getInstance();
        docker = DockerHelper.initDockerClient();
    }

    @Override
    public String getRepairToolName() {
        return TOOL_NAME;
    }

    @Override
    protected StepStatus businessExecute() {
        this.getLogger().info("Start SequencerRepair");

        String pathPrefix = ""; // for macOS: "/private";
        JobStatus jobStatus = this.getInspector().getJobStatus();
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

        /// pull Sequencer if image not present

        try {
            List<Image> images = docker.listImages(DockerClient.ListImagesParam.byName(config.dockerTag));
            if(images.size() <= 0) docker.pull(config.dockerTag);
        } catch (Exception e) {
            return StepStatus.buildSkipped(this,"Error while retrieving sequencer docker image");
        }

        final ExecutorService executor = Executors.newFixedThreadPool(config.threads);
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
                int beamSize = config.beam_size;
                String buggyFileName = suspiciousFile.getFileName().toString();
                Path outputDirPath = patchDir.toAbsolutePath().resolve(buggyFileName + smpId);
                if ( !Files.exists(outputDirPath) || !Files.isDirectory(outputDirPath)) {
                    Files.createDirectory(outputDirPath);
                }

                String sequencerCommand = "./sequencer-predict.sh "
                                            + "--buggy_file=" + "/tmp/" + buggyFileName + " "
                                            + "--buggy_line=" + buggyLineNumber + " "
                                            + "--beam_size=" + beamSize + " "
                                            + "--real_file_path=" + relativePath + " "
                                            + "--output=" + "/out";

                HostConfig hostConfig = HostConfig.builder()
                        .appendBinds(HostConfig.Bind
                                .from(buggyParentPath.toString())
                                .to("/tmp")
                                .build())
                        .appendBinds(HostConfig.Bind
                                .from(outputDirPath.toString())
                                .to("/out")
                                .build())
                        .build();

                ContainerConfig containerConfig = ContainerConfig.builder()
                        .image(config.dockerTag)
                        .hostConfig(hostConfig)
                        .cmd("bash", "-c", sequencerCommand)
                        .attachStdout(true)
                        .attachStderr(true)
                        .build();

                 ContainerCreation container = docker.createContainer(containerConfig);
                 docker.startContainer(container.id());
                 docker.waitContainer(container.id());

                 String stdOut = docker.logs(
                         container.id(),
                         DockerClient.LogsParam.stdout()
                 ).readFully();

                String stdErr = docker.logs(
                        container.id(),
                        DockerClient.LogsParam.stderr()
                ).readFully();

                docker.removeContainer(container.id());

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
            executor.awaitTermination(config.timeout, TimeUnit.MINUTES);
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
}
