package fr.inria.spirals.repairnator.process.step.paths;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.states.PipelineState;

import java.io.File;

/**
 * Created by urli on 08/02/2017.
 */
public class ComputeTestDir extends ComputeDir {

    public ComputeTestDir(ProjectInspector inspector, boolean blockingStep) {
        super(inspector, blockingStep);
    }

    private void computeMetricsOnTestDirs(File[] dirs) {
        int numberTestFiles = super.computeMetricsOnDirs(dirs);
        this.getInspector().getJobStatus().getProperties().getProjectMetrics().setNumberTestFiles(numberTestFiles);
    }

    @Override
    protected StepStatus businessExecute() {
        this.getLogger().debug("Computing the test directory ...");

        super.setComputeDirType(ComputeDirType.COMPUTE_TEST_DIR);
        super.setRootDirPath(this.getInspector().getRepoLocalPath());

        StepStatus superStepStatus = super.businessExecute();

        if (superStepStatus.isSuccess()) {
            File[] testDirs = super.getResultDirs();

            this.computeMetricsOnTestDirs(testDirs);

            this.getInspector().getJobStatus().setTestDir(testDirs);
            return StepStatus.buildSuccess(this);
        } else {
            return StepStatus.buildError(this, PipelineState.TESTDIRNOTCOMPUTED);
        }
    }

}
