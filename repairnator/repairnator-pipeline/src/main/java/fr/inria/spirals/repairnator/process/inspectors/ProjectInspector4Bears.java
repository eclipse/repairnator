package fr.inria.spirals.repairnator.process.inspectors;

import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.process.step.paths.ComputeClasspath;
import fr.inria.spirals.repairnator.process.step.paths.ComputeSourceDir;
import fr.inria.spirals.repairnator.process.step.paths.ComputeTestDir;
import fr.inria.spirals.repairnator.process.step.push.*;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import fr.inria.spirals.repairnator.notifier.AbstractNotifier;
import fr.inria.spirals.repairnator.process.step.*;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutPatchedBuild;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuild;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuildSourceCode;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldFail;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldPass;
import fr.inria.spirals.repairnator.process.step.gatherinfo.GatherTestInformation;
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

    public ProjectInspector4Bears(BuildToBeInspected buildToBeInspected, String workspace, List<AbstractDataSerializer> serializers, List<AbstractNotifier> notifiers) {
        super(buildToBeInspected, workspace, serializers, notifiers);
        this.isFixerBuild_Case1 = false;
        this.isFixerBuild_Case2 = false;
    }

    public void run() {
        AbstractStep firstStep;

        AbstractStep cloneRepo = new CloneRepository(this);

        if (this.getBuildToBeInspected().getStatus() == ScannedBuildStatus.FAILING_AND_PASSING) {
            cloneRepo.setNextStep(new CheckoutBuggyBuild(this, true))
                    .setNextStep(new ComputeSourceDir(this, true, true))
                    .setNextStep(new ComputeTestDir(this, false))
                    .setNextStep(new BuildProject(this, BuildProject.class.getSimpleName()+"PreviousBuild", true))
                    .setNextStep(new TestProject(this, TestProject.class.getSimpleName()+"PreviousBuild", true))
                    .setNextStep(new GatherTestInformation(this, true, new BuildShouldFail(), false, GatherTestInformation.class.getSimpleName()+"PreviousBuild"))
                    .setNextStep(new InitRepoToPush(this))
                    .setNextStep(new ComputeClasspath(this, false))
                    .setNextStep(new CheckoutPatchedBuild(this, true))
                    .setNextStep(new BuildProject(this, BuildProject.class.getSimpleName()+"Build", true))
                    .setNextStep(new TestProject(this, TestProject.class.getSimpleName()+"Build", true))
                    .setNextStep(new GatherTestInformation(this, true, new BuildShouldPass(), true, GatherTestInformation.class.getSimpleName()+"Build"))
                    .setNextStep(new CommitPatch(this, CommitType.COMMIT_HUMAN_PATCH));
        } else {
            if (this.getBuildToBeInspected().getStatus() == ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES) {
                cloneRepo.setNextStep(new CheckoutPatchedBuild(this, true))
                        .setNextStep(new ComputeSourceDir(this, true, true))
                        .setNextStep(new ComputeTestDir(this, false))
                        .setNextStep(new CheckoutBuggyBuildSourceCode(this, true))
                        .setNextStep(new BuildProject(this, BuildProject.class.getSimpleName()+"PreviousBuildSourceCode", true))
                        .setNextStep(new TestProject(this, TestProject.class.getSimpleName()+"PreviousBuildSourceCode", true))
                        .setNextStep(new GatherTestInformation(this, true, new BuildShouldFail(), false, GatherTestInformation.class.getSimpleName()+"PreviousBuildSourceCode"))
                        .setNextStep(new InitRepoToPush(this))
                        .setNextStep(new ComputeClasspath(this, false))
                        .setNextStep(new CheckoutPatchedBuild(this, true))
                        .setNextStep(new BuildProject(this, BuildProject.class.getSimpleName()+"Build", true))
                        .setNextStep(new TestProject(this, TestProject.class.getSimpleName()+"Build", true))
                        .setNextStep(new GatherTestInformation(this, true, new BuildShouldPass(), true, GatherTestInformation.class.getSimpleName()+"Build"))
                        .setNextStep(new CommitPatch(this, CommitType.COMMIT_HUMAN_PATCH));
            } else {
                this.logger.debug("The pair of scanned builds is not interesting.");
                return;
            }
        }

        super.setFinalStep(new CommitProcessEnd(this));
        super.getFinalStep().setNextStep(new PushProcessEnd(this));

        firstStep = cloneRepo;
        firstStep.setDataSerializer(this.getSerializers());
        firstStep.setNotifiers(this.getNotifiers());

        this.printPipeline();

        try {
            firstStep.execute();
        } catch (Exception e) {
            this.getJobStatus().addStepError("Unknown", e.getMessage());
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
