package fr.inria.spirals.repairnator.process.inspectors;

import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.ScannedBuildStatus;
import fr.inria.spirals.repairnator.process.step.*;
import fr.inria.spirals.repairnator.process.step.gatherinfocontract.BuildShouldFail;
import fr.inria.spirals.repairnator.process.step.gatherinfocontract.BuildShouldPass;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by fermadeiral.
 */
public class ProjectInspector4Bears extends ProjectInspector {
    private final Logger logger = LoggerFactory.getLogger(ProjectInspector4Bears.class);

    private boolean isFixerBuild_Case1;
    private boolean isFixerBuild_Case2;

    public ProjectInspector4Bears(BuildToBeInspected buildToBeInspected, String workspace, List<AbstractDataSerializer> serializers) {
        super(buildToBeInspected, workspace, serializers);
        super.setPreviousBuildFlag(false);
        this.isFixerBuild_Case1 = false;
        this.isFixerBuild_Case2 = false;
    }

    public void run() {
        AbstractStep firstStep;

        AbstractStep cloneRepo = new CloneRepository(this);
        GatherTestInformation gatherTestInformation;

        if (this.getBuildToBeInspected().getStatus() == ScannedBuildStatus.FAILING_AND_PASSING) {
            gatherTestInformation = new GatherTestInformation(this, new BuildShouldFail());

            cloneRepo.setNextStep(new CheckoutBuild(this))
                    .setNextStep(new BuildProject(this))
                    .setNextStep(new TestProject(this))
                    .setNextStep(new GatherTestInformation(this, new BuildShouldPass()))
                    .setNextStep(new CheckoutPreviousBuild(this))
                    .setNextStep(new BuildProject(this))
                    .setNextStep(new TestProject(this))
                    .setNextStep(gatherTestInformation)
                    .setNextStep(new SquashRepository(this))
                    .setNextStep(new PushIncriminatedBuild(this));

        } else {
            if (this.getBuildToBeInspected().getStatus() == ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES) {
                gatherTestInformation = new GatherTestInformation(this, new BuildShouldFail());

                cloneRepo.setNextStep(new CheckoutBuild(this))
                        .setNextStep(new BuildProject(this))
                        .setNextStep(new TestProject(this))
                        .setNextStep(new GatherTestInformation(this, new BuildShouldPass()))
                        .setNextStep(new CheckoutPreviousBuild(this))
                        .setNextStep(new BuildProject(this))
                        .setNextStep(new TestProject(this))
                        .setNextStep(new GatherTestInformation(this, new BuildShouldPass()))
                        .setNextStep(new CheckoutPreviousBuildSourceCode(this))
                        .setNextStep(new BuildProject(this))
                        .setNextStep(new TestProject(this))
                        .setNextStep(gatherTestInformation)
                        .setNextStep(new SquashRepository(this))
                        .setNextStep(new PushIncriminatedBuild(this));
            } else {
                this.logger.debug("The pair of scanned builds is not interesting.");
                return;
            }
        }

        this.setTestInformations(gatherTestInformation);

        firstStep = cloneRepo;
        firstStep.setDataSerializer(this.getSerializers());
        firstStep.setState(ProjectState.INIT);

        try {
            firstStep.execute();
        } catch (Exception e) {
            this.addStepError("Unknown", e.getMessage());
            this.logger.debug("Exception catch while executing steps: ", e);
        }
    }

    public boolean isFixerBuildCase1() {
        return this.isFixerBuild_Case1;
    }

    public void setFixerBuildCase1(boolean fixerBuildCase1) {
        this.isFixerBuild_Case1 = fixerBuildCase1;
    }

    public boolean isFixerBuildCase2() {
        return this.isFixerBuild_Case2;
    }

    public void setFixerBuildCase2(boolean fixerBuildCase2) {
        this.isFixerBuild_Case2 = fixerBuildCase2;
    }
}
