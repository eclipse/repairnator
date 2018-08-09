package fr.inria.spirals.repairnator.process.step.push;

import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector4Bears;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.states.PushState;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.*;
import java.net.URISyntaxException;

public class PushProcessEnd extends AbstractStep {

    public static final String REMOTE_NAME = "saveFail";

    private String remoteRepoUrl;
    private String branchName;

    public PushProcessEnd(ProjectInspector inspector) {
        super(inspector, false);
        this.remoteRepoUrl = this.getConfig().getPushRemoteRepo();
        this.branchName = this.getInspector().getRemoteBranchName();
    }

    @Override
    protected StepStatus businessExecute() {
        if (this.getConfig().isPush()) {
            if (this.getInspector().getJobStatus().getLastPushState() != PushState.NONE) {
                if (this.getInspector() instanceof ProjectInspector4Bears &&
                        !((ProjectInspector4Bears) this.getInspector()).isBug()) {
                    this.getLogger().error("The reproduction of the bug and/or the patch failed. Step bypassed.");
                    this.setPushState(PushState.REPO_NOT_PUSHED);
                    return StepStatus.buildSkipped(this, "The reproduction of the bug and/or the patch failed. Step bypassed.");
                }

                if (this.remoteRepoUrl == null || this.remoteRepoUrl.equals("")) {
                    this.getLogger().error("Remote repo URL should be set !");
                    this.setPushState(PushState.REPO_NOT_PUSHED);
                    return StepStatus.buildSkipped(this, "Remote repo information was not provided.");
                }

                String remoteRepo = this.remoteRepoUrl + Utils.REMOTE_REPO_EXT;

                this.getLogger().debug("Start to push pipeline state in the remote repository: " + remoteRepo + " - branch: " + branchName);

                if (this.getConfig().getGithubToken() == null || this.getConfig().getGithubToken().equals("")) {
                    this.getLogger().warn("You must set the GITHUB_OAUTH env property to push data.");
                    this.setPushState(PushState.REPO_NOT_PUSHED);
                    return StepStatus.buildSkipped(this, "GitHub authentication information was not provided.");
                }

                try {
                    Git git = Git.open(new File(this.getInspector().getRepoToPushLocalPath()));

                    this.getLogger().debug("Add the remote repository to push the current pipeline state...");

                    RemoteAddCommand remoteAdd = git.remoteAdd();
                    remoteAdd.setName(REMOTE_NAME);
                    remoteAdd.setUri(new URIish(remoteRepo));
                    remoteAdd.call();

                    CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(this.getConfig().getGithubToken(), "");

                    this.getLogger().debug("Check if a branch already exists in the remote repository...");
                    ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", "git remote show " + REMOTE_NAME + " | grep " + branchName)
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

                    if (!processReturn.equals("")) {
                        this.getLogger().warn("A branch already exists in the remote repo with the following name: " + branchName);
                        this.getLogger().debug("Here the grep return: " + processReturn);
                        return StepStatus.buildSkipped(this, "A branch already exists in the remote repo with the following name: " + branchName);
                    }

                    this.getLogger().debug("Prepare the branch and push...");
                    Ref branch = git.checkout().setCreateBranch(true).setName(branchName).call();

                    git.push().setRemote(REMOTE_NAME).add(branch).setCredentialsProvider(credentialsProvider).call();

                    this.getInspector().getJobStatus().setHasBeenPushed(true);

                    this.getInspector().getJobStatus().setGitBranchUrl(this.remoteRepoUrl + "/tree/" + branchName);
                    this.setPushState(PushState.REPO_PUSHED);
                    return StepStatus.buildSuccess(this);
                } catch (IOException e) {
                    this.addStepError("Error while reading git directory at the following location: " + this.getInspector().getRepoLocalPath() + ".", e);
                } catch (URISyntaxException e) {
                    this.addStepError("Error while setting remote repository with the following URL: " + remoteRepo + ".", e);
                } catch (GitAPIException e) {
                    this.addStepError("Error while executing a JGit operation.", e);
                } catch (InterruptedException e) {
                    this.addStepError("Error while executing git command to check branch existence.", e);
                }
                this.setPushState(PushState.REPO_NOT_PUSHED);
                return StepStatus.buildSkipped(this, "Error while pushing.");
            } else {
                this.getLogger().info("No commit has been done, so there is nothing to push. Step bypassed.");
                this.setPushState(PushState.REPO_NOT_PUSHED);
                return StepStatus.buildSkipped(this);
            }
        } else {
            this.getLogger().info("Repairnator is configured NOT to push. Step bypassed.");
            this.setPushState(PushState.REPO_NOT_PUSHED);
            return StepStatus.buildSkipped(this);
        }
    }
}
