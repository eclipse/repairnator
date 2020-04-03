package fr.inria.spirals.repairnator.process.step;

import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.utils.Utils;
import fr.inria.spirals.repairnator.process.inspectors.GitRepositoryProjectInspector;
import fr.inria.spirals.repairnator.states.PipelineState;
import org.eclipse.jgit.api.Git;

import java.io.File;
import java.nio.file.Files;
import org.apache.commons.io.FileUtils;
import java.util.concurrent.TimeUnit;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;

public class BranchJenkinsCloneRepository extends AbstractStep {

    public BranchJenkinsCloneRepository(GitRepositoryProjectInspector inspector) {
        super(inspector,true);
    }

    @Override
    protected StepStatus businessExecute() {
        String repoUrl = ((GitRepositoryProjectInspector) getInspector()).getGitRepositoryUrl() + ".git";
        String repoLocalPath = this.getInspector().getRepoLocalPath();
        String repoBranch = ((GitRepositoryProjectInspector) getInspector()).getGitRepositoryBranch();
        String gitCommitHash = getConfig().getGitRepositoryIdCommit();

        this.getLogger().info("Cloning repository " + repoUrl + " in the following directory: " + repoLocalPath);
        try {
            File file = new File(repoLocalPath);
            if(file.isDirectory()){
                if(file.list().length>0){
                    getLogger().info("Directory is not empty! - will be overwritten");
                    FileUtils.cleanDirectory(file);
                }
            }
            
            Git git = Git.cloneRepository().setCloneSubmodules(true).setURI(repoUrl).setBranch(repoBranch).setDirectory(new File(repoLocalPath)).call();

            if (gitCommitHash != null){
                git.checkout().setName(gitCommitHash).call();
            }
            return StepStatus.buildSuccess(this);
        } catch (Exception e) {
            this.addStepError("Repository cannot be cloned.", e);
            return StepStatus.buildError(this, PipelineState.NOTCLONABLE);
        }
    }

}
