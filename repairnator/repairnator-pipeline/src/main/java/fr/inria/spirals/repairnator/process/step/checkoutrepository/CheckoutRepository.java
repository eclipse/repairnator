package fr.inria.spirals.repairnator.process.step.checkoutrepository;

import fr.inria.jtravis.entities.Build;
import fr.inria.jtravis.entities.PullRequest;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.inspectors.Metrics;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.process.inspectors.metrics4bears.commits.Commit;
import fr.inria.spirals.repairnator.process.inspectors.metrics4bears.Metrics4Bears;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.states.PipelineState;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by fernanda on 02/03/17.
 */
public abstract class CheckoutRepository extends AbstractStep {

    private CheckoutType checkoutType;

    public CheckoutRepository(ProjectInspector inspector, boolean blockingStep) {
        super(inspector, blockingStep);
    }

    public CheckoutRepository(ProjectInspector inspector, boolean blockingStep, String stepName) {
        super(inspector, blockingStep, stepName);
    }

    private String getCommitUrl(String commitId) {
        return "http://github.com/"+this.getInspector().getRepoSlug()+"/commit/"+commitId;
    }

    @Override
    protected StepStatus businessExecute() {

        Metrics metric = this.getInspector().getJobStatus().getMetrics();
        Git git;
        try {
            GitHelper gitHelper = this.getInspector().getGitHelper();
            git = Git.open(new File(this.getInspector().getRepoLocalPath()));
            Build build;
            Metrics4Bears metrics4Bears = this.getInspector().getJobStatus().getMetrics4Bears();
            Commit commit;

            switch (checkoutType) {
                case CHECKOUT_BUGGY_BUILD:
                    build = this.getInspector().getBuggyBuild();
                    metric.setBugCommit(build.getCommit().getSha());
                    metric.setBugCommitUrl(this.getCommitUrl(build.getCommit().getSha()));

                    commit = this.createCommitForMetrics(build);
                    metrics4Bears.getCommits().setBuggyBuild(commit);
                    break;

                case CHECKOUT_BUGGY_BUILD_SOURCE_CODE:
                    build = this.getInspector().getBuggyBuild();
                    metric.setBugCommit(build.getCommit().getSha());
                    metric.setBugCommitUrl(this.getCommitUrl(build.getCommit().getSha()));
                    metric.setReconstructedBugCommit(true);

                    commit = this.createCommitForMetrics(build);
                    metrics4Bears.getCommits().setBuggyBuild(commit);
                    break;

                case CHECKOUT_PATCHED_BUILD:
                    build = this.getInspector().getPatchedBuild();
                    metric.setPatchCommit(build.getCommit().getSha());
                    metric.setPatchCommitUrl(this.getCommitUrl(build.getCommit().getSha()));

                    commit = this.createCommitForMetrics(build);
                    metrics4Bears.getCommits().setFixerBuild(commit);
                    break;

                default:
                    this.getLogger().warn("A case seems not to have been considered. Buggy build will be used.");
                    build = this.getInspector().getBuggyBuild();
            }

            if (build.isPullRequest()) {
                Optional<PullRequest> obsPrInformation = RepairnatorConfig.getInstance().getJTravis().pullRequest().fromBuild(build);

                if (obsPrInformation.isPresent()) {
                    PullRequest prInformation = obsPrInformation.get();
                    if (checkoutType == CheckoutType.CHECKOUT_PATCHED_BUILD) {
                        this.writeProperty("is-pr", "true");
                        this.writeProperty("pr-remote-repo", prInformation.getOtherRepo().getFullName());
                        this.writeProperty("pr-head-commit-id", prInformation.getHead().getSHA1());
                        this.writeProperty("pr-head-commit-id-url", prInformation.getHead().getHtmlUrl());
                        this.writeProperty("pr-base-commit-id", prInformation.getBase().getSHA1());
                        this.writeProperty("pr-base-commit-id-url", prInformation.getBase().getHtmlUrl());
                        this.writeProperty("pr-id", build.getPullRequestNumber());

                        commit = this.createCommitForMetrics(build, prInformation, false);
                        metrics4Bears.getCommits().setFixerBuildForkRepo(commit);
                        commit = this.createCommitForMetrics(build, prInformation, true);
                        metrics4Bears.getCommits().setFixerBuildBaseRepo(commit);
                    } else if (checkoutType == CheckoutType.CHECKOUT_BUGGY_BUILD ||
                            checkoutType == CheckoutType.CHECKOUT_BUGGY_BUILD_SOURCE_CODE) {
                        commit = this.createCommitForMetrics(build, prInformation, false);
                        metrics4Bears.getCommits().setBuggyBuildForkRepo(commit);
                        commit = this.createCommitForMetrics(build, prInformation, true);
                        metrics4Bears.getCommits().setBuggyBuildBaseRepo(commit);
                    }

                    gitHelper.addAndCommitRepairnatorLogAndProperties(this.getInspector().getJobStatus(), git, "After getting PR information");

                    String repository = this.getInspector().getRepoSlug();
                    this.getLogger().debug("Reproduce the PR for " + repository + " by fetching remote branch and merging.");

                    List<String> paths;
                    if (checkoutType == CheckoutType.CHECKOUT_BUGGY_BUILD_SOURCE_CODE) {
                        paths = this.getPaths(this.getInspector().getJobStatus().getRepairSourceDir(), git);
                    } else {
                        paths = null;
                    }

                    boolean successfulMerge = gitHelper.mergeTwoCommitsForPR(git, build, prInformation, repository, this, paths);
                    if (!successfulMerge) {
                        this.getLogger().debug("Error while merging two commits to reproduce the PR.");
                        return StepStatus.buildError(this, PipelineState.BUILDNOTCHECKEDOUT);
                    }

                } else {
                    this.addStepError("Error while getting the PR information...");
                    return StepStatus.buildError(this, PipelineState.BUILDNOTCHECKEDOUT);
                }
            } else {
                String commitCheckout = build.getCommit().getSha();
                commitCheckout = gitHelper.testCommitExistence(git, commitCheckout, this, build);
                if (commitCheckout != null) {
                    this.getLogger().debug("Get the commit " + commitCheckout + " for repo " + this.getInspector().getRepoSlug());
                    if (checkoutType != CheckoutType.CHECKOUT_BUGGY_BUILD_SOURCE_CODE) {
                        git.checkout().setName(commitCheckout).call();
                    } else {
                        List<String> paths = this.getPaths(this.getInspector().getJobStatus().getRepairSourceDir(), git);

                        git.checkout().setStartPoint(commitCheckout).addPaths(paths).call();

                        // FIXME: commit should not be there
                        PersonIdent personIdent = new PersonIdent("Luc Esape", "luc.esape@gmail.com");
                        git.commit().setMessage("Undo changes on source code").setAuthor(personIdent).setCommitter(personIdent).call();
                    }
                    this.writeProperty("bugCommit", this.getInspector().getBuggyBuild().getCommit().getCompareUrl());
                } else {
                    this.addStepError("Error while getting the commit to checkout from the repo.");
                    return StepStatus.buildError(this, PipelineState.BUILDNOTCHECKEDOUT);
                }
            }
        } catch (IOException | GitAPIException e) {
            this.addStepError("Exception while getting the commit to checkout from the repo.", e);
            return StepStatus.buildError(this, PipelineState.BUILDNOTCHECKEDOUT);
        }

        return StepStatus.buildSuccess(this);
    }

    private List<String> getPaths(File[] dir, Git git) {
        List<String> paths = new ArrayList<>();
        try {
            for (File path : dir) {
                URI gitRepoURI = git.getRepository().getDirectory().getParentFile().toURI();
                URI pathURI = path.getCanonicalFile().toURI();
                String relativePath = gitRepoURI.relativize(pathURI).getPath();

                paths.add(relativePath);
            }
        } catch (IOException e) {
            this.addStepError("Exception while getting paths.", e);
        }
        return paths;
    }

    private Commit createCommitForMetrics(Build build) {
        Commit commit = new Commit();
        commit.setRepoName(build.getRepository().getName());
        commit.setBranchName(build.getBranch().getName());
        commit.setSha(build.getCommit().getSha());
        commit.setUrl(this.getCommitUrl(build.getCommit().getSha()));
        commit.setDate(build.getCommit().getCommittedAt());
        return commit;
    }

    private Commit createCommitForMetrics(Build build, PullRequest prInformation, boolean base) {
        Commit commit = new Commit();
        commit.setRepoName(prInformation.getOtherRepo().getFullName());
        commit.setBranchName(build.getBranch().getName());
        commit.setDate(build.getCommit().getCommittedAt());
        if (base) {
            commit.setSha(prInformation.getBase().getSHA1());
            commit.setUrl(prInformation.getBase().getHtmlUrl().toString());
        } else {
            commit.setSha(prInformation.getHead().getSHA1());
            commit.setUrl(prInformation.getHead().getHtmlUrl().toString());
        }
        return commit;
    }

    protected CheckoutType getCheckoutType() {
        return this.checkoutType;
    }

    protected void setCheckoutType(CheckoutType checkoutType) {
        this.checkoutType = checkoutType;
    }

}
