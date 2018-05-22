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

    private boolean bug;
    private String bugType;

    public ProjectInspector4Bears(BuildToBeInspected buildToBeInspected, String workspace, List<AbstractDataSerializer> serializers, List<AbstractNotifier> notifiers) {
        super(buildToBeInspected, workspace, serializers, notifiers);
        this.bug = false;
    }

    public void run() {
        AbstractStep cloneRepo = new CloneRepository(this);

        if (this.getBuildToBeInspected().getStatus() == ScannedBuildStatus.FAILING_AND_PASSING) {
            cloneRepo.setNextStep(new CheckoutBuggyBuild(this, true, CheckoutBuggyBuild.class.getSimpleName()+"Candidate"))
                    .setNextStep(new ComputeSourceDir(this, true, true))
                    .setNextStep(new ComputeTestDir(this, false))
                    .setNextStep(new BuildProject(this, true, BuildProject.class.getSimpleName()+"BuggyBuildCandidate"))
                    .setNextStep(new TestProject(this, true, TestProject.class.getSimpleName()+"BuggyBuildCandidate"))
                    .setNextStep(new GatherTestInformation(this, true, new BuildShouldFail(), false, GatherTestInformation.class.getSimpleName()+"BuggyBuildCandidate"))
                    .setNextStep(new InitRepoToPush(this))
                    .setNextStep(new ComputeClasspath(this, false))
                    .setNextStep(new CheckoutPatchedBuild(this, true, CheckoutPatchedBuild.class.getSimpleName()+"Candidate"))
                    .setNextStep(new BuildProject(this, true, BuildProject.class.getSimpleName()+"PatchedBuildCandidate"))
                    .setNextStep(new TestProject(this, true, TestProject.class.getSimpleName()+"PatchedBuildCandidate"))
                    .setNextStep(new GatherTestInformation(this, true, new BuildShouldPass(), true, GatherTestInformation.class.getSimpleName()+"PatchedBuildCandidate"))
                    .setNextStep(new CommitPatch(this, CommitType.COMMIT_HUMAN_PATCH));
        } else {
            if (this.getBuildToBeInspected().getStatus() == ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES) {
                cloneRepo.setNextStep(new CheckoutPatchedBuild(this, true, CheckoutPatchedBuild.class.getSimpleName()+"Candidate"))
                        .setNextStep(new ComputeSourceDir(this, true, true))
                        .setNextStep(new ComputeTestDir(this, false))
                        .setNextStep(new CheckoutBuggyBuildSourceCode(this, true, "CheckoutBuggyBuildCandidateSourceCode"))
                        .setNextStep(new BuildProject(this, true, BuildProject.class.getSimpleName()+"BuggyBuildCandidateSourceCode"))
                        .setNextStep(new TestProject(this, true, TestProject.class.getSimpleName()+"BuggyBuildCandidateSourceCode"))
                        .setNextStep(new GatherTestInformation(this, true, new BuildShouldFail(), false, GatherTestInformation.class.getSimpleName()+"BuggyBuildCandidateSourceCode"))
                        .setNextStep(new InitRepoToPush(this))
                        .setNextStep(new ComputeClasspath(this, false))
                        .setNextStep(new CheckoutPatchedBuild(this, true, CheckoutPatchedBuild.class.getSimpleName()+"Candidate"))
                        .setNextStep(new BuildProject(this, true, BuildProject.class.getSimpleName()+"PatchedBuildCandidate"))
                        .setNextStep(new TestProject(this, true, TestProject.class.getSimpleName()+"PatchedBuildCandidate"))
                        .setNextStep(new GatherTestInformation(this, true, new BuildShouldPass(), true, GatherTestInformation.class.getSimpleName()+"PatchedBuildCandidate"))
                        .setNextStep(new CommitPatch(this, CommitType.COMMIT_HUMAN_PATCH));
            } else {
                this.logger.debug("The pair of scanned builds is not interesting.");
                return;
            }
        }

        super.setFinalStep(new CommitProcessEnd(this));
        super.getFinalStep().setNextStep(new PushProcessEnd(this));

        cloneRepo.setDataSerializer(this.getSerializers());
        cloneRepo.setNotifiers(this.getNotifiers());

        super.printPipeline();

        try {
            cloneRepo.execute();
        } catch (Exception e) {
            this.getJobStatus().addStepError("Unknown", e.getMessage());
            this.logger.debug("Exception catch while executing steps: ", e);
        }
    }

    public boolean isBug() {
        return bug;
    }

    public String getBugType() {
        return bugType;
    }

    public void setBug(boolean bug, String bugType) {
        this.bug = bug;
        this.bugType = bugType;
    }

}
