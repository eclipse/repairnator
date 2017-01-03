package fr.inria.spirals.repairnator.step;

import fr.inria.spirals.repairnator.ProjectInspector;
import fr.inria.spirals.repairnator.ProjectState;

/**
 * Created by urli on 03/01/2017.
 */
public abstract class AbstractStep {

    protected ProjectInspector inspector;
    protected ProjectState state;
    private AbstractStep nextStep;

    public AbstractStep(ProjectInspector inspector) {
        this.inspector = inspector;
    }

    public AbstractStep setNextStep(AbstractStep nextStep) {
        this.nextStep = nextStep;
        return nextStep;
    }

    public ProjectState getState() {
        return state;
    }

    protected void executeNextStep() {
        if (this.nextStep != null) {
            this.nextStep.execute();
        }
    }

    public abstract void execute();
}
