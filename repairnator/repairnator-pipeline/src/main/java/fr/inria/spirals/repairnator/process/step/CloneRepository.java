package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import org.eclipse.jgit.api.Git;

import java.io.File;

/**
 * Created by urli on 03/01/2017.
 */
public class CloneRepository extends AbstractStep {
    public static final String GITHUB_ROOT_REPO = "https://github.com/";
    private static final String GITHUB_PATCH_ACCEPT = "application/vnd.github.v3.patch";

    protected Build build;

    public CloneRepository(ProjectInspector inspector) {
        super(inspector);
        this.build = inspector.getBuild();
    }

    protected void businessExecute() {
        String repository = this.build.getRepository().getSlug();
        String repoRemotePath = GITHUB_ROOT_REPO + repository + ".git";
        String repoLocalPath = this.inspector.getRepoLocalPath();

        try {
            this.getLogger().debug("Cloning repository " + repository + " in the following directory: " + repoLocalPath);

            Git.cloneRepository().setURI(repoRemotePath).setDirectory(new File(repoLocalPath)).call();

            this.writeProperty("workspace", this.inspector.getWorkspace());
            this.writeProperty("buildid", this.build.getId());
            this.writeProperty("repo", this.build.getRepository().getSlug());

            this.setState(ProjectState.CLONABLE);
        } catch (Exception e) {
            this.getLogger().warn("Repository " + repository + " cannot be cloned.");
            this.getLogger().debug(e.toString());
            this.addStepError(e.getMessage());
            this.setState(ProjectState.NOTCLONABLE);
            this.shouldStop = true;
        }
    }

    protected void cleanMavenArtifacts() {
    }

}
