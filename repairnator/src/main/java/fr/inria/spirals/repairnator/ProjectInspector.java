package fr.inria.spirals.repairnator;

import fr.inria.spirals.jtravis.entities.Build;
import org.apache.maven.cli.MavenCli;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.URIish;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Created by urli on 26/12/2016.
 */
public class ProjectInspector {
    public static final String GITHUB_ROOT_REPO = "https://github.com/";

    private Build build;
    private String repoLocalPath;
    private boolean canBeBuilt;
    private boolean canBeCloned;

    public ProjectInspector(Build failingBuild) {
        this.build = failingBuild;
        this.canBeBuilt = false;
        this.canBeCloned = true;
    }

    public boolean canBeBuilt() {
        return this.canBeBuilt;
    }

    public Build getBuild() {
        return this.build;
    }

    public boolean canBeCloned() {
        return this.canBeCloned;
    }

    public void cloneInWorkspace(String workspace) {
        String repository = this.build.getRepository().getSlug();
        this.repoLocalPath = workspace+File.separator+repository;
        String repoRemotePath = GITHUB_ROOT_REPO+repository+".git";

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

                String commitCheckout = this.build.getHeadCommit().getSha();

                Launcher.LOGGER.debug("Get the commit "+commitCheckout+" for repo "+repository);
                git.checkout().setName(commitCheckout).call();

                Launcher.LOGGER.debug("Do the merge with the PR commit for repo "+repository);
                Ref prCommitRef = git.getRepository().exactRef(this.build.getCommit().getSha());
                git.merge().include(prCommitRef).call();
            } else {
                String commitCheckout = this.build.getCommit().getSha();

                Launcher.LOGGER.debug("Get the commit "+commitCheckout+" for repo "+repository);
                git.checkout().setName(commitCheckout).call();
            }



        } catch (Exception e) {
            Launcher.LOGGER.warn("Repository "+repository+" cannot be cloned.");
            Launcher.LOGGER.debug(e.getMessage());
            this.canBeCloned = false;
        }

        MavenCli cli = new MavenCli();

        System.setProperty("maven.test.skip","true");
        int result = cli.doMain(new String[]{"test"},
                repoLocalPath,
                System.out, System.err);

        if (result == 0) {
            this.canBeBuilt = true;
        } else {
            Launcher.LOGGER.info("Repository "+repository+" cannot be built. It will be ignored for the following steps.");
        }
        System.setProperty("maven.test.skip","false");
    }
}
