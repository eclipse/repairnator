package fr.inria.spirals.repairnator.process.step.checkoutrepository;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.PRInformation;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.process.git.GitHelper;
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

    @Override
    protected void businessExecute() {
        Git git;
        try {
            GitHelper gitHelper = this.getInspector().getGitHelper();
            git = Git.open(new File(inspector.getRepoLocalPath()));
            Build build;
            if (checkoutType == CheckoutType.CHECKOUT_BUILD) {
                build = inspector.getBuild();
            } else {
                build = inspector.getPreviousBuild();
            }

            if (build.isPullRequest()) {
                PRInformation prInformation = build.getPRInformation();

                if (prInformation != null) {
                    if (checkoutType == CheckoutType.CHECKOUT_BUILD) {
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

                gitHelper.addAndCommitRepairnatorLogAndProperties(git, "After getting PR information");

                String repository = this.inspector.getRepoSlug();
                this.getLogger().debug("Reproduce the PR for " + repository + " by fetching remote branch and merging.");

                List<String> pathes;
                if (checkoutType == CheckoutType.CHECKOUT_PREVIOUS_BUILD_SOURCE_CODE) {
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
                    if (checkoutType != CheckoutType.CHECKOUT_PREVIOUS_BUILD_SOURCE_CODE) {
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
                    this.writeProperty("bugRepo",this.inspector.getRepoSlug());
                    this.writeProperty("bugCommit", this.inspector.getBuild().getCommit().getCompareUrl());
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
        this.writeProperty("nbCPU", Runtime.getRuntime().availableProcessors());
        this.writeProperty("freeMemory", Runtime.getRuntime().freeMemory());
        this.writeProperty("totalMemory", Runtime.getRuntime().totalMemory());

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
