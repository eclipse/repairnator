package fr.inria.spirals.repairnator.process.inspectors;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.BuildStatus;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.repairnator.RepairMode;
import fr.inria.spirals.repairnator.process.ProjectState;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.step.BuildProject;
import fr.inria.spirals.repairnator.process.step.BuildShouldFail;
import fr.inria.spirals.repairnator.process.step.BuildShouldPass;
import fr.inria.spirals.repairnator.process.step.CheckoutPreviousBuild;
import fr.inria.spirals.repairnator.process.step.CheckoutSourceCodeForPreviousBuild;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import fr.inria.spirals.repairnator.process.step.GatherTestInformation;
import fr.inria.spirals.repairnator.process.step.PushIncriminatedBuild;
import fr.inria.spirals.repairnator.process.step.TestProject;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;

/**
 * Created by fermadeiral.
 */
public class ProjectInspector4Bears extends ProjectInspector {
    private final Logger logger = LoggerFactory.getLogger(ProjectInspector4Bears.class);

    public ProjectInspector4Bears(Build build, String workspace, List<AbstractDataSerializer> serializers,
                                  String nopolSolverPath, boolean push, RepairMode mode) {
        super(build, workspace, serializers, null, push, mode);
        this.previousBuildFlag = false;
    }

	public void run() {
		// First of all, here it is checked if the current passing build has a
		// previous build---if doesn't, nothing is made as this build is not
		// useful for Bears
		Build previousBuild = BuildHelper.getLastBuildOfSameBranchOfStatusBeforeBuild(this.getBuild(), null);

		if (previousBuild != null) {

			this.logger.debug("Build: " + this.getBuild().getId());
			this.logger.debug("Previous build: " + previousBuild.getId());

			if (previousBuild.getBuildStatus() == BuildStatus.FAILED
					|| previousBuild.getBuildStatus() == BuildStatus.PASSED) {

				this.setPreviousBuild(previousBuild);

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
			} else {
				this.setState(ProjectState.PREVIOUSVERSIONISNOTINTERESTING);
			}
		} else {
			this.setState(ProjectState.DOESNOTHAVEPREVIOUSVERSION);
		}
	}
}
