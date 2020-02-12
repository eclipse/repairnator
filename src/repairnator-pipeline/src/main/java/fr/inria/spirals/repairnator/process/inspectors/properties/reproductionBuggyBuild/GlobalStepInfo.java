package fr.inria.spirals.repairnator.process.inspectors.properties.reproductionBuggyBuild;

import java.util.ArrayList;
import java.util.List;

public class GlobalStepInfo {

    private int nbSteps;
    private int totalDuration;
    private List<String> stepNames;
    private List<Integer> stepDurations;

    GlobalStepInfo() {
        this.nbSteps = 0;
        this.totalDuration = 0;
        this.stepNames = new ArrayList<>();
        this.stepDurations = new ArrayList<>();
    }

    public int getNbSteps() {
        return nbSteps;
    }

    public int getTotalDuration() {
        return totalDuration;
    }

    public List<String> getStepNames() {
        return stepNames;
    }

    public List<Integer> getStepDurations() {
        return stepDurations;
    }

    public void addStep(String stepName, int stepDuration) {
        if (!this.stepNames.contains(stepName)) {
            this.nbSteps++;
            this.totalDuration += stepDuration;
            this.stepNames.add(this.stepNames.size(), stepName);
            this.stepDurations.add(this.stepDurations.size(), stepDuration);
        }
    }
}
