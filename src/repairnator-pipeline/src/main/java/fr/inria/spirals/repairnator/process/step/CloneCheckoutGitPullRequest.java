package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.process.inspectors.GitRepositoryProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.states.PipelineState;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RefSpec;

import java.io.File;

public class CloneCheckoutGitPullRequest extends AbstractStep {

    public CloneCheckoutGitPullRequest(ProjectInspector inspector) {
        super(inspector, true);
    }

    @Override
    protected StepStatus businessExecute() {
        GitRepositoryProjectInspector githubInspector;

        try {
            githubInspector = (GitRepositoryProjectInspector) getInspector();
        } catch (Exception ex){
            this.addStepError("CloneCheckoutBranchRepository only supports GitHub commit references");
            return StepStatus.buildError(this, PipelineState.NOTCLONABLE);
        }

        if (githubInspector.getGitRepositoryPullRequest() == null) {
            this.addStepError("CloneCheckoutBranchRepository requires a pull request identifier.");
            return StepStatus.buildError(this, PipelineState.BUILDNOTCHECKEDOUT);
        }

        String repoUrl = githubInspector.getGitRepositoryUrl() + ".git";

        String repoLocalPath = githubInspector.getRepoLocalPath();
        try {
            this.getLogger().info("Cloning repository " + repoUrl + " in the following directory: " + repoLocalPath);

            // Clone repository
            CloneCommand cloneRepositoryCommand = Git.cloneRepository()
                    .setCloneSubmodules(true)
                    .setURI(repoUrl)
                    .setDirectory(new File(repoLocalPath));
            Git git = cloneRepositoryCommand.call();

            // Fetch the pull request according to
            // https://docs.github.com/en/github/collaborating-with-pull-requests/reviewing-changes-in-pull-requests/checking-out-pull-requests-locally
            String branch = githubInspector.getGitRepositoryPullRequest() + "@" + System.currentTimeMillis();
            FetchCommand fetchCommand = git.fetch()
                    .setForceUpdate(true)
                    .setRemote(repoUrl)
                    .setRefSpecs(new RefSpec("refs/pull/" + githubInspector.getGitRepositoryPullRequest() + "/head" + ":" + branch));
            fetchCommand.call();

            // Checkout the pull request branch
            CheckoutCommand checkoutCommand = git.checkout().setName(branch);
            checkoutCommand.call();

            return StepStatus.buildSuccess(this);
        } catch (Exception e) {
            this.addStepError("Repository " + repoUrl + " cannot be cloned.", e);
            return StepStatus.buildError(this, PipelineState.NOTCLONABLE);
        }
    }
}