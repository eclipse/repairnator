package fr.inria.spirals.repairnator.process.step;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.BuildStatus;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;

/**
 * Created by fermadeiral.
 */
public class CheckoutPreviousBuild extends CloneRepository {

	public CheckoutPreviousBuild(ProjectInspector inspector) {
		super(inspector);
	}

	protected void businessExecute() {
		this.getLogger().debug("Testing previous build...");

		this.inspector.setPreviousBuildFlag(true);

		Build previousBuild = BuildHelper.getLastBuildOfSameBranchOfStatusBeforeBuild(this.inspector.getBuild(), null);

		if (previousBuild != null) {
			this.inspector.setPreviousBuild(previousBuild);

			this.getLogger().debug("Build: " + this.inspector.getBuild().getId());
			this.getLogger().debug("Previous build: " + previousBuild.getId());

			if (previousBuild.getBuildStatus() == BuildStatus.FAILED) {
				this.shouldStop = false;

				Git git;
				try {
					git = Git.open(new File(inspector.getRepoLocalPath()));
					String commitCheckout = previousBuild.getCommit().getSha();
					commitCheckout = this.testCommitExistence(git, commitCheckout);
					if (commitCheckout != null) {
						this.getLogger().debug(
								"Get the commit " + commitCheckout + " for repo " + this.inspector.getRepoSlug());
						git.checkout().setName(commitCheckout).call();
					} else {
						this.addStepError("Error while getting the commit to checkout from the repo.");
						this.shouldStop = true;
						return;
					}
				} catch (IOException | GitAPIException e) {
					e.printStackTrace();
				}
			} else {
				this.shouldStop = true;
			}
		} else {
			this.shouldStop = true;
		}
	}

}