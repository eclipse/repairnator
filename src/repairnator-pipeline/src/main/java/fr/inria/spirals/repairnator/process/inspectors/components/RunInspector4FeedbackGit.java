package fr.inria.spirals.repairnator.process.inspectors.components;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.ErrorNotifier;
import fr.inria.spirals.repairnator.pipeline.RepairToolsManager;
import fr.inria.spirals.repairnator.process.inspectors.GitRepositoryProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.*;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutPatchedBuild;
import fr.inria.spirals.repairnator.process.step.feedback.sobo.SoboBot;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldFail;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldPass;
import fr.inria.spirals.repairnator.process.step.gatherinfo.GatherTestInformation;
import fr.inria.spirals.repairnator.process.step.paths.ComputeClasspath;
import fr.inria.spirals.repairnator.process.step.paths.ComputeModules;
import fr.inria.spirals.repairnator.process.step.paths.ComputeSourceDir;
import fr.inria.spirals.repairnator.process.step.paths.ComputeTestDir;
import fr.inria.spirals.repairnator.process.step.push.*;
import fr.inria.spirals.repairnator.process.step.repair.AbstractRepairStep;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.states.LauncherMode;

public class RunInspector4FeedbackGit extends IRunInspector{
    private boolean command = false;
    public RunInspector4FeedbackGit() {}
    public RunInspector4FeedbackGit(boolean command) {
        this.command = command;
    }

	@Override
	public void run(ProjectInspector inspector_in) {
        GitRepositoryProjectInspector inspector = (GitRepositoryProjectInspector) inspector_in;
		if (inspector.getGitRepositoryUrl() != null) {
		    if(command){
                    SoboBot feedbackStep=new SoboBot(inspector);
                    feedbackStep.addNextStep(new GitRepositoryCommitPatch(inspector, CommitType.COMMIT_REPAIR_INFO))
                            .addNextStep(new CheckoutPatchedBuild(inspector, true))
                            .addNextStep(new BuildProject(inspector))
                            .addNextStep(new TestProject(inspector))
                            .addNextStep(new GatherTestInformation(inspector, true, new BuildShouldPass(), true));

                    AbstractStep finalStep = new ComputeSourceDir(inspector, false, true);

                    finalStep.
                            addNextStep(new ComputeModules(inspector, false)).
                            addNextStep(new WritePropertyFile(inspector)).
                            addNextStep(new GitRepositoryCommitProcessEnd(inspector)).
                            addNextStep(new GitRepositoryPushProcessEnd(inspector));
                    feedbackStep.execute();


                }
                else {
                    AbstractStep cloneRepo = new CloneRepository(inspector);


                    SoboBot feedbackStep = new SoboBot();
                    feedbackStep.setProjectInspector(inspector);
                    cloneRepo.addNextStep(feedbackStep);
                    cloneRepo.addNextStep(new GitRepositoryCommitPatch(inspector, CommitType.COMMIT_REPAIR_INFO))
                            .addNextStep(new CheckoutPatchedBuild(inspector, true))
                            .addNextStep(new BuildProject(inspector))
                            .addNextStep(new TestProject(inspector))
                            .addNextStep(new GatherTestInformation(inspector, true, new BuildShouldPass(), true));

                    AbstractStep finalStep = new ComputeSourceDir(inspector, false, true); // this step is used to compute code metrics on the project

                    finalStep.
                            addNextStep(new ComputeModules(inspector, false)).
                            addNextStep(new WritePropertyFile(inspector)).
                            addNextStep(new GitRepositoryCommitProcessEnd(inspector)).
                            addNextStep(new GitRepositoryPushProcessEnd(inspector));
                    cloneRepo.execute();
                }

            }else {
            inspector.getLogger().debug("Build " + inspector.getBuggyBuild().getId() + " is not a failing build.");
        }
	}
}