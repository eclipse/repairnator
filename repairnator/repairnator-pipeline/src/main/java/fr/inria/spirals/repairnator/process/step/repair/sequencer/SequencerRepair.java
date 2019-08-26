package fr.inria.spirals.repairnator.process.step.repair.sequencer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import java.nio.file.Files;
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

        // initJobStatus
        JobStatus jobStatus = this.getInspector().getJobStatus();
        // initPatchDir
        this.patchDir = new File("/private" + this.getInspector().getRepoLocalPath()+"/"+"repairnator." + this.getRepairToolName().toLowerCase() + ".results");
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
        cs.command.put("-dependencies", StringUtils.join(dependencies,":"));
        cs.command.put("-mode", "sequencer");
        cs.command.put("-location", jobStatus.getFailingModulePath());
        String relativeSourcePath = new File(jobStatus.getFailingModulePath()).toURI().relativize(jobStatus.getRepairSourceDir()[0].toURI()).getPath();
        cs.command.put("-srcjavafolder", relativeSourcePath);
        cs.command.put("-stopfirst", "true");
        cs.command.put("-population", "1");
        cs.command.put("-maxtime", TOTAL_TIME + "");
        cs.command.put("-seed", "1");
        cs.command.put("-ingredientstrategy", "fr.inria.astor.test.repair.evaluation.extensionpoints.ingredients.MaxLcsSimSearchStrategy"); // is this needed?
        cs.command.put("-customengine", ZmEngine.class.getCanonicalName());
        cs.command.put("-parameters", "disablelog:false:logtestexecution:true:timezone:Europe/Paris:maxnumbersolutions:3:limitbysuspicious:false:maxmodificationpoints:1000:javacompliancelevel:8:logfilepath:"+this.getInspector().getRepoLocalPath()+"/repairnator.sequencer.log");

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

                        final String command = "docker run "
                            + "-v /private/var/folders:/private/var/folders "
                            + "sequencer "
                            + "bash ./src/sequencer-predict.sh "
                            + "--buggy_file=" + buggyFilePath + " "
                            + "--buggy_line=" + buggyLineNumber + " "
                            + "--beam_size=" + beamSize + " "
                            + "--output=" + outputDirPath;
                        System.out.println("Command:\n" + command);

                        Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        StringJoiner stringJoiner = new StringJoiner("\n");
                        bufferedReader.lines().forEach(stringJoiner::add);
                        String message = stringJoiner.toString();
                        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                        StringJoiner errorJoiner = new StringJoiner("\n");
                        errorReader.lines().forEach(errorJoiner::add);
                        String errorInfo = errorJoiner.toString();
                        process.waitFor();
                        System.out.println(">>>>>>>>>>>>>>>>");
                        System.out.println(message);
                        System.out.println("================");
                        System.out.println(errorInfo);
                        System.out.println("<<<<<<<<<<<<<<<<");
                        sequencerResults.add(new SequencerResult(buggyFilePath, outputDirPath, message));
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

        /// record results
        File sequencerLog = new File(System.getProperty("user.dir"), "debug.log");
        if (sequencerLog.exists()) {
            String sequencerDestName = "repairnator.sequencer.log";
            File sequencerDest = new File(this.getInspector().getRepoLocalPath(), sequencerDestName);
            try {
                Files.move(sequencerLog.toPath(), sequencerDest.toPath());
                this.getInspector().getJobStatus().addFileToPush(sequencerDestName);
            } catch (IOException e) {
                getLogger().error("Error while renaming sequencer log", e);
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
