package fr.inria.spirals.repairnator.process.inspectors.components;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
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

public class RunInspector4Bears extends IRunInspector {
	
	@Override
	public void run (ProjectInspector inspector) {
		AbstractStep cloneRepo = new CloneRepository(inspector);

        if (inspector.getBuildToBeInspected().getStatus() == ScannedBuildStatus.FAILING_AND_PASSING) {
            cloneRepo.addNextStep(new CheckoutBuggyBuild(inspector, true, CheckoutBuggyBuild.class.getSimpleName()+"Candidate"))
                    .addNextStep(new ComputeSourceDir(inspector, false, true))
                    .addNextStep(new ComputeTestDir(inspector, false))
                    .addNextStep(new BuildProject(inspector, true, BuildProject.class.getSimpleName()+"BuggyBuildCandidate"))
                    .addNextStep(new TestProject(inspector, true, TestProject.class.getSimpleName()+"BuggyBuildCandidate"))
                    .addNextStep(new GatherTestInformation(inspector, true, new BuildShouldFail(), false, GatherTestInformation.class.getSimpleName()+"BuggyBuildCandidate"))
                    .addNextStep(new InitRepoToPush(inspector))
                    .addNextStep(new ComputeClasspath(inspector, false))
                    .addNextStep(new ComputeModules(inspector, false))
                    .addNextStep(new CheckoutPatchedBuild(inspector, true, CheckoutPatchedBuild.class.getSimpleName()+"Candidate"))
                    .addNextStep(new BuildProject(inspector, true, BuildProject.class.getSimpleName()+"PatchedBuildCandidate"))
                    .addNextStep(new TestProject(inspector, true, TestProject.class.getSimpleName()+"PatchedBuildCandidate"))
                    .addNextStep(new GatherTestInformation(inspector, true, new BuildShouldPass(), true, GatherTestInformation.class.getSimpleName()+"PatchedBuildCandidate"))
                    .addNextStep(new CommitPatch(inspector, CommitType.COMMIT_HUMAN_PATCH));
        } else {
            if (inspector.getBuildToBeInspected().getStatus() == ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES) {
                cloneRepo.addNextStep(new CheckoutPatchedBuild(inspector, true, CheckoutPatchedBuild.class.getSimpleName()+"Candidate"))
                        .addNextStep(new ComputeSourceDir(inspector, true, true))
                        .addNextStep(new ComputeTestDir(inspector, true))
                        .addNextStep(new CheckoutBuggyBuildSourceCode(inspector, true, "CheckoutBuggyBuildCandidateSourceCode"))
                        .addNextStep(new BuildProject(inspector, true, BuildProject.class.getSimpleName()+"BuggyBuildCandidateSourceCode"))
                        .addNextStep(new TestProject(inspector, true, TestProject.class.getSimpleName()+"BuggyBuildCandidateSourceCode"))
                        .addNextStep(new GatherTestInformation(inspector, true, new BuildShouldFail(), false, GatherTestInformation.class.getSimpleName()+"BuggyBuildCandidateSourceCode"))
                        .addNextStep(new CheckoutBuggyBuildTestCode(inspector, true))
                        .addNextStep(new InitRepoToPush(inspector))
                        .addNextStep(new ComputeClasspath(inspector, false))
                        .addNextStep(new ComputeModules(inspector, false))
                        .addNextStep(new CheckoutBuggyBuildSourceCode(inspector, true, "CheckoutBuggyBuildCandidateSourceCode"))
                        .addNextStep(new CommitChangedTests(inspector))
                        .addNextStep(new CheckoutPatchedBuild(inspector, true, CheckoutPatchedBuild.class.getSimpleName()+"Candidate"))
                        .addNextStep(new BuildProject(inspector, true, BuildProject.class.getSimpleName()+"PatchedBuildCandidate"))
                        .addNextStep(new TestProject(inspector, true, TestProject.class.getSimpleName()+"PatchedBuildCandidate"))
                        .addNextStep(new GatherTestInformation(inspector, true, new BuildShouldPass(), true, GatherTestInformation.class.getSimpleName()+"PatchedBuildCandidate"))
                        .addNextStep(new CommitPatch(inspector, CommitType.COMMIT_HUMAN_PATCH));
            } else {
                inspector.getLogger().debug("The pair of builds " + inspector.getBuggyBuild().getId() + ", " +
                        inspector.getPatchedBuild().getId() + " is not interesting.");
                return;
            }
        }

        inspector.setFinalStep(new WritePropertyFile(inspector));
        inspector.getFinalStep()
                .addNextStep(new CommitProcessEnd(inspector))
                .addNextStep(new PushProcessEnd(inspector));

        cloneRepo.setDataSerializer(inspector.getSerializers());
        cloneRepo.setNotifiers(inspector.getNotifiers());

        inspector.printPipeline();

        try {
            cloneRepo.execute();
        } catch (Exception e) {
            inspector.getJobStatus().addStepError("Unknown", e.getMessage());
            inspector.getLogger().debug("Exception catch while executing steps: ", e);
        }
	}
}
