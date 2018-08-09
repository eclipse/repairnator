package fr.inria.spirals.repairnator.process.step.paths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.states.PipelineState;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by urli on 08/02/2017.
 */
public class ComputeSourceDir extends ComputeDir {

    private static final String COMPUTE_TOTAL_CLOC = "cloc --json --vcs=git .";

    private boolean allModules;

    public ComputeSourceDir(ProjectInspector inspector, boolean blockingStep, boolean allModules) {
        super(inspector, blockingStep);
        this.allModules = allModules;
    }

    private void computeMetricsOnSourceDirs(File[] dirs) {
        int numberSourceFiles = super.computeMetricsOnDirs(dirs);
        this.getInspector().getJobStatus().getProperties().getProjectMetrics().setNumberSourceFiles(numberSourceFiles);
    }

    private void computeMetricsOnCompleteRepo() {
        this.getLogger().debug("Compute the line of code of the project");
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh","-c",COMPUTE_TOTAL_CLOC)
                .directory(new File(this.getInspector().getRepoLocalPath()));

        try {
            Process p = processBuilder.start();
            BufferedReader stdin = new BufferedReader(new InputStreamReader(p.getInputStream()));
            p.waitFor();

            this.getLogger().debug("Get result from cloc process...");
            String processReturn = "";
            String line;
            while (stdin.ready() && (line = stdin.readLine()) != null) {
                processReturn += line;
            }

            Gson gson = new GsonBuilder().create();
            JsonObject json = gson.fromJson(processReturn, JsonObject.class);

            int numberLines = 0;
            if (json != null && json.getAsJsonObject("Java") != null) {
                JsonObject java = json.getAsJsonObject("Java");
                if (java.getAsJsonPrimitive("code") != null) {
                    numberLines = java.getAsJsonPrimitive("code").getAsInt();
                }
            }

            this.getInspector().getJobStatus().getProperties().getProjectMetrics().setNumberLines(numberLines);
        } catch (IOException | InterruptedException e) {
            this.getLogger().error("Error while computing metrics on source code of the whole repo.", e);
        }
    }

    @Override
    protected StepStatus businessExecute() {
        this.getLogger().debug("Computing the source directory ...");

        String dirPath = (this.allModules) ?
                this.getInspector().getRepoLocalPath() : this.getInspector().getJobStatus().getFailingModulePath();

        super.setComputeDirType(ComputeDirType.COMPUTE_SOURCE_DIR);
        super.setRootDirPath(dirPath);
        super.setAllModules(this.allModules);

        StepStatus superStepStatus = super.businessExecute();

        if (superStepStatus.isSuccess()) {
            File[] sourceDirs = super.getResultDirs();

            if (allModules) {
                this.computeMetricsOnSourceDirs(sourceDirs);
                this.computeMetricsOnCompleteRepo();
            }

            this.getInspector().getJobStatus().setRepairSourceDir(sourceDirs);
            return StepStatus.buildSuccess(this);
        } else {
            this.getInspector().getJobStatus().setRepairSourceDir(null);
            return StepStatus.buildError(this, PipelineState.SOURCEDIRNOTCOMPUTED);
        }
    }

}
