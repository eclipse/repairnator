package fr.inria.spirals.repairnator.process.step;

import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.states.PipelineState;
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
        this.build = inspector.getBuggyBuild();
    }

    protected void businessExecute() {
        String repository = this.build.getRepository().getSlug();
        String repoRemotePath = GITHUB_ROOT_REPO + repository + ".git";
        String repoLocalPath = this.inspector.getRepoLocalPath();

        try {
            this.getLogger().debug("Cloning repository " + repository + " in the following directory: " + repoLocalPath);

            Git.cloneRepository().setURI(repoRemotePath).setDirectory(new File(repoLocalPath)).call();

            this.writeProperty("repo",this.inspector.getRepoSlug());
            this.setPipelineState(PipelineState.CLONABLE);
        } catch (Exception e) {
            this.getLogger().warn("Repository " + repository + " cannot be cloned.");
            this.getLogger().debug(e.toString());
            this.addStepError(e.getMessage());
            this.setPipelineState(PipelineState.NOTCLONABLE);
            this.shouldStop = true;
        }
    }

    protected void cleanMavenArtifacts() {
    }

}
