package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.jtravis.entities.Repository;
import fr.inria.spirals.repairnator.Launcher;
import fr.inria.spirals.repairnator.process.ProjectInspector;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by urli on 05/01/2017.
 */
public class PushIncriminatedBuild extends AbstractStep {
    private static final String REMOTE_REPO_ROOT = "https://github.com/Spirals-Team/librepair-experiments";
    private static final String REMOTE_REPO = REMOTE_REPO_ROOT+".git";
    private static final String REMOTE_REPO_TREE = REMOTE_REPO_ROOT+"/tree/";
    private static final String NB_COMMITS_TO_KEEP = "10";

    private String branchName;

    public PushIncriminatedBuild(ProjectInspector inspector) {
        super(inspector);

        SimpleDateFormat dateFormat = new SimpleDateFormat("YYYYMMdd-HHmmss");
        String formattedDate = dateFormat.format(this.inspector.getBuild().getFinishedAt());

        this.branchName = inspector.getRepoSlug().replace('/','-')+'-'+inspector.getBuild().getId()+'-'+formattedDate;
    }

    public String getRemoteLocation() {
        return REMOTE_REPO_TREE+this.branchName;
    }

    @Override
    protected void businessExecute() {
        this.getLogger().debug("Start to push failing state in the remote repository: "+REMOTE_REPO+" branch: "+branchName);
        if (System.getenv("GITHUB_OAUTH") == null) {
            this.getLogger().warn("You must the GITHUB_OAUTH env property to push incriminated build.");
            return;
        }

        try {
            Git git = Git.open(new File(inspector.getRepoLocalPath()));
            git.checkout().setCreateBranch(true).setName("detached").call();
            ObjectId id = git.getRepository().resolve("HEAD~"+NB_COMMITS_TO_KEEP);
            if (id != null) {
                this.getLogger().debug("Get only the last "+NB_COMMITS_TO_KEEP+" commits before push.");
                ProcessBuilder processBuilder = new ProcessBuilder("git","rebase-last-x",NB_COMMITS_TO_KEEP)
                        .directory(new File(this.inspector.getRepoLocalPath()))
                        .inheritIO();

                Process p = processBuilder.start();
                p.waitFor();
            } else {
                this.getLogger().debug("The repository contains less than "+NB_COMMITS_TO_KEEP+": push all the repo.");
            }





            RemoteAddCommand remoteAdd = git.remoteAdd();
            remoteAdd.setName("saveFail");
            remoteAdd.setUri(new URIish(REMOTE_REPO));
            remoteAdd.call();

            git.fetch().setRemote("saveFail").call();

            Ref theRef = git.getRepository().findRef("refs/remotes/saveFail/"+branchName);

            if (theRef != null) {
                this.getLogger().warn("A branch already exist in the remote repo with the following name: "+branchName);
                this.setState(ProjectState.PUSHED);
                return;
            }

            Ref branch = git.branchCreate().setName(branchName).call();

            git.push()
                .setRemote("saveFail")
                .add(branch)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider( System.getenv("GITHUB_OAUTH"), "" ))
                .call();

            this.setState(ProjectState.PUSHED);

        } catch (IOException e) {
            this.getLogger().error("Error while reading git directory at the following location: "+inspector.getRepoLocalPath()+" : "+e);
            this.addStepError(e.getMessage());
        } catch (URISyntaxException e) {
            this.getLogger().error("Error while setting remote repository with following URL: "+REMOTE_REPO+" : "+e);
            this.addStepError(e.getMessage());
        } catch (GitAPIException e) {
            this.getLogger().error("Error while executing a JGit operation: "+e);
            this.addStepError(e.getMessage());
        } catch (InterruptedException e) {
            this.addStepError("Error while executing git command to gest last 10 commits"+e.getMessage());
        }
    }


}
