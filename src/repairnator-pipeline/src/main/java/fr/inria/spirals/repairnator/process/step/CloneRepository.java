package fr.inria.spirals.repairnator.process.step;

import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.process.inspectors.GitRepositoryProjectInspector;
import fr.inria.spirals.repairnator.utils.Utils;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.states.PipelineState;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.UserTokenHandler;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by urli on 03/01/2017.
 */
public class CloneRepository extends AbstractStep {

    protected Build build;

    public CloneRepository(ProjectInspector inspector) {
        super(inspector, true);
    }

    @Override
    protected StepStatus businessExecute() {
        GitRepositoryProjectInspector githubInspector;

        try {
            githubInspector = (GitRepositoryProjectInspector) getInspector();
        } catch (Exception ex) {
            this.addStepError("Problem with calling the inspector");
            return StepStatus.buildError(this, PipelineState.NOTCLONABLE);
        }
        String branch = null;
        if (githubInspector.getGitRepositoryBranch() != null) {
            branch = "refs/heads/" + githubInspector.getGitRepositoryBranch();
        }
        String repoUrl = githubInspector.getGitRepositoryUrl() + ".git";
        String repoLocalPath = githubInspector.getRepoLocalPath();
        try {
            this.getLogger().info("Cloning repository " + repoUrl + " in the following directory: " + repoLocalPath);


            FileUtils.deleteDirectory(new File(repoLocalPath));

            CloneCommand cloneRepositoryCommand = Git.cloneRepository()
                    .setCloneSubmodules(true)
                    .setURI(repoUrl)
                    .setDirectory(new File(repoLocalPath))
                    ;
            String auth=System.getenv("GOAUTH");
            if(auth!=null){
                cloneRepositoryCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                        System.getenv("GOAUTH"),""));
            }
            cloneRepositoryCommand.call();

            return StepStatus.buildSuccess(this);
        } catch (Exception e) {
            this.addStepError("Repository " + repoUrl + " cannot be cloned.", e);
            return StepStatus.buildError(this, PipelineState.NOTCLONABLE);
        }
    }

}
