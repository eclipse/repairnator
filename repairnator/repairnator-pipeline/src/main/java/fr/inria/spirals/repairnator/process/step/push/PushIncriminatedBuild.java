package fr.inria.spirals.repairnator.process.step.push;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.states.PushState;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;

/**
 * Created by urli on 05/01/2017.
 */
public class PushIncriminatedBuild extends AbstractStep {
    private static final String REMOTE_REPO_EXT = ".git";

    public static final String REMOTE_NAME = "saveFail";

    private String branchName;
    private String remoteRepoUrl;

    public PushIncriminatedBuild(ProjectInspector inspector) {
        super(inspector, false);
        this.remoteRepoUrl = this.getConfig().getPushRemoteRepo();
        this.branchName = this.getInspector().getRemoteBranchName();
    }

    @Override
    protected StepStatus businessExecute() {
        if (this.getConfig().isPush()) {
            if (this.remoteRepoUrl == null || this.remoteRepoUrl.equals("")) {
                this.getLogger().error("Remote repo should be set !");
                this.setPushState(PushState.REPO_NOT_PUSHED);
                return StepStatus.buildSkipped(this, "Remote information was not provided");
            }

            String remoteRepo = this.remoteRepoUrl + REMOTE_REPO_EXT;

            this.getLogger().debug("Start to push failing pipelineState in the remote repository: " + remoteRepo + " branch: " + branchName);

            if (this.getConfig().getGithubToken() == null || this.getConfig().getGithubToken().equals("")) {
                this.getLogger().warn("You must set the GITHUB_OAUTH env property to push incriminated build.");
                this.setPushState(PushState.REPO_NOT_PUSHED);
                return StepStatus.buildSkipped(this, "Github authentication information was not provided.");
            }

            try {
                Git git = Git.open(new File(this.getInspector().getRepoToPushLocalPath()));
                this.getLogger().debug("Add the remote repository to push the current pipelineState");

                RemoteAddCommand remoteAdd = git.remoteAdd();
                remoteAdd.setName(REMOTE_NAME);
                remoteAdd.setUri(new URIish(remoteRepo));
                remoteAdd.call();

                CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(this.getConfig().getGithubToken(), "");

                this.getLogger().debug("Check if a branch already exists in the remote repository");
                ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh","-c","git remote show "+REMOTE_NAME+" | grep "+branchName)
                        .directory(new File(this.getInspector().getRepoLocalPath()));

                Process p = processBuilder.start();
                BufferedReader stdin = new BufferedReader(new InputStreamReader(p.getInputStream()));
                p.waitFor();

                this.getLogger().debug("Get result from grep process...");
                String processReturn = "";
                String line;
                while (stdin.ready() && (line = stdin.readLine()) != null) {
                    processReturn += line;
                }

                if (!processReturn .equals("")) {
                    this.getLogger().warn("A branch already exist in the remote repo with the following name: " + branchName);
                    this.getLogger().debug("Here the grep return: "+processReturn);
                    return StepStatus.buildSkipped(this, "A branch already exist in the remote repo with the following name: " + branchName);
                }

                this.getLogger().debug("Prepare the branch and push");
                Ref branch = git.checkout().setCreateBranch(true).setName(branchName).call();

                git.push().setRemote(REMOTE_NAME).add(branch).setCredentialsProvider(credentialsProvider).call();

                this.getInspector().getJobStatus().setHasBeenPushed(true);

                this.getInspector().getJobStatus().setGitBranchUrl(this.remoteRepoUrl+"/tree/"+branchName);
                this.setPushState(PushState.REPO_PUSHED);
                return StepStatus.buildSuccess(this);
            } catch (IOException e) {
                this.getLogger().error("Error while reading git directory at the following location: "
                        + this.getInspector().getRepoLocalPath() + " : " + e);
                this.addStepError(e.getMessage());
            } catch (URISyntaxException e) {
                this.getLogger()
                        .error("Error while setting remote repository with following URL: " + remoteRepo + " : " + e);
                this.addStepError(e.getMessage());
            } catch (GitAPIException e) {
                this.getLogger().error("Error while executing a JGit operation: " + e);
                this.addStepError(e.getMessage());
            } catch (InterruptedException e) {
                this.addStepError("Error while executing git command to check branch presence" + e.getMessage());
            }
            this.setPushState(PushState.REPO_NOT_PUSHED);
            return StepStatus.buildSkipped(this, "Error while pushing.");
        } else {
            this.getLogger().info("The push argument is set to false. Nothing will be pushed.");
            return StepStatus.buildSkipped(this);
        }
    }

}
