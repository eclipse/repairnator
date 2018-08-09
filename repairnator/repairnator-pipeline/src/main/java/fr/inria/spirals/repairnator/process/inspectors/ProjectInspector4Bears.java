package fr.inria.spirals.repairnator.process.inspectors;

import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuildTestCode;
import fr.inria.spirals.repairnator.process.step.paths.ComputeClasspath;
import fr.inria.spirals.repairnator.process.step.paths.ComputeModules;
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

    protected void initProperties() {
        this.getJobStatus().getProperties().setVersion("Bears 1.0");
        super.initProperties();
    }

    public String getRemoteBranchName() {
        return this.getRepoSlug().replace('/', '-') + '-' + this.getBuggyBuild().getId() + '-' + this.getPatchedBuild().getId();
    }

    public void run() {
        AbstractStep cloneRepo = new CloneRepository(this);

        if (this.getBuildToBeInspected().getStatus() == ScannedBuildStatus.FAILING_AND_PASSING) {
            cloneRepo.addNextStep(new CheckoutBuggyBuild(this, true, CheckoutBuggyBuild.class.getSimpleName()+"Candidate"))
                    .addNextStep(new ComputeSourceDir(this, false, true))
                    .addNextStep(new ComputeTestDir(this, false))
                    .addNextStep(new BuildProject(this, true, BuildProject.class.getSimpleName()+"BuggyBuildCandidate"))
                    .addNextStep(new TestProject(this, true, TestProject.class.getSimpleName()+"BuggyBuildCandidate"))
                    .addNextStep(new GatherTestInformation(this, true, new BuildShouldFail(), false, GatherTestInformation.class.getSimpleName()+"BuggyBuildCandidate"))
                    .addNextStep(new InitRepoToPush(this))
                    .addNextStep(new ComputeClasspath(this, false))
                    .addNextStep(new ComputeModules(this, false))
                    .addNextStep(new CheckoutPatchedBuild(this, true, CheckoutPatchedBuild.class.getSimpleName()+"Candidate"))
                    .addNextStep(new BuildProject(this, true, BuildProject.class.getSimpleName()+"PatchedBuildCandidate"))
                    .addNextStep(new TestProject(this, true, TestProject.class.getSimpleName()+"PatchedBuildCandidate"))
                    .addNextStep(new GatherTestInformation(this, true, new BuildShouldPass(), true, GatherTestInformation.class.getSimpleName()+"PatchedBuildCandidate"))
                    .addNextStep(new CommitPatch(this, CommitType.COMMIT_HUMAN_PATCH));
        } else {
            if (this.getBuildToBeInspected().getStatus() == ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES) {
                cloneRepo.addNextStep(new CheckoutPatchedBuild(this, true, CheckoutPatchedBuild.class.getSimpleName()+"Candidate"))
                        .addNextStep(new ComputeSourceDir(this, true, true))
                        .addNextStep(new ComputeTestDir(this, true))
                        .addNextStep(new CheckoutBuggyBuildSourceCode(this, true, "CheckoutBuggyBuildCandidateSourceCode"))
                        .addNextStep(new BuildProject(this, true, BuildProject.class.getSimpleName()+"BuggyBuildCandidateSourceCode"))
                        .addNextStep(new TestProject(this, true, TestProject.class.getSimpleName()+"BuggyBuildCandidateSourceCode"))
                        .addNextStep(new GatherTestInformation(this, true, new BuildShouldFail(), false, GatherTestInformation.class.getSimpleName()+"BuggyBuildCandidateSourceCode"))
                        .addNextStep(new CheckoutBuggyBuildTestCode(this, true))
                        .addNextStep(new InitRepoToPush(this))
                        .addNextStep(new ComputeClasspath(this, false))
                        .addNextStep(new ComputeModules(this, false))
                        .addNextStep(new CheckoutBuggyBuildSourceCode(this, true, "CheckoutBuggyBuildCandidateSourceCode"))
                        .addNextStep(new CommitChangedTests(this))
                        .addNextStep(new CheckoutPatchedBuild(this, true, CheckoutPatchedBuild.class.getSimpleName()+"Candidate"))
                        .addNextStep(new BuildProject(this, true, BuildProject.class.getSimpleName()+"PatchedBuildCandidate"))
                        .addNextStep(new TestProject(this, true, TestProject.class.getSimpleName()+"PatchedBuildCandidate"))
                        .addNextStep(new GatherTestInformation(this, true, new BuildShouldPass(), true, GatherTestInformation.class.getSimpleName()+"PatchedBuildCandidate"))
                        .addNextStep(new CommitPatch(this, CommitType.COMMIT_HUMAN_PATCH));
            } else {
                this.logger.debug("The pair of builds " + this.getBuggyBuild().getId() + ", " +
                        this.getPatchedBuild().getId() + " is not interesting.");
                return;
            }
        }

        super.setFinalStep(new WritePropertyFile(this));
        super.getFinalStep()
                .addNextStep(new CommitProcessEnd(this))
                .addNextStep(new PushProcessEnd(this));

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
