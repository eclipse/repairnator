package fr.inria.spirals.repairnator.process.step;

import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import org.eclipse.jgit.api.Git;

import java.io.File;

/**
 * Created by urli on 03/01/2017.
 */
public class CloneRepository extends AbstractStep {
    public static final String GITHUB_ROOT_REPO = "https://github.com/";

    protected Build build;

    public CloneRepository(ProjectInspector inspector) {
        super(inspector, true);
        this.build = inspector.getBuggyBuild();
    }

    @Override
    protected StepStatus businessExecute() {
        String repository = this.build.getRepository().getSlug();
        String repoRemotePath = GITHUB_ROOT_REPO + repository + ".git";
        String repoLocalPath = this.getInspector().getRepoLocalPath();

        try {
            this.getLogger().debug("Cloning repository " + repository + " in the following directory: " + repoLocalPath);

            Git.cloneRepository().setCloneSubmodules(true).setURI(repoRemotePath).setDirectory(new File(repoLocalPath)).call();

            this.writeProperty("repo",this.getInspector().getRepoSlug());
            return StepStatus.buildSuccess(this);
        } catch (Exception e) {
            this.getLogger().warn("Repository " + repository + " cannot be cloned.");
            this.getLogger().debug(e.toString());
            this.addStepError(e.getMessage());
            return StepStatus.buildError(this,"NOTCLONABLE");
        }
    }

    @Override
    protected void cleanMavenArtifacts() {
        // There is nothing to be clean
        // FIXME: we should not have to override it like that
    }

}
