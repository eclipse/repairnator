package fr.inria.spirals.repairnator.process.inspectors.properties.reproductionBuggyBuild;

import fr.inria.spirals.repairnator.process.inspectors.properties.machineInfo.MachineInfo;
import fr.inria.spirals.repairnator.process.step.AbstractStep;

import java.util.*;

public class ReproductionBuggyBuild {

    private Date reproductionDateBeginning;
    private Date reproductionDateEnd;
    private MachineInfo machineInfo;
    private int totalDuration;
    private ProcessDurations processDurations;
    private String projectRootPomPath;

    public ReproductionBuggyBuild() {
        this.machineInfo = new MachineInfo();
        this.totalDuration = 0;
        this.processDurations = new ProcessDurations();
    }

    public Date getReproductionDateBeginning() {
        return reproductionDateBeginning;
    }

    public Date getReproductionDateEnd() {
        return reproductionDateEnd;
    }

    public MachineInfo getMachineInfo() {
        return machineInfo;
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

    public String getProjectRootPomPath() {
        return projectRootPomPath;
    }

    public void setProjectRootPomPath(String projectRootPomPath) {
        this.projectRootPomPath = projectRootPomPath;
    }
}
