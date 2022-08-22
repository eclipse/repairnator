package fr.inria.spirals.repairnator.process.inspectors;

import fr.inria.spirals.repairnator.GithubInputBuild;
import fr.inria.spirals.repairnator.notifier.AbstractNotifier;
import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutType;
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

    private GithubInputBuild inputBuild;
    private boolean gitRepositoryFirstCommit;
    
    public GitRepositoryProjectInspector(GithubInputBuild inputBuild, boolean isGitRepositoryFirstCommit,
    		String workspace, List<AbstractDataSerializer> serializers, List<AbstractNotifier> notifiers) {

    	this.inputBuild = inputBuild;
        this.gitRepositoryFirstCommit = isGitRepositoryFirstCommit;

        this.gitSlug = this.inputBuild.getSlug().replace("/", "-");
        this.workspace = workspace;
        this.repoLocalPath = workspace + File.separator + getProjectIdToBeInspected() + "_" + System.currentTimeMillis() + "_repo";

        this.repoToPushLocalPath = repoLocalPath+"_topush";
        this.m2LocalPath = new File(this.repoLocalPath + File.separator + ".m2").getAbsolutePath();
        this.serializers = serializers;
        this.gitHelper = new GitHelper();
        this.jobStatus = new JobStatus(repoLocalPath);
        this.notifiers = notifiers;
        this.checkoutType = CheckoutType.NO_CHECKOUT;
        this.steps = new ArrayList<>();
        this.buildLog = new ArrayList<>();
    }
    
    public GitRepositoryProjectInspector(GithubInputBuild inputBuild, boolean isGitRepositoryFirstCommit,
                                         String workspace, List<AbstractNotifier> notifiers) {

        this.inputBuild = inputBuild;
        this.gitRepositoryFirstCommit = isGitRepositoryFirstCommit;

        this.gitSlug = this.inputBuild.getSlug().replace("/", "-");
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
        this.buildLog = new ArrayList<>();
    }


    @Override
    public String getRepoSlug() {
        return this.inputBuild.getSlug();
    }

    public String getGitRepositoryUrl() {
    	return this.inputBuild.getUrl();
    }

    @Override
    public String getGitRepositoryBranch() {
    	return this.inputBuild.getBranch();
    }

    public String getGitRepositoryIdCommit() {
    	return this.inputBuild.getSha();
    }

    public boolean isGitRepositoryFirstCommit() {
    	return this.gitRepositoryFirstCommit;
    }

    public Integer getGitRepositoryPullRequest() {
        return this.inputBuild.getPr();
    }
    
    @Override
    public String getProjectIdToBeInspected() {
    	return getGitSlug() + "-" + (getGitRepositoryBranch() != null ? getGitRepositoryBranch() : "master") +
				(getGitRepositoryIdCommit() != null ? "-" + getGitRepositoryIdCommit() : "") +
				(isGitRepositoryFirstCommit() ? "-firstCommit" : "") +
                (getGitRepositoryPullRequest() != null ? "-" + getGitRepositoryPullRequest() : "");
    }

}
