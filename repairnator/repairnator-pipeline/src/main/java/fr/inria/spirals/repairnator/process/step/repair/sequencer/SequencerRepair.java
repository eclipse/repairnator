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

import java.io.*;
import java.lang.Runtime;
import java.lang.Process;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class SequencerRepair extends AbstractRepairStep {
    protected static final String TOOL_NAME = "SequencerRepair";
    private static final int TOTAL_TIME = 120; // 120 minutes

    private File patchDir;
    private List<RepairPatch> repairPatches;

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
        this.patchDir = new File(this.getInspector().getRepoLocalPath()+"/"+"repairnator." + this.getRepairToolName().toLowerCase() + ".results");
        this.patchDir.mkdirs();

        // construct ZmEngine
        AstorMain astorMain = new AstorMain();
        CommandSummary cs = new CommandSummary();
        cs.command.put("-customengine", ZmEngine.class.getCanonicalName());
        cs.command.put("-parameters", "disablelog:false:logtestexecution:true");
        try {
            astorMain.execute(cs.flat());
        } catch (Exception e) {
            e.printStackTrace();
        }
        ZmEngine zmengine = (ZmEngine) astorMain.getEngine();
        List<SuspiciousModificationPoint> susp = zmengine.getSuspicious();

        /// run Sequencer
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<List<SequencerResult>> sequencerExecution = executor.submit(() -> {

            try {
                List<SequencerResult> sequencerResults = new ArrayList<>();
                sequencerResults.add(new SequencerResult(null, null));

                for (SuspiciousModificationPoint smp : susp) {
                    try {
                        File suspiciousFile = smp.getCodeElement().getPosition().getFile();
                        String buggyFilePath = suspiciousFile.getAbsolutePath();
                        int buggyLineNumber = new SuspiciousFile(smp).getSuspiciousLineNumber();
                        int beamSize = 50; // Sequencer paper http://arxiv.org/pdf/1901.01808
                        String outputDirPath = patchDir.getAbsolutePath();

                        final String command = "docker run sequencer "
                            + "bash ./src/sequencer-predict.sh "
                            + "--buggy_file=" + buggyFilePath
                            + "--buggy_line=" + buggyLineNumber
                            + "--beam_size=" + beamSize
                            + "--output=" + outputDirPath;
                        System.out.println("Command:\n" + command);

                        Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});
                        process.waitFor();
//                        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
//                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
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
            diag.addProperty("className", result.getTestClass());
            diag.addProperty("methodName", result.getTestMethod());
            diag.addProperty("exceptionMessage", result.getExceptionMessage());
            toolDiagnostic.add(diag);

            if (result.isSuccess()) {
                success = true;
                RepairPatch patch = new RepairPatch(this.getRepairToolName(), result.getTestClass(), result.getDiff());
                listPatches.add(patch);
            }
        }

        /// record results
        File nopolLog = new File(System.getProperty("user.dir"), "debug.log");
        if (nopolLog.exists()) {
            String nopolDestName = "repairnator.nopol.log";
            File nopolDest = new File(this.getInspector().getRepoLocalPath(), nopolDestName);
            try {
                Files.move(nopolLog.toPath(), nopolDest.toPath());
                this.getInspector().getJobStatus().addFileToPush(nopolDestName);
            } catch (IOException e) {
                getLogger().error("Error while renaming nopol log", e);
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
