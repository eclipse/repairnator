package fr.inria.spirals.repairnator.process.step.repair;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.AbstractStep;

public abstract class AbstractRepairStep extends AbstractStep {

    public AbstractRepairStep() {
        super(null);
    }

    public void setProjectInspector(ProjectInspector inspector) {
        this.inspector = inspector;
        this.setName(this.getRepairToolName());
        this.initStates();
    }

    @Override
    public void execute() {
        if (this.getConfig().getRepairTools().contains(this.getRepairToolName())) {
            super.execute();
        } else {
            this.getLogger().warn("Skipping repair step "+this.getRepairToolName());
            super.executeNextStep();
        }
    }

    public abstract String getRepairToolName();
}
