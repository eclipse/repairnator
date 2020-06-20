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
import fr.inria.spirals.repairnator.process.step.push.CommitType;
import fr.inria.spirals.repairnator.process.step.push.GitRepositoryCommitPatch;
import fr.inria.spirals.repairnator.process.step.push.GitRepositoryCommitProcessEnd;
import fr.inria.spirals.repairnator.process.step.push.GitRepositoryInitRepoToPush;
import fr.inria.spirals.repairnator.process.step.push.GitRepositoryPushProcessEnd;
import fr.inria.spirals.repairnator.process.step.repair.AbstractRepairStep;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class initialize the pipelines by creating the steps when Repairnator
 * is executed in GIT_REPOSITORY launcher mode: it's the backbone of the pipeline.
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
        this.repoLocalPath = workspace + File.separator + getProjectIdToBeInspected() + "_repo";

        this.repoToPushLocalPath = repoLocalPath+"_topush";
        this.m2LocalPath = new File(this.repoLocalPath + File.separator + ".m2").getAbsolutePath();
        this.serializers = serializers;
        this.gitHelper = new GitHelper();
        this.jobStatus = new JobStatus(repoLocalPath);
        this.notifiers = notifiers;
        this.checkoutType = CheckoutType.NO_CHECKOUT;
        this.steps = new ArrayList<>();
    }
    
    public GitRepositoryProjectInspector(String gitRepoUrl, String gitRepoBranch, String gitRepoIdCommit, boolean isGitRepositoryFirstCommit,
            String workspace, List<AbstractNotifier> notifiers) {

        this.gitRepositoryUrl = gitRepoUrl;
        this.gitRepositoryBranch = gitRepoBranch;
        this.gitRepositoryIdCommit = gitRepoIdCommit;
        this.gitRepositoryFirstCommit = isGitRepositoryFirstCommit;

        this.gitSlug = this.gitRepositoryUrl.split("https://github.com/",2)[1].replace("/", "-");
        this.workspace = workspace;
        this.repoLocalPath = workspace + File.separator + getProjectIdToBeInspected() + "_repo";

        this.repoToPushLocalPath = repoLocalPath+"_topush";
        this.m2LocalPath = new File(this.repoLocalPath + File.separator + ".m2").getAbsolutePath();
        this.serializers = new ArrayList<AbstractDataSerializer>();

        this.gitHelper = new GitHelper();
        this.jobStatus = new JobStatus(repoLocalPath);
        this.notifiers = notifiers;
        this.checkoutType = CheckoutType.NO_CHECKOUT;
        this.steps = new ArrayList<>();
    }


    @Override
    public String getRepoSlug() {
        return this.gitRepositoryUrl.split("https://github.com/",2)[1];
    }

    public String getGitRepositoryUrl() {
    	return this.gitRepositoryUrl;
    }

    @Override
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

}
