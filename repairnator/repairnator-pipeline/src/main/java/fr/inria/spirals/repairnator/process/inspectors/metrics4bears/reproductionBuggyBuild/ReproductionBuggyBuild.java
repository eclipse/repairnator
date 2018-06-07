package fr.inria.spirals.repairnator.process.inspectors.metrics4bears.reproductionBuggyBuild;

import fr.inria.spirals.repairnator.process.step.AbstractStep;

import java.util.*;

public class ReproductionBuggyBuild {

    private Date reproductionDateBeginning;
    private Date reproductionDateEnd;
    private int totalDuration;
    private ProcessDurations processDurations;

    public ReproductionBuggyBuild() {
        this.totalDuration = 0;
        this.processDurations = new ProcessDurations();
    }

    public Date getReproductionDateBeginning() {
        return reproductionDateBeginning;
    }

    public Date getReproductionDateEnd() {
        return reproductionDateEnd;
    }

    public int getTotalDuration() {
        return totalDuration;
    }

    public ProcessDurations getProcessDurations() {
        return processDurations;
    }

    public void addStep(AbstractStep step) {
        if (this.reproductionDateBeginning == null) { // so this is the first step
            this.reproductionDateBeginning = step.getDateBegin();
        }
        this.reproductionDateEnd = step.getDateEnd(); // so this will always save the end of the last step executed
        this.totalDuration += step.getDuration();

        this.processDurations.addGlobalStepInfo(step);
    }
}
