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
        AbstractStep firstStep = null;
        AbstractStep lastStep = null;

        // Clone, build, test and gather test information for the
        // current passing build to ensure it is reproducible
        AbstractStep cloneRepo = new CloneRepository(this);
        AbstractStep checkoutBuild = new CheckoutBuild(this);
        AbstractStep buildRepo = new BuildProject(this);
        AbstractStep testProject = new TestProject(this);
        this.setTestInformations(new GatherTestInformation(this, new BuildShouldPass()));

        AbstractStep checkoutPreviousBuild = new CheckoutPreviousBuild(this);
        AbstractStep buildRepoForPreviousBuild = new BuildProject(this);
        AbstractStep testProjectForPreviousBuild = new TestProject(this);
        if (this.getBuildToBeInspected().getStatus() == ScannedBuildStatus.FAILING_AND_PASSING) {
            AbstractStep gatherTestInformation = new GatherTestInformation(this, new BuildShouldFail());

            cloneRepo.setNextStep(checkoutBuild).setNextStep(buildRepo).setNextStep(testProject).setNextStep(this.getTestInformations())
                    .setNextStep(checkoutPreviousBuild).setNextStep(buildRepoForPreviousBuild)
                    .setNextStep(testProjectForPreviousBuild).setNextStep(gatherTestInformation);

            lastStep = gatherTestInformation;
        } else {
            if (this.getBuildToBeInspected().getStatus() == ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES) {
                AbstractStep gatherTestInformation = new GatherTestInformation(this, new BuildShouldPass());
                AbstractStep checkoutSourceCodeForPreviousBuild = new CheckoutPreviousBuildSourceCode(this);
                AbstractStep buildRepoForPreviousBuild2 = new BuildProject(this);
                AbstractStep testProjectForPreviousBuild2 = new TestProject(this);
                AbstractStep gatherTestInformation2 = new GatherTestInformation(this, new BuildShouldFail());

                cloneRepo.setNextStep(checkoutBuild).setNextStep(buildRepo).setNextStep(testProject).setNextStep(this.getTestInformations())
                        .setNextStep(checkoutPreviousBuild).setNextStep(buildRepoForPreviousBuild)
                        .setNextStep(testProjectForPreviousBuild).setNextStep(gatherTestInformation)
                        .setNextStep(checkoutSourceCodeForPreviousBuild).setNextStep(buildRepoForPreviousBuild2)
                        .setNextStep(testProjectForPreviousBuild2).setNextStep(gatherTestInformation2);

                lastStep = gatherTestInformation2;
            } else {
                this.logger.debug("The pair of scanned builds is not interesting.");
            }
        }

        if (this.getPushMode()) {
            PushIncriminatedBuild pushIncriminatedBuild = new PushIncriminatedBuild(this);
            pushIncriminatedBuild.setRemoteRepoUrl(PushIncriminatedBuild.REMOTE_REPO_BEAR);
            this.setPushBuild(pushIncriminatedBuild);
            lastStep.setNextStep(pushIncriminatedBuild);
        }

        firstStep = cloneRepo;
        firstStep.setState(ProjectState.INIT);
        firstStep.setDataSerializer(this.getSerializers());

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
