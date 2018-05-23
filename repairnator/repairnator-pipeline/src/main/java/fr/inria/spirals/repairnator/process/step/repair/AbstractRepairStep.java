package fr.inria.spirals.repairnator.process.step.repair;

import com.google.gson.JsonElement;
import fr.inria.spirals.repairnator.notifier.PatchNotifier;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.process.step.AbstractStep;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public abstract class AbstractRepairStep extends AbstractStep {

    public static final String DEFAULT_DIR_PATCHES = "repairnator-patches";

    public AbstractRepairStep() {
        super(null, false);
    }

    public void setProjectInspector(ProjectInspector inspector) {
        super.setProjectInspector(inspector);
        this.setName(this.getRepairToolName());
    }

    @Override
    public void execute() {
        if (this.getConfig().getRepairTools().contains(this.getRepairToolName())) {
            super.execute();
        } else {
            this.getLogger().warn("Skipping repair step "+this.getRepairToolName());
            this.getInspector().getJobStatus().addStepStatus(StepStatus.buildSkipped(this,"Not configured to run."));
            super.executeNextStep();
        }
    }

    private void serializePatches(List<RepairPatch> patchList) throws IOException {
        File parentDirectory = new File(this.getInspector().getRepoToPushLocalPath(), DEFAULT_DIR_PATCHES);

        if (!parentDirectory.exists()) {
            parentDirectory.mkdirs();
        }

        File toolDirectory = new File(parentDirectory, this.getRepairToolName());
        toolDirectory.mkdirs();

        int i = 0;
        for (RepairPatch repairPatch : patchList) {
            File patchFile = new File(toolDirectory, "patch_" + i + ".patch");
            BufferedWriter bufferedWriter = Files.newBufferedWriter(patchFile.toPath());
            bufferedWriter.write(repairPatch.getDiff());
            bufferedWriter.close();
            this.getInspector().getJobStatus().addFileToPush(patchFile.getAbsolutePath());
        }
    }

    private void notify(List<RepairPatch> patches) {
        PatchNotifier patchNotifier = this.getInspector().getPatchNotifier();
        if (patchNotifier != null) {
            patchNotifier.notify(this.getInspector(), this.getRepairToolName(), patches);
        }
    }

    protected void recordPatches(List<RepairPatch> patchList) {
        this.getInspector().getJobStatus().addPatches(this.getRepairToolName(), patchList);

        if (!patchList.isEmpty()) {
            this.getInspector().getJobStatus().setHasBeenPatched(true);
            try {
                this.serializePatches(patchList);
            } catch (IOException e) {
                this.addStepError("Error while serializing patches", e);
            }
            this.notify(patchList);
        }
    }

    protected void recordToolDiagnostic(JsonElement element) {
        this.getInspector().getJobStatus().addToolDiagnostic(this.getRepairToolName(), element);
    }

    public abstract String getRepairToolName();
}
