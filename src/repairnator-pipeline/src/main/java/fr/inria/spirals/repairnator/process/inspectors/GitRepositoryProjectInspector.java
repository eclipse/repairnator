package fr.inria.spirals.repairnator.process.inspectors;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.AbstractNotifier;
import fr.inria.spirals.repairnator.notifier.ErrorNotifier;
import fr.inria.spirals.repairnator.pipeline.RepairToolsManager;
import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.step.AddExperimentalPluginRepo;
import fr.inria.spirals.repairnator.process.step.BuildProject;
import fr.inria.spirals.repairnator.process.step.CloneCheckoutBranchRepository;
import fr.inria.spirals.repairnator.process.step.TestProject;
import fr.inria.spirals.repairnator.process.step.WritePropertyFile;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutPatchedBuild;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutType;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldFail;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldPass;
import fr.inria.spirals.repairnator.process.step.gatherinfo.GatherTestInformation;
import fr.inria.spirals.repairnator.process.step.paths.ComputeClasspath;
import fr.inria.spirals.repairnator.process.step.paths.ComputeModules;
import fr.inria.spirals.repairnator.process.step.paths.ComputeSourceDir;
import fr.inria.spirals.repairnator.process.step.paths.ComputeTestDir;
import fr.inria.spirals.repairnator.process.step.push.CommitPatch;
import fr.inria.spirals.repairnator.process.step.push.CommitProcessEnd;
import fr.inria.spirals.repairnator.process.step.push.CommitType;
import fr.inria.spirals.repairnator.process.step.push.GitRepositoryPushProcessEnd;
import fr.inria.spirals.repairnator.process.step.push.InitRepoToPush;
import fr.inria.spirals.repairnator.process.step.repair.AbstractRepairStep;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class initialize the pipelines by creating the steps:
 * it's the backbone of the pipeline.
 */
public class GitRepositoryProjectInspector extends ProjectInspector {
    private final Logger logger = LoggerFactory.getLogger(GitRepositoryProjectInspector.class);

    private String gitRepositoryUrl;
    private String gitRepositoryBranch;
    private String gitRepositoryIdCommit;
    private boolean gitRepositoryFirstCommit;
    
    public GitRepositoryProjectInspector(String gitRepoUrl, String gitRepoBranch, String gitRepoIdCommit, boolean isGitRepositoryFirstCommit,
    		String workspace, List<AbstractDataSerializer> serializers, List<AbstractNotifier> notifiers) {
    	
    	this.gitRepositoryUrl = gitRepoUrl;
        this.gitRepositoryBranch = gitRepoBranch;
        this.gitRepositoryIdCommit = gitRepoIdCommit;
        this.gitRepositoryFirstCommit = isGitRepositoryFirstCommit;

        this.gitSlug = this.gitRepositoryUrl.split("https://github.com/",2)[1].replace("/", "-");

        this.workspace = workspace;
        this.repoLocalPath = workspace + File.separator + getProjectIdToBeInspected();

        this.repoToPushLocalPath = repoLocalPath+"_topush";
        this.m2LocalPath = new File(this.repoLocalPath + File.separator + ".m2").getAbsolutePath();
        this.serializers = serializers;
        this.gitHelper = new GitHelper();
        this.jobStatus = new JobStatus(repoLocalPath);
        this.notifiers = notifiers;
        this.checkoutType = CheckoutType.NO_CHECKOUT;
        this.steps = new ArrayList<>();
    }
    
    public String getGitRepositoryUrl() {
    	return this.gitRepositoryUrl;
    }

    public String getGitRepositoryBranch() {
    	return this.gitRepositoryBranch;
    }

    public String getGitRepositoryIdCommit() {
    	return this.gitRepositoryIdCommit;
    }

    public boolean isGitRepositoryFirstCommit() {
    	return this.gitRepositoryFirstCommit;
    }
    
    @Override
    public String getProjectIdToBeInspected() {
    	return getGitSlug() + "-" + (getGitRepositoryBranch() != null ? getGitRepositoryBranch() : "master") +
				(getGitRepositoryIdCommit() != null ? "-" + getGitRepositoryIdCommit() : "") +
				(isGitRepositoryFirstCommit() ? "-firstCommit" : "");
    }

    @Override
    public void run() {
        if (getGitRepositoryUrl() != null) {
            AbstractStep cloneRepo = new CloneCheckoutBranchRepository(this);
            
            // If we have experimental plugins, we need to add them here.
            String[] repos = RepairnatorConfig.getInstance().getExperimentalPluginRepoList();
            if(repos != null) {
                for(int i = 0; i < repos.length-1; i =+ 2) {
                    cloneRepo.addNextStep(new AddExperimentalPluginRepo(this, repos[i], repos[i+1], repos[i+2]));
                }
            }
            // Add the next steps
            cloneRepo
                    .addNextStep(new BuildProject(this))
                    .addNextStep(new TestProject(this))
                    .addNextStep(new GatherTestInformation(this, true, new BuildShouldFail(), false))
                    .addNextStep(new InitRepoToPush(this))
                    .addNextStep(new ComputeClasspath(this, false))
                    .addNextStep(new ComputeSourceDir(this, false, false))
                    .addNextStep(new ComputeTestDir(this, false));

            for (String repairToolName : RepairnatorConfig.getInstance().getRepairTools()) {
                AbstractRepairStep repairStep = RepairToolsManager.getStepFromName(repairToolName);
                if (repairStep != null) {
                    repairStep.setProjectInspector(this);
                    cloneRepo.addNextStep(repairStep);
                } else {
                    logger.error("Error while getting repair step class for following name: " + repairToolName);
                }
            }

            cloneRepo.addNextStep(new CommitPatch(this, CommitType.COMMIT_REPAIR_INFO))
                    .addNextStep(new CheckoutPatchedBuild(this, true))
                    .addNextStep(new BuildProject(this))
                    .addNextStep(new TestProject(this))
                    .addNextStep(new GatherTestInformation(this, true, new BuildShouldPass(), true))
                    .addNextStep(new CommitPatch(this, CommitType.COMMIT_HUMAN_PATCH));

            this.finalStep = new ComputeSourceDir(this, false, true); // this step is used to compute code metrics on the project
            
            this.finalStep.
                    addNextStep(new ComputeModules(this, false)).
                    addNextStep(new WritePropertyFile(this)).
                    addNextStep(new CommitProcessEnd(this)).
                    addNextStep(new GitRepositoryPushProcessEnd(this));

            cloneRepo.setDataSerializer(this.serializers);
            cloneRepo.setNotifiers(this.notifiers);

            this.printPipeline();

            try {
                cloneRepo.execute();
            } catch (Exception e) {
                this.jobStatus.addStepError("Unknown", e.getMessage());
                this.logger.error("Exception catch while executing steps: ", e);
                this.jobStatus.setFatalError(e);

                ErrorNotifier errorNotifier = ErrorNotifier.getInstance();
                if (errorNotifier != null) {
                    errorNotifier.observe(this);
                }

                for (AbstractDataSerializer serializer : this.serializers) {
                    serializer.serialize();
                }
            }
        } else {
            this.logger.debug("Build " + this.getBuggyBuild().getId() + " is not a failing build.");
        }
    }
}
