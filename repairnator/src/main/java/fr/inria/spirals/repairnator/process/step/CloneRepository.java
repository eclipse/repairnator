package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.PRInformation;
import fr.inria.spirals.repairnator.Launcher;
import fr.inria.spirals.repairnator.process.ProjectInspector;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
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

    protected void businessExecute() {
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
                PRInformation prInformation = this.build.getPRInformation();

                Launcher.LOGGER.debug("Reproduce the PR for "+repository+" by fetching remote branch and merging.");
                String remoteBranchPath = GITHUB_ROOT_REPO+prInformation.getOtherRepo().getSlug()+".git";

                RemoteAddCommand remoteBranchCommand = git.remoteAdd();
                remoteBranchCommand.setName("PR");
                remoteBranchCommand.setUri(new URIish(remoteBranchPath));
                remoteBranchCommand.call();

                git.fetch().setRemote("PR").call();

                String commitHeadSha = prInformation.getHead().getSha();
                String commitBaseSha = prInformation.getBase().getSha();


                ObjectId commitHeadId = git.getRepository().resolve(commitHeadSha);
                ObjectId commitBaseId = git.getRepository().resolve(commitBaseSha);

                if (commitHeadId == null) {
                    Launcher.LOGGER.warn("Commit head ref cannot be retrieved in the repository: "+commitHeadSha+". Operation aborted.");
                    Launcher.LOGGER.debug(prInformation.getHead());
                    this.shouldStop = true;
                    return;
                }

                if (commitBaseId == null) {
                    Launcher.LOGGER.warn("Commit base ref cannot be retrieved in the repository: "+commitBaseSha+". Operation aborted.");
                    Launcher.LOGGER.debug(prInformation.getBase());
                    this.shouldStop = true;
                    return;
                }

                Launcher.LOGGER.debug("Get the commit "+commitHeadSha+" for repo "+repository);
                git.checkout().setName(commitHeadSha).call();

                RevWalk revwalk = new RevWalk(git.getRepository());
                RevCommit revCommitBase = revwalk.lookupCommit(commitBaseId);

                Launcher.LOGGER.debug("Do the merge with the PR commit for repo "+repository);
                git.merge().include(revCommitBase).setFastForward(MergeCommand.FastForwardMode.NO_FF).call();
            } else {
                String commitCheckout = this.build.getCommit().getSha();

                Launcher.LOGGER.debug("Get the commit "+commitCheckout+" for repo "+repository);
                git.checkout().setName(commitCheckout).call();
            }



        } catch (Exception e) {
            Launcher.LOGGER.warn("Repository "+repository+" cannot be cloned.");
            Launcher.LOGGER.debug(e.toString());
            this.addStepError(e.getMessage());
            this.shouldStop = true;
            return;
        }

        this.state = ProjectState.CLONABLE;
    }
}
