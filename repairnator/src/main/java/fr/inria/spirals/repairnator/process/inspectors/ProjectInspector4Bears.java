package fr.inria.spirals.repairnator.process.inspectors;

import fr.inria.spirals.jtravis.entities.BuildStatus;
import fr.inria.spirals.repairnator.RepairMode;
import fr.inria.spirals.repairnator.process.BuildToBeInspected;
import fr.inria.spirals.repairnator.process.ProjectState;
import fr.inria.spirals.repairnator.process.step.*;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by fermadeiral.
 */
public class ProjectInspector4Bears extends ProjectInspector {
    private final Logger logger = LoggerFactory.getLogger(ProjectInspector4Bears.class);

    public ProjectInspector4Bears(BuildToBeInspected buildToBeInspected, String workspace, List<AbstractDataSerializer> serializers,
                                  String nopolSolverPath, boolean push, RepairMode mode) {
        super(buildToBeInspected, workspace, serializers, null, push, mode);
        this.previousBuildFlag = false;
    }

    public void run() {
        AbstractStep firstStep = null;
        AbstractStep lastStep = null;

        // Clone, build, test and gather test information for the
        // current passing build to ensure it is reproducible
        AbstractStep cloneRepo = new CloneRepository(this);
        AbstractStep buildRepo = new BuildProject(this);
        AbstractStep testProject = new TestProject(this);
        this.testInformations = new GatherTestInformation(this, new BuildShouldPass());

        AbstractStep checkoutPreviousBuild = new CheckoutPreviousBuild(this);
        AbstractStep buildRepoForPreviousBuild = new BuildProject(this);
        AbstractStep testProjectForPreviousBuild = new TestProject(this);
        if (this.getPreviousBuild().getBuildStatus() == BuildStatus.FAILED) {
            AbstractStep gatherTestInformation = new GatherTestInformation(this, new BuildShouldFail());

            cloneRepo.setNextStep(buildRepo).setNextStep(testProject).setNextStep(this.testInformations)
                    .setNextStep(checkoutPreviousBuild).setNextStep(buildRepoForPreviousBuild)
                    .setNextStep(testProjectForPreviousBuild).setNextStep(gatherTestInformation);

            lastStep = gatherTestInformation;
        } else {
            AbstractStep gatherTestInformation = new GatherTestInformation(this, new BuildShouldPass());
            AbstractStep checkoutSourceCodeForPreviousBuild = new CheckoutSourceCodeForPreviousBuild(this);
            AbstractStep buildRepoForPreviousBuild2 = new BuildProject(this);
            AbstractStep testProjectForPreviousBuild2 = new TestProject(this);
            AbstractStep gatherTestInformation2 = new GatherTestInformation(this, new BuildShouldFail());

            cloneRepo.setNextStep(buildRepo).setNextStep(testProject).setNextStep(this.testInformations)
                    .setNextStep(checkoutPreviousBuild).setNextStep(buildRepoForPreviousBuild)
                    .setNextStep(testProjectForPreviousBuild).setNextStep(gatherTestInformation)
                    .setNextStep(checkoutSourceCodeForPreviousBuild).setNextStep(buildRepoForPreviousBuild2)
                    .setNextStep(testProjectForPreviousBuild2).setNextStep(gatherTestInformation2);

            lastStep = gatherTestInformation2;
        }

        if (this.getPushMode()) {
            PushIncriminatedBuild pushIncriminatedBuild = new PushIncriminatedBuild(this);
            pushIncriminatedBuild.setRemoteRepoUrl(PushIncriminatedBuild.REMOTE_REPO_BEAR);
            this.setPushBuild(pushIncriminatedBuild);
            lastStep.setNextStep(pushIncriminatedBuild);
        }

        firstStep = cloneRepo;
        firstStep.setState(ProjectState.INIT);
        firstStep.setDataSerializer(this.serializers);

        try {
            firstStep.execute();
        } catch (Exception e) {
            this.addStepError("Unknown", e.getMessage());
            this.logger.debug("Exception catch while executing steps: ", e);
        }
    }

}
