package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.Launcher;
import fr.inria.spirals.repairnator.process.ProjectInspector;

import java.util.Date;

/**
 * Created by urli on 03/01/2017.
 */
public abstract class AbstractStep {

    private int limitStepNumber;
    protected ProjectInspector inspector;
    protected ProjectState state;

    protected boolean shouldStop;
    private AbstractStep nextStep;
    private long dateBegin;
    private long dateEnd;

    public AbstractStep(ProjectInspector inspector) {
        this.inspector = inspector;
        this.shouldStop = false;
        this.state = ProjectState.NONE;
    }

    public void setLimitStepNumber(int limitStepNumber) {
        this.limitStepNumber = limitStepNumber;
    }

    public AbstractStep setNextStep(AbstractStep nextStep) {
        this.nextStep = nextStep;
        return nextStep;
    }

    public ProjectState getState() {
        return state;
    }

    public void setState(ProjectState state) {
        this.state = state;
    }

    protected ProjectState executeNextStep() {
        if (this.nextStep != null) {
            this.limitStepNumber--;
            Launcher.LOGGER.debug(this.limitStepNumber+" steps remaining...");

            if (this.limitStepNumber > 0) {
                this.nextStep.setLimitStepNumber(this.limitStepNumber);
                this.nextStep.setState(this.state);
                return this.nextStep.execute();
            }

        }
        return this.state;
    }

    public ProjectState execute() {
        this.dateBegin = new Date().getTime();
        this.businessExecute();
        this.dateEnd = new Date().getTime();
        if (!shouldStop) {
            return this.executeNextStep();
        } else {
            return this.state;
        }
    }

    public int getDuration() {
        if (dateEnd == 0 || dateBegin == 0) {
            return 0;
        }
        return Math.round((dateEnd-dateBegin) / 1000);
    }

    protected abstract void businessExecute();
}
