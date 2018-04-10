package fr.inria.spirals.repairnator.process.inspectors;

import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.process.step.pathes.ComputeClasspath;
import fr.inria.spirals.repairnator.process.step.pathes.ComputeSourceDir;
import fr.inria.spirals.repairnator.process.step.pathes.ComputeTestDir;
import fr.inria.spirals.repairnator.process.step.push.InitRepoToPush;
import fr.inria.spirals.repairnator.process.step.push.PushIncriminatedBuild;
import fr.inria.spirals.repairnator.process.step.push.CommitPatch;
import fr.inria.spirals.repairnator.process.step.repair.NopolRepair;
import fr.inria.spirals.repairnator.states.PipelineState;
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
            cloneRepo.setNextStep(new CheckoutBuggyBuild(this))
                    .setNextStep(new ComputeSourceDir(this, true))
                    .setNextStep(new ComputeTestDir(this))
                    .setNextStep(new ResolveDependency(this))
                    .setNextStep(new BuildProject(this, BuildProject.class.getSimpleName()+"PreviousBuild"))
                    .setNextStep(new TestProject(this, TestProject.class.getSimpleName()+"PreviousBuild"))
                    .setNextStep(new GatherTestInformation(this, new BuildShouldFail(), false, GatherTestInformation.class.getSimpleName()+"PreviousBuild"))
                    .setNextStep(new InitRepoToPush(this))
                    .setNextStep(new ComputeClasspath(this))
                    .setNextStep(new NopolRepair(this))
                    .setNextStep(new CommitPatch(this, false))
                    .setNextStep(new CheckoutPatchedBuild(this))
                    .setNextStep(new BuildProject(this, BuildProject.class.getSimpleName()+"Build"))
                    .setNextStep(new TestProject(this, TestProject.class.getSimpleName()+"Build"))
                    .setNextStep(new GatherTestInformation(this, new BuildShouldPass(), true, GatherTestInformation.class.getSimpleName()+"Build"))
                    .setNextStep(new PushIncriminatedBuild(this))
                    .setNextStep(new CommitPatch(this, true));
        } else {
            if (this.getBuildToBeInspected().getStatus() == ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES) {
                cloneRepo.setNextStep(new CheckoutPatchedBuild(this))
                        .setNextStep(new ComputeSourceDir(this, true))
                        .setNextStep(new ComputeTestDir(this))
                        .setNextStep(new CheckoutBuggyBuildSourceCode(this))
                        .setNextStep(new ResolveDependency(this))
                        .setNextStep(new BuildProject(this, BuildProject.class.getSimpleName()+"PreviousBuildSourceCode"))
                        .setNextStep(new TestProject(this, TestProject.class.getSimpleName()+"PreviousBuildSourceCode"))
                        .setNextStep(new GatherTestInformation(this, new BuildShouldFail(), false, GatherTestInformation.class.getSimpleName()+"PreviousBuildSourceCode"))
                        .setNextStep(new InitRepoToPush(this))
                        .setNextStep(new ComputeClasspath(this))
                        .setNextStep(new NopolRepair(this))
                        .setNextStep(new CommitPatch(this, false))
                        .setNextStep(new CheckoutPatchedBuild(this))
                        .setNextStep(new BuildProject(this, BuildProject.class.getSimpleName()+"Build"))
                        .setNextStep(new TestProject(this, TestProject.class.getSimpleName()+"Build"))
                        .setNextStep(new GatherTestInformation(this, new BuildShouldPass(), true, GatherTestInformation.class.getSimpleName()+"Build"))
                        .setNextStep(new PushIncriminatedBuild(this))
                        .setNextStep(new CommitPatch(this, true));
            } else {
                this.logger.debug("The pair of scanned builds is not interesting.");
                return;
            }
        }

        firstStep = cloneRepo;
        firstStep.setDataSerializer(this.getSerializers());
        firstStep.setNotifiers(this.getNotifiers());
        firstStep.setPipelineState(PipelineState.INIT);

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
