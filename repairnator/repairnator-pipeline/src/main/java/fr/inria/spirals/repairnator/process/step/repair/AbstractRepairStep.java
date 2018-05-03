package fr.inria.spirals.repairnator.process.step.repair;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.process.step.AbstractStep;

public abstract class AbstractRepairStep extends AbstractStep {

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
            this.getInspector().getJobStatus().putStatusStep(this, StepStatus.buildSkipped("Not configured to run."));
            super.executeNextStep();
        }
    }

    public abstract String getRepairToolName();
}
