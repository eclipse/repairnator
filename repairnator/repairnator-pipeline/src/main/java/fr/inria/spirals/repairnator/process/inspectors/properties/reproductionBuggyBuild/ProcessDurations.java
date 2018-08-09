package fr.inria.spirals.repairnator.process.inspectors.properties.reproductionBuggyBuild;

import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.step.BuildProject;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import fr.inria.spirals.repairnator.process.step.TestProject;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuild;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuildSourceCode;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutPatchedBuild;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutType;
import fr.inria.spirals.repairnator.process.step.paths.ComputeSourceDir;
import fr.inria.spirals.repairnator.process.step.paths.ComputeTestDir;
import fr.inria.spirals.repairnator.process.step.repair.AbstractRepairStep;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;

public class ProcessDurations {

    private GlobalStepInfo cloning;
    private GlobalStepInfo building;
    private GlobalStepInfo testing;
    private GlobalStepInfo fixing; // this property is specific for repairnator.json

    ProcessDurations() {
        this.cloning = new GlobalStepInfo();
        this.building = new GlobalStepInfo();
        this.testing = new GlobalStepInfo();
    }

    public GlobalStepInfo getCloning() {
        return cloning;
    }

    public GlobalStepInfo getBuilding() {
        return building;
    }

    public GlobalStepInfo getTesting() {
        return testing;
    }

    public GlobalStepInfo getFixing() {
        return fixing;
    }

    public void addGlobalStepInfo(AbstractStep step) {
        String stepName = step.getName();
        int stepDuration = step.getDuration();
        if (step instanceof CloneRepository) {
            this.cloning.addStep(stepName, stepDuration);
        } else if (step instanceof AbstractRepairStep) {
            if (this.fixing == null) {
                this.fixing = new GlobalStepInfo();
            }
            this.fixing.addStep(stepName, stepDuration);
        } else if (step instanceof TestProject &&
                (step.getInspector().getCheckoutType().equals(CheckoutType.CHECKOUT_BUGGY_BUILD) ||
                step.getInspector().getCheckoutType().equals(CheckoutType.CHECKOUT_BUGGY_BUILD_SOURCE_CODE))) {
            this.testing.addStep(stepName, stepDuration);
        } else if (step instanceof BuildProject &&
                (step.getInspector().getCheckoutType().equals(CheckoutType.CHECKOUT_BUGGY_BUILD) ||
                step.getInspector().getCheckoutType().equals(CheckoutType.CHECKOUT_BUGGY_BUILD_SOURCE_CODE))) {
            this.building.addStep(stepName, stepDuration);
        } else {
            if (step.getInspector().getBuildToBeInspected().getStatus() == ScannedBuildStatus.ONLY_FAIL ||
                    step.getInspector().getBuildToBeInspected().getStatus() == ScannedBuildStatus.FAILING_AND_PASSING) {
                if (step instanceof CheckoutBuggyBuild) {
                    this.building.addStep(stepName, stepDuration);
                }
            } else { // PASSING_AND_PASSING_WITH_TEST_CHANGES
                if (step instanceof CheckoutPatchedBuild || step instanceof ComputeSourceDir ||
                        step instanceof ComputeTestDir || step instanceof CheckoutBuggyBuildSourceCode) {
                    this.building.addStep(stepName, stepDuration);
                }
            }
        }
    }
}
