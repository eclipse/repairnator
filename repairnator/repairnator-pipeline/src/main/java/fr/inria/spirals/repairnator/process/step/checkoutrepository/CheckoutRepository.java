package fr.inria.spirals.repairnator.process.step.checkoutrepository;

import fr.inria.jtravis.entities.Build;
import fr.inria.jtravis.entities.PRInformation;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.inspectors.Metrics;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by fernanda on 02/03/17.
 */
public abstract class CheckoutRepository extends AbstractStep {

    private CheckoutType checkoutType;

    public CheckoutRepository(ProjectInspector inspector) {
        super(inspector);
    }

    private String getCommitUrl(String commitId) {
        return "http://github.com/"+this.inspector.getRepoSlug()+"/commit/"+commitId;
    }

    @Override
    protected void businessExecute() {

        Metrics metric = this.getInspector().getJobStatus().getMetrics();
        Git git;
        try {
            GitHelper gitHelper = this.getInspector().getGitHelper();
            git = Git.open(new File(inspector.getRepoLocalPath()));
            Build build;

            switch (checkoutType) {
                case CHECKOUT_BUGGY_BUILD:
                    build = inspector.getBuggyBuild();
                    metric.setBugCommit(build.getCommit().getSha());
                    metric.setBugCommitUrl(this.getCommitUrl(build.getCommit().getSha()));
                    break;

                case CHECKOUT_BUGGY_BUILD_SOURCE_CODE:
                    build = inspector.getBuggyBuild();
                    metric.setBugCommit(build.getCommit().getSha());
                    metric.setBugCommitUrl(this.getCommitUrl(build.getCommit().getSha()));
                    metric.setReconstructedBugCommit(true);
                    break;

                case CHECKOUT_PATCHED_BUILD:
                    build = inspector.getPatchedBuild();
                    metric.setPatchCommit(build.getCommit().getSha());
                    metric.setPatchCommitUrl(this.getCommitUrl(build.getCommit().getSha()));
                    break;

                default:
                    this.getLogger().warn("A case seems not to have been considered. Buggy build will be used.");
                    build = inspector.getBuggyBuild();
            }

            if (build.isPullRequest()) {
                PRInformation prInformation = build.getPRInformation();

                if (prInformation != null) {
                    if (checkoutType == CheckoutType.CHECKOUT_PATCHED_BUILD) {
                        this.writeProperty("is-pr", "true");
                        this.writeProperty("pr-remote-repo", prInformation.getOtherRepo().getSlug());
                        this.writeProperty("pr-head-commit-id", prInformation.getHead().getSha());
                        this.writeProperty("pr-head-commit-id-url", prInformation.getHead().getCompareUrl());
                        this.writeProperty("pr-base-commit-id", prInformation.getBase().getSha());
                        this.writeProperty("pr-base-commit-id-url", prInformation.getBase().getCompareUrl());
                        this.writeProperty("pr-id", build.getPullRequestNumber());
                    }
                } else {
                    this.addStepError("Error while getting the PR information...");
                    this.shouldStop = true;
                    return;
                }

                gitHelper.addAndCommitRepairnatorLogAndProperties(this.getInspector().getJobStatus(), git, "After getting PR information");

                String repository = this.inspector.getRepoSlug();
                this.getLogger().debug("Reproduce the PR for " + repository + " by fetching remote branch and merging.");

                List<String> pathes;
                if (checkoutType == CheckoutType.CHECKOUT_BUGGY_BUILD_SOURCE_CODE) {
                    pathes = new ArrayList<String>();
                    for (File path : this.getInspector().getJobStatus().getRepairSourceDir()) {
                        URI gitRepoURI = git.getRepository().getDirectory().getParentFile().toURI();
                        URI pathURI = path.getCanonicalFile().toURI();
                        String relativePath = gitRepoURI.relativize(pathURI).getPath();

                        pathes.add(relativePath);
                    }
                } else {
                    pathes = null;
                }

                boolean successfulMerge = gitHelper.mergeTwoCommitsForPR(git, build, prInformation, repository, this, pathes);
                if (!successfulMerge) {
                    this.getLogger().debug("Error while merging two commits to reproduce the PR.");
                    this.shouldStop = true;
                }
            } else {
                String commitCheckout = build.getCommit().getSha();
                commitCheckout = gitHelper.testCommitExistence(git, commitCheckout, this, build);
                if (commitCheckout != null) {
                    this.getLogger().debug("Get the commit " + commitCheckout + " for repo " + this.inspector.getRepoSlug());
                    if (checkoutType != CheckoutType.CHECKOUT_BUGGY_BUILD_SOURCE_CODE) {
                        git.checkout().setName(commitCheckout).call();
                    } else {

                        List<String> pathes = new ArrayList<String>();
                        for (File path : this.getInspector().getJobStatus().getRepairSourceDir()) {
                            URI gitRepoURI = git.getRepository().getDirectory().getParentFile().toURI();
                            URI pathURI = path.getCanonicalFile().toURI();
                            String relativePath = gitRepoURI.relativize(pathURI).getPath();

                            pathes.add(relativePath);
                        }
                        git.checkout().setStartPoint(commitCheckout).addPaths(pathes).call();

                        PersonIdent personIdent = new PersonIdent("Luc Esape", "luc.esape@gmail.com");
                        git.commit().setMessage("Undo changes on source code").setAuthor(personIdent).setCommitter(personIdent).call();
                    }
                    this.writeProperty("bugCommit", this.inspector.getBuggyBuild().getCommit().getCompareUrl());
                } else {
                    this.addStepError("Error while getting the commit to checkout from the repo.");
                    this.shouldStop = true;
                    return;
                }
            }
        } catch (IOException | GitAPIException e) {
            this.addStepError("Error while getting the commit to checkout from the repo.");
            this.shouldStop = true;
        }

        this.writeProperty("hostname", Utils.getHostname());

        switch (this.getInspector().getBuildToBeInspected().getStatus()) {
            case ONLY_FAIL:
                this.writeProperty("bugType", "only_fail");
                break;

            case FAILING_AND_PASSING:
                this.writeProperty("bugType", "failing_passing");
                break;

            case PASSING_AND_PASSING_WITH_TEST_CHANGES:
                this.writeProperty("bugType", "passing_passing");
                break;
        }
    }

    protected CheckoutType getCheckoutType() {
        return this.checkoutType;
    }

    protected void setCheckoutType(CheckoutType checkoutType) {
        this.checkoutType = checkoutType;
    }

}
