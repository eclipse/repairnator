package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.PRInformation;
import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;

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
            git = Git.open(new File(inspector.getRepoLocalPath()));
            Build build;
            if (checkoutType == CheckoutType.CHECKOUT_BUILD) {
                build = inspector.getBuild();
            } else {
                build = inspector.getPreviousBuild();
            }

            if (build.isPullRequest()) {
                PRInformation prInformation = build.getPRInformation();

                if (checkoutType == CheckoutType.CHECKOUT_BUILD) {
                    this.writeProperty("is-pr", "true");
                    this.writeProperty("pr-remote-repo", prInformation.getOtherRepo().getSlug());
                    this.writeProperty("pr-head-commit-id", prInformation.getHead().getSha());
                    this.writeProperty("pr-base-commit-id", prInformation.getBase().getSha());
                    this.writeProperty("pr-id", build.getPullRequestNumber() + "");
                }

                GitHelper.addAndCommitRepairnatorLogAndProperties(git, "After getting PR information");

                String repository = this.inspector.getRepoSlug();
                this.getLogger().debug("Reproduce the PR for " + repository + " by fetching remote branch and merging.");

                boolean successfulMerge = GitHelper.mergeTwoCommitsForPR(git, build, prInformation, repository, this);
                if (!successfulMerge) {
                    this.getLogger().debug("Error while merging two commits to reproduce the PR.");
                    this.shouldStop = true;
                }
            } else {
                String commitCheckout = build.getCommit().getSha();
                commitCheckout = GitHelper.testCommitExistence(git, commitCheckout, this, build);
                if (commitCheckout != null) {
                    this.getLogger().debug("Get the commit " + commitCheckout + " for repo " + this.inspector.getRepoSlug());
                    if (checkoutType != CheckoutType.CHECKOUT_PREVIOUS_BUILD_SOURCE_CODE) {
                        git.checkout().setName(commitCheckout).call();
                    } else {
                        git.checkout().setStartPoint(commitCheckout).addPath("src/main/java").call();
                    }
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
    }

    protected void setCheckoutType(CheckoutType checkoutType) {
        this.checkoutType = checkoutType;
    }

}
