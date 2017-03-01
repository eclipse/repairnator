package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.process.ProjectState;
import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;

/**
 * Created by fermadeiral.
 */
public class CheckoutSourceCodeForPreviousBuild extends CloneRepository {

    public CheckoutSourceCodeForPreviousBuild(ProjectInspector inspector) {
        super(inspector);
    }

    protected void businessExecute() {
        this.getLogger().debug("Checking out the source code folder for previous build...");

        Git git;
        try {
            git = Git.open(new File(inspector.getRepoLocalPath()));
            Build build = inspector.getBuild();
            String commitCheckout = build.getCommit().getSha();
            commitCheckout = GitHelper.testCommitExistence(git, commitCheckout, this, build);
            if (commitCheckout != null) {
                this.getLogger().debug("Get the commit " + commitCheckout + " for repo " + this.inspector.getRepoSlug());
                git.checkout().setName(commitCheckout).call();
                Build previousBuild = inspector.getPreviousBuild();
                commitCheckout = previousBuild.getCommit().getSha();
                commitCheckout = GitHelper.testCommitExistence(git, commitCheckout, this, previousBuild);
                if (commitCheckout != null) {
                    this.getLogger().debug("Get the commit " + commitCheckout + " for repo " + this.inspector.getRepoSlug());
                    git.checkout().setStartPoint(inspector.getPreviousBuild().getCommit().getSha()).addPath("src/main/java").call();
                    this.state = ProjectState.PREVIOUSBUILDCODECHECKEDOUT;
                } else {
                    this.addStepError("Error while getting the commit " + commitCheckout + " to checkout from the repo.");
                    this.shouldStop = true;
                    this.state = ProjectState.PREVIOUSBUILDCODENOTCHECKEDOUT;
                    return;
                }
            } else {
                this.addStepError("Error while getting the commit " + commitCheckout + " to checkout from the repo.");
                this.shouldStop = true;
                this.state = ProjectState.PREVIOUSBUILDCODENOTCHECKEDOUT;
                return;
            }
        } catch (IOException | GitAPIException e) {
            this.addStepError("Error while getting the commit to checkout from the repo.");
            this.shouldStop = true;
            this.state = ProjectState.PREVIOUSBUILDCODENOTCHECKEDOUT;
        }
    }

}
