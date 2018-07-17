package fr.inria.spirals.repairnator.process.step.paths;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.states.PipelineState;
import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * Created by urli on 08/02/2017.
 */
public class ComputeTestDir extends ComputeDir {

    public ComputeTestDir(ProjectInspector inspector, boolean blockingStep) {
        super(inspector, blockingStep);
    }

    private void computeMetricsOnTest(File[] sources) {
        int totalAppFiles = 0;
        if (sources != null && sources.length > 0) {
            for (File f : sources) {
                int nbFile = FileUtils.listFiles(f, new String[] {"java"}, true).size();
                totalAppFiles += nbFile;
            }
            this.getInspector().getJobStatus().getMetrics().setNbFileTests(totalAppFiles);
            this.getInspector().getJobStatus().getMetrics4Bears().getProjectMetrics().setNumberTestFiles(totalAppFiles);
        }
    }

    @Override
    protected StepStatus businessExecute() {
        this.getLogger().debug("Computing the test directory ...");

        super.setComputeDirType(ComputeDirType.COMPUTE_TEST_DIR);
        super.setDirPath(this.getInspector().getRepoLocalPath());

        StepStatus superStepStatus = super.businessExecute();

        if (superStepStatus.isSuccess()) {
            File[] sources = super.getResultDirs();

            this.computeMetricsOnTest(sources);

            this.getInspector().getJobStatus().setTestDir(sources);
            return StepStatus.buildSuccess(this);
        } else {
            return StepStatus.buildError(this, PipelineState.TESTDIRNOTCOMPUTED);
        }
    }

}
