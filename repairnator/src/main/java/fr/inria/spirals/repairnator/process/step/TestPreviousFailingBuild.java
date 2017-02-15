package fr.inria.spirals.repairnator.process.step;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;

/**
 * Created by fermadeiral.
 */
public class TestPreviousFailingBuild extends CloneRepository {

	private Build previousFailingBuild;
	
	public TestPreviousFailingBuild(ProjectInspector inspector, Build previousFailingBuild) {
		super(inspector);
		this.previousFailingBuild = previousFailingBuild;
	}

	protected void businessExecute() {
		this.getLogger().debug("Testing previous failing build...");
		
		Git git;
		try {
			git = Git.open(new File(inspector.getRepoLocalPath()));
			String commitCheckout = previousFailingBuild.getCommit().getSha();
	        commitCheckout = this.testCommitExistence(git, commitCheckout);
	        if (commitCheckout != null) {
	            this.getLogger().debug("Get the commit "+commitCheckout+" for repo "+this.inspector.getRepoSlug());
	            git.checkout().setName(commitCheckout).call();
	        } else {
	            this.addStepError("Error while getting the commit to checkout from the repo.");
	            this.shouldStop = true;
	            return;
	        }
		} catch (IOException | GitAPIException e) {
			e.printStackTrace();
		}
	}
}