package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;

import java.util.HashMap;
import java.util.List;

public class BranchingStep extends AbstractStep {

    HashMap<StepStatus.StatusKind, AbstractStep> branches;
    AbstractStep defaultNextStep;
    AbstractStep branchAfterStep;

    public BranchingStep(ProjectInspector inspector, AbstractStep defaultNextStep) {
        super(inspector, false);

        this.defaultNextStep = defaultNextStep;
        branches = new HashMap<>();
    }

    public void createBranch(StepStatus.StatusKind stepStatus, AbstractStep step){
        branches.put(stepStatus, step);
    }

    public void branchAfter(AbstractStep step){
        branchAfterStep = step;
    }

    @Override
    protected StepStatus businessExecute() {
        resolve();
        return StepStatus.buildSuccess(this);
    }

    void resolve(){
        StepStatus.StatusKind lastStepStatus = branchAfterStep.getStepStatus().getStatus();

        AbstractStep nextStep = branches.getOrDefault(lastStepStatus, defaultNextStep);
        this.setNextStep(nextStep);
    }


}
