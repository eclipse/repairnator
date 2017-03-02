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
public class CheckoutPreviousBuild extends CloneRepository {

    public CheckoutPreviousBuild(ProjectInspector inspector) {
        super(inspector);
    }

    protected void businessExecute() {
        this.getLogger().debug("Checking out previous build...");

        this.inspector.setPreviousBuildFlag(true);

        Git git;
        try {
            git = Git.open(new File(inspector.getRepoLocalPath()));
            Build previousBuild = inspector.getPreviousBuild();
            String commitCheckout = previousBuild.getCommit().getSha();
            commitCheckout = GitHelper.testCommitExistence(git, commitCheckout, this, previousBuild);
            if (commitCheckout != null) {
                this.getLogger().debug("Get the commit " + commitCheckout + " for repo " + this.inspector.getRepoSlug());
                git.checkout().setName(commitCheckout).call();
                this.state = ProjectState.PREVIOUSBUILDCHECKEDOUT;
            } else {
                this.addStepError("Error while getting the commit to checkout from the repo.");
                this.shouldStop = true;
                this.state = ProjectState.PREVIOUSBUILDNOTCHECKEDOUT;
                return;
            }
        } catch (IOException | GitAPIException e) {
            this.addStepError("Error while getting the commit to checkout from the repo.");
            this.shouldStop = true;
        }
    }

}
