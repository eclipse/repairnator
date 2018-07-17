package fr.inria.spirals.repairnator.process.step.paths;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.process.step.AbstractStep;

public class ComputeDir extends AbstractStep {

    private ComputeDirType computeDirType;

    public ComputeDir(ProjectInspector inspector, boolean blockingStep) {
        super(inspector, blockingStep);
    }

    @Override
    protected StepStatus businessExecute() {
        return null;
    }

    public void setComputeDirType(ComputeDirType computeDirType) {
        this.computeDirType = computeDirType;
    }
}
