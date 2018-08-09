package fr.inria.spirals.repairnator.process.step;

import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.states.PipelineState;
import org.eclipse.jgit.api.Git;

import java.io.File;

/**
 * Created by urli on 03/01/2017.
 */
public class CloneRepository extends AbstractStep {

    protected Build build;

    public CloneRepository(ProjectInspector inspector) {
        super(inspector, true);
        this.build = inspector.getBuggyBuild();
    }

    @Override
    protected StepStatus businessExecute() {
        String repoSlug = this.build.getRepository().getSlug();
        String repoRemotePath = Utils.getCompleteGithubRepoUrl(repoSlug);
        String repoLocalPath = this.getInspector().getRepoLocalPath();

        try {
            this.getLogger().debug("Cloning repository " + repoSlug + " in the following directory: " + repoLocalPath);

            Git.cloneRepository().setCloneSubmodules(true).setURI(repoRemotePath).setDirectory(new File(repoLocalPath)).call();

            return StepStatus.buildSuccess(this);
        } catch (Exception e) {
            this.addStepError("Repository " + repoSlug + " cannot be cloned.", e);
            return StepStatus.buildError(this, PipelineState.NOTCLONABLE);
        }
    }

}
