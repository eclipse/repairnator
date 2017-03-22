package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;

import java.io.File;
import java.io.IOException;

/**
 * Created by urli on 21/03/2017.
 */
public class SquashRepository extends AbstractStep {
    private static final String NB_COMMITS_TO_KEEP = "10";
    private static final String ROLLBACK_BRANCH = "repair_rollback";
    private static final String SQUASH_BRANCH = "squash_branch";


    public SquashRepository(ProjectInspector inspector) {
        super(inspector);
    }

    private boolean isRepositoryContainEnoughCommits(Git git) {
        try {
            ObjectId id = git.getRepository().resolve("HEAD~" + NB_COMMITS_TO_KEEP);
            return (id != null);
        } catch (IOException e) {
            this.addStepError("Error while getting the "+NB_COMMITS_TO_KEEP+"th commits", e);
            return false;
        }
    }

    private boolean squashRepository() {
        this.getLogger().debug("Apply squash on " + NB_COMMITS_TO_KEEP + " commits");
        ProcessBuilder processBuilder = new ProcessBuilder("git", "rebase-last-x", NB_COMMITS_TO_KEEP)
                .directory(new File(this.inspector.getRepoLocalPath())).inheritIO();

        try {
            Process p = processBuilder.start();
            p.waitFor();
            return true;
        } catch (IOException | InterruptedException e) {
            this.addStepError("There was an error during the squash operation", e);
            return false;
        }
    }

    private boolean createBranches(Git git) {
        try {
            this.getLogger().debug("Create rollback and squash branches");
            git.checkout().setCreateBranch(true).setName(ROLLBACK_BRANCH).call();
            git.checkout().setCreateBranch(true).setName(SQUASH_BRANCH).call();
            return true;
        } catch (GitAPIException e) {
            this.addStepError("Error while creating branches ", e);
            return false;
        }
    }

    private boolean rollbackRepo(Git git) {
        try {
            git.checkout().setName(ROLLBACK_BRANCH).call();
            return true;
        } catch (GitAPIException e) {
            this.addStepError("Error while rollbacking the repository");
            return false;
        }
    }

    @Override
    protected void businessExecute() {
        if (this.getConfig().isPush()) {
            Git git;
            try {
                git = Git.open(new File(inspector.getRepoLocalPath()));
            } catch (IOException e) {
                this.addStepError("Error while opening git repository", e);
                this.shouldStop = true;
                this.setState(ProjectState.NOT_SQUASHED_REPO);
                return;
            }

            this.getLogger().debug("Commit the logs and properties files");
            GitHelper.addAndCommitRepairnatorLogAndProperties(git, "Commit done before squashing repository.");

            boolean shouldBeSquashed = this.isRepositoryContainEnoughCommits(git);

            boolean branchesCreated = this.createBranches(git);

            if (branchesCreated) {
                this.getLogger().debug("Branches are not created, the repository won't be squashed.");
                this.setState(ProjectState.NOT_SQUASHED_REPO);
                return;
            }

            if (shouldBeSquashed) {
                boolean squashResult = this.squashRepository();
                if (squashResult) {
                    this.setState(ProjectState.SQUASHED_REPO);
                } else {
                    this.setState(ProjectState.NOT_SQUASHED_REPO);

                    if (!rollbackRepo(git)) {
                        this.shouldStop = true;
                    }
                }
            } else {
                this.getLogger().debug("The repository contains less than " + NB_COMMITS_TO_KEEP + ": push all the repo.");
            }
        }
    }
}
