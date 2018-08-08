package fr.inria.spirals.repairnator.process.step;

import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.properties.machineInfo.MachineInfo;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.states.PipelineState;
import org.eclipse.jgit.api.Git;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.File;
import java.io.IOException;

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

            this.writeProperties();

            return StepStatus.buildSuccess(this);
        } catch (Exception e) {
            this.addStepError("Repository " + repoSlug + " cannot be cloned.", e);
            return StepStatus.buildError(this, PipelineState.NOTCLONABLE);
        }
    }

    private void writeProperties() {
        JobStatus jobStatus = this.getInspector().getJobStatus();

        MachineInfo machineInfo = jobStatus.getProperties().getReproductionBuggyBuild().getMachineInfo();
        machineInfo.setHostName(Utils.getHostname());

        if (this.getConfig().getLauncherMode() == LauncherMode.BEARS) {
            this.getInspector().getJobStatus().getProperties().setVersion("Bears 1.0");
        }

        fr.inria.spirals.repairnator.process.inspectors.properties.repository.Repository repository = this.getInspector().getJobStatus().getProperties().getRepository();
        repository.setName(this.getInspector().getRepoSlug());
        repository.setUrl(Utils.getSimpleGithubRepoUrl(this.getInspector().getRepoSlug()));

        if (this.build.isPullRequest()) {
            repository.setIsPullRequest(true);
            repository.setPullRequestId(this.build.getPullRequestNumber());
        }

        GitHub gitHub;
        try {
            gitHub = this.getConfig().getGithub();
            GHRepository repo = gitHub.getRepository(this.getInspector().getRepoSlug());
            repository.setGithubId(repo.getId());
            if (repo.isFork()) {
                repository.setIsFork(true);
                repository.getOriginal().setName(repo.getParent().getFullName());
                repository.getOriginal().setGithubId(repo.getParent().getId());
                repository.getOriginal().setUrl(Utils.getSimpleGithubRepoUrl(repo.getParent().getFullName()));
            }
        } catch (IOException e) {
            this.getLogger().warn("It was not possible to retrieve information to check if " + this.getInspector().getRepoSlug() + " is a fork.");
            this.getLogger().debug(e.toString());
        }

        switch (this.getInspector().getBuildToBeInspected().getStatus()) {
            case ONLY_FAIL:
                this.getInspector().getJobStatus().getProperties().setType("only_fail");
                break;

            case FAILING_AND_PASSING:
                this.getInspector().getJobStatus().getProperties().setType("failing_passing");
                break;

            case PASSING_AND_PASSING_WITH_TEST_CHANGES:
                this.getInspector().getJobStatus().getProperties().setType("passing_passing");
                break;
        }
    }

}
