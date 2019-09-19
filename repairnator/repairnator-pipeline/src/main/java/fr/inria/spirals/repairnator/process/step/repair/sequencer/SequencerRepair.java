package fr.inria.spirals.repairnator.process.step.repair.sequencer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.inria.astor.approaches._3sfix.SuspiciousFile;
import fr.inria.astor.approaches._3sfix.ZmEngine;
import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import fr.inria.main.CommandSummary;
import fr.inria.main.evolution.AstorMain;
import fr.inria.spirals.repairnator.process.files.FileHelper;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.step.repair.AbstractRepairStep;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.lang.Runtime;
import java.lang.Process;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class SequencerRepair extends AbstractRepairStep {
    protected static final String TOOL_NAME = "SequencerRepair";
    private static final int TOTAL_TIME = 120; // 120 minutes

    private File patchDir;

    @Override
    public String getRepairToolName() {
        return TOOL_NAME;
    }

    @Override
    protected StepStatus businessExecute() {
        this.getLogger().info("Start SequencerRepair");
        String pathPrefix = ""; // for macOS: "/private";
        String imageTag = "ycaxgjd/sequencer:1.0";
        // initJobStatus
        JobStatus jobStatus = this.getInspector().getJobStatus();
        // initPatchDir
        this.patchDir = new File(pathPrefix + this.getInspector().getRepoLocalPath()+"/repairnator." + this.getRepairToolName().toLowerCase() + ".results");
        this.patchDir.mkdirs();

        // check ...
        List<URL> classPath = this.getInspector().getJobStatus().getRepairClassPath();
        File[] sources = this.getInspector().getJobStatus().getRepairSourceDir();
        if (classPath == null || sources == null) {
            return StepStatus.buildSkipped(this,"Classpath or Sources not computed.");
        }
        // prepare CommandSummary
        CommandSummary cs = new CommandSummary();
        List<String> dependencies = new ArrayList<>();
        for (URL url : jobStatus.getRepairClassPath()) {
            if (url.getFile().endsWith(".jar")) {
                dependencies.add(url.getPath());
            }
        }
//        cs.command.put("-loglevel", "DEBUG");
        cs.command.put("-mode", "custom");
        cs.command.put("-dependencies", StringUtils.join(dependencies,":"));
        cs.command.put("-location", jobStatus.getFailingModulePath());
//        cs.command.put("-ingredientstrategy", "fr.inria.astor.test.repair.evaluation.extensionpoints.ingredients.MaxLcsSimSearchStrategy");
        cs.command.put("-flthreshold", "0.5");
        cs.command.put("-maxgen", "0");
//        cs.command.put("-population", "1");
//        cs.command.put("-seed", "1");
        cs.command.put("-javacompliancelevel", "8");
        cs.command.put("-customengine", ZmEngine.class.getCanonicalName());
        cs.command.put("-parameters", "disablelog:false:logtestexecution:true:logfilepath:"+ pathPrefix + this.getInspector().getRepoLocalPath()+"/repairnator." + this.getRepairToolName().toLowerCase() + ".log");

        // construct AstorMain
        AstorMain astorMain = new AstorMain();
        try {
            astorMain.execute(cs.flat());
        } catch (Exception e) {
            e.printStackTrace();
        }
        // construct ZmEngine
        ZmEngine zmengine = (ZmEngine) astorMain.getEngine();
        List<SuspiciousModificationPoint> susp = zmengine.getSuspicious();

        /// run Sequencer
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<List<SequencerResult>> sequencerExecution = executor.submit(() -> {

            try {
                List<SequencerResult> sequencerResults = new ArrayList<>();
                int smpId = 0;
                for (SuspiciousModificationPoint smp : susp) {
                    try {
                        File suspiciousFile = smp.getCodeElement().getPosition().getFile();
                        String buggyFilePath = suspiciousFile.getAbsolutePath();
                        int buggyLineNumber = new SuspiciousFile(smp).getSuspiciousLineNumber();
                        int beamSize = 50; // Sequencer paper http://arxiv.org/pdf/1901.01808
                        String buggyFileName = suspiciousFile.getName();
                        String outputDirPath = patchDir.getAbsolutePath() + File.separator + buggyFileName + smpId++;
                        File outputDir = new File(outputDirPath);
                        if (!outputDir.exists() || !outputDir.isDirectory()) {
                            outputDir.mkdirs();
                        }

                        // make sure that "privileged: true" in running container
                        StringJoiner commandStringJoiner = new StringJoiner(";");
                        commandStringJoiner
                            .add("if [ -z `docker images " + imageTag + " -q` ]")
                            .add("then docker pull " + imageTag)
                            .add("fi");
                        commandStringJoiner.add("docker run "
//                            + "-v " + pathPrefix + "/sys:" + pathPrefix + "/sys "
//                            + "-v " + pathPrefix + "/usr/bin/docker:" + pathPrefix + "/usr/bin/folders "
                            + "-v " + pathPrefix + "/var/folders:" + pathPrefix + "/var/folders "
                            + imageTag + " "
                            + "bash ./src/sequencer-predict.sh "
                            + "--buggy_file=" + buggyFilePath + " "
                            + "--buggy_line=" + buggyLineNumber + " "
                            + "--beam_size=" + beamSize + " "
                            + "--output=" + outputDirPath);

                        String commandStr = commandStringJoiner.toString();
                        Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", commandStr});
                        BufferedReader outputBufferReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        StringJoiner outputStringJoiner = new StringJoiner("\n");
                        outputBufferReader.lines().forEach(outputStringJoiner::add);
                        String outputStr = outputStringJoiner.toString();
                        System.out.println(">>> outputStr: \n" + outputStr);
                        BufferedReader errorBufferReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                        StringJoiner errorStringJoiner = new StringJoiner("\n");
                        errorBufferReader.lines().forEach(errorStringJoiner::add);
                        String errorStr = errorStringJoiner.toString();
                        System.err.println(">>> errorStr: \n" + errorStr);
                        process.waitFor();
                        sequencerResults.add(new SequencerResult(buggyFilePath, outputDirPath, outputStr, errorStr));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                return sequencerResults;
            } catch (Throwable throwable) {
                addStepError("Got exception when running SequencerRepair: ", throwable);
                return new ArrayList<>();
            }
        });

        List<SequencerResult> sequencerResults = new ArrayList<>();
        try {
            executor.shutdown();
            sequencerResults.addAll(sequencerExecution.get(TOTAL_TIME, TimeUnit.MINUTES));
        } catch (Exception e) {
            addStepError("Error while executing AssertFixer", e);
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

        this.recordPatches(listPatches);
        this.recordToolDiagnostic(toolDiagnostic);

        try {
            FileHelper.deleteFile(patchDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (success) {
            jobStatus.setHasBeenPatched(true);
            return StepStatus.buildSuccess(this);
        } else {
            return StepStatus.buildSkipped(this, "No patch found");
        }
    }
}
