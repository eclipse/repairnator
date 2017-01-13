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
            this.getLogger().debug("Cloning repository "+repository+" has in the following directory: "+repoLocalPath);
            Git git = Git.cloneRepository()
                    .setURI(repoRemotePath)
                    .setDirectory(new File(repoLocalPath))
                    .call();

            if (this.build.isPullRequest()) {
                PRInformation prInformation = this.build.getPRInformation();

                this.getLogger().debug("Reproduce the PR for "+repository+" by fetching remote branch and merging.");
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
                    this.getLogger().warn("Commit head ref cannot be retrieved in the repository: "+commitHeadSha+". Operation aborted.");
                    this.getLogger().debug(prInformation.getHead().toString());
                    this.shouldStop = true;
                    return;
                }

                if (commitBaseId == null) {
                    this.getLogger().warn("Commit base ref cannot be retrieved in the repository: "+commitBaseSha+". Operation aborted.");
                    this.getLogger().debug(prInformation.getBase().toString());
                    this.shouldStop = true;
                    return;
                }

                this.getLogger().debug("Get the commit "+commitHeadSha+" for repo "+repository);
                git.checkout().setName(commitHeadSha).call();

                RevWalk revwalk = new RevWalk(git.getRepository());
                RevCommit revCommitBase = revwalk.lookupCommit(commitBaseId);

                this.getLogger().debug("Do the merge with the PR commit for repo "+repository);
                git.merge().include(revCommitBase).setFastForward(MergeCommand.FastForwardMode.NO_FF).call();
            } else {
                String commitCheckout = this.build.getCommit().getSha();

                this.getLogger().debug("Get the commit "+commitCheckout+" for repo "+repository);
                git.checkout().setName(commitCheckout).call();
            }



        } catch (Exception e) {
            this.getLogger().warn("Repository "+repository+" cannot be cloned.");
            this.getLogger().debug(e.toString());
            this.addStepError(e.getMessage());
            this.shouldStop = true;
            return;
        }

        this.state = ProjectState.CLONABLE;
    }

    protected void cleanMavenArtifacts() {}
}
