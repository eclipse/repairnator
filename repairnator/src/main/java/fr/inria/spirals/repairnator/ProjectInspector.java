package fr.inria.spirals.repairnator;

import fr.inria.spirals.jtravis.entities.Build;
import org.apache.maven.cli.MavenCli;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.errors.MissingObjectException;

import java.io.File;

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

            Launcher.LOGGER.debug("Get the commit "+this.build.getCommit().getSha()+" for repo "+repository);
            git.checkout().setName(this.build.getCommit().getSha()).call();
        } catch (GitAPIException e) {
            Launcher.LOGGER.warn("Repository "+repository+" cannot be cloned.");
            Launcher.LOGGER.debug(e.getMessage());
            this.canBeCloned = false;
        } catch (JGitInternalException internalException) {
            Launcher.LOGGER.warn("Repository "+repository+" cannot be cloned.");
            Launcher.LOGGER.debug(internalException.getMessage());
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
