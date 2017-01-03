package fr.inria.spirals.repairnator.step;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.Launcher;
import fr.inria.spirals.repairnator.ProjectInspector;
import fr.inria.spirals.repairnator.ProjectState;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.URIish;

import java.io.File;

/**
 * Created by urli on 03/01/2017.
 */
public class CloneRepository extends AbstractStep {
    public static final String GITHUB_ROOT_REPO = "https://github.com/";

    private Build build;

    public CloneRepository(ProjectInspector inspector) {
        super(inspector);
        this.build = inspector.getBuild();
    }

    public void execute() {
        String repository = this.inspector.getRepoSlug();
        String repoRemotePath = GITHUB_ROOT_REPO+repository+".git";
        String repoLocalPath = this.inspector.getRepoLocalPath();

        // start cloning
        try {
            Launcher.LOGGER.debug("Cloning repository "+repository+" has in the following directory: "+repoLocalPath);
            Git git = Git.cloneRepository()
                    .setURI(repoRemotePath)
                    .setDirectory(new File(repoLocalPath))
                    .call();

            if (this.build.isPullRequest()) {
                Launcher.LOGGER.debug("Reproduce the PR for "+repository+" by fetching remote branch and merging.");
                String remoteBranchPath = GITHUB_ROOT_REPO+build.getPRRepository().getSlug()+".git";

                RemoteAddCommand remoteBranchCommand = git.remoteAdd();
                remoteBranchCommand.setName("PR");
                remoteBranchCommand.setUri(new URIish(remoteBranchPath));
                remoteBranchCommand.call();

                git.fetch().setRemote("PR").call();

                String commitHeadSha = this.build.getHeadCommit().getSha();
                String commitBaseSha = this.build.getBaseCommit().getSha();


                ObjectId commitHeadId = git.getRepository().resolve(commitHeadSha);
                ObjectId commitBaseId = git.getRepository().resolve(commitBaseSha);

                if (commitHeadId == null) {
                    Launcher.LOGGER.warn("Commit head ref cannot be retrieved in the repository: "+commitHeadSha+". Operation aborted.");
                    Launcher.LOGGER.debug(this.build.getHeadCommit());
                    return;
                }

                if (commitBaseId == null) {
                    Launcher.LOGGER.warn("Commit base ref cannot be retrieved in the repository: "+commitBaseSha+". Operation aborted.");
                    Launcher.LOGGER.debug(this.build.getBaseCommit());
                    return;
                }

                Launcher.LOGGER.debug("Get the commit "+commitHeadSha+" for repo "+repository);
                git.checkout().setName(commitHeadSha).call();

                RevWalk revwalk = new RevWalk(git.getRepository());
                RevCommit revCommitBase = revwalk.lookupCommit(commitBaseId);

                Launcher.LOGGER.debug("Do the merge with the PR commit for repo "+repository);
                git.merge().include(revCommitBase).call();
            } else {
                String commitCheckout = this.build.getCommit().getSha();

                Launcher.LOGGER.debug("Get the commit "+commitCheckout+" for repo "+repository);
                git.checkout().setName(commitCheckout).call();
            }



        } catch (Exception e) {
            Launcher.LOGGER.warn("Repository "+repository+" cannot be cloned.");
            Launcher.LOGGER.debug(e.toString());
            return;
        }

        this.state = ProjectState.CLONABLE;
        this.executeNextStep();
    }
}
