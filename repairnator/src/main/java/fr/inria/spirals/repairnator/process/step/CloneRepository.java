package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.Commit;
import fr.inria.spirals.jtravis.entities.PRInformation;
import fr.inria.spirals.repairnator.process.ProjectInspector;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.jgit.api.ApplyResult;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.PatchApplyException;
import org.eclipse.jgit.api.errors.PatchFormatException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.URIish;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCompare;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Created by urli on 03/01/2017.
 */
public class CloneRepository extends AbstractStep {
    public static final String GITHUB_ROOT_REPO = "https://github.com/";
    private static final String GITHUB_PATCH_ACCEPT = "application/vnd.github.v3.patch";

    private Build build;

    public CloneRepository(ProjectInspector inspector) {
        super(inspector);
        this.build = inspector.getBuild();
    }

    /**
     * When a commit has been force deleted it still can be retrieved from GitHub API.
     * This function intend to retrieve a patch from the Github API and to apply it back on the repo
     *
     * @param git
     * @param oldCommitSha
     * @return the SHA of the commit created after applying the patch or null if an error occured.
     */
    private String retrieveAndApplyCommitFromGithub(Git git, String oldCommitSha) {
        try {
            GitHub gh = GitHubBuilder.fromEnvironment().build();
            GHRepository ghRepo = gh.getRepository(this.build.getRepository().getSlug());
            GHCommit commit = ghRepo.getCommit(oldCommitSha); // get the deleted commit from GH

            // the loop compute the last parent of the deleted commit which is still in the repo
            // sometimes multiple commits may have been deleted...
            String lastKnowParent = null;
            for (String parentSha : commit.getParentSHA1s()) {
                try {
                    ObjectId commitObject = git.getRepository().resolve(parentSha);
                    git.getRepository().open(commitObject);

                    lastKnowParent = parentSha;
                    break;
                } catch (MissingObjectException e) {
                    continue;
                }
            }

            // checkout the repo to the last known parent of the deleted commit
            git.checkout().setName(lastKnowParent).call();

            // get from github a patch between that commit and the targeted commit
            // note that this patch could contain changes of multiple commits
            GHCompare compare = ghRepo.getCompare(lastKnowParent, oldCommitSha);
            URL patchUrl = compare.getPatchUrl();

            // retrieve it through a simple HTTP request
            // some errors occurs when applying patch from snippets contained in GHCompare object
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(patchUrl).build();
            Call call = client.newCall(request);
            Response response = call.execute();

            // apply the patch and commit changes using message and authors of the referenced commit.
            if (response.code() == 200) {
                ApplyResult result = git.apply().setPatch(response.body().byteStream()).call();

                Commit buildCommit = this.build.getCommit();

                RevCommit ref = git.commit().setAll(true)
                            .setAuthor(buildCommit.getAuthorName(),buildCommit.getAuthorEmail())
                            .setCommitter(buildCommit.getCommitterName(),buildCommit.getCommitterEmail())
                            .setMessage(buildCommit.getMessage()+"\n(This is a retrieve from the following deleted commit: "+oldCommitSha+")")
                            .call();

                return ref.getName();
            }
        } catch (IOException e) {
            this.addStepError("Error while getting commit from Github: "+e);
        } catch (PatchFormatException e) {
            this.addStepError("Error while getting patch from Github: "+e);
        } catch (PatchApplyException e) {
            this.addStepError("Error while applying patch from Github: "+e);
        } catch (GitAPIException e) {
            this.addStepError("Error with Git API: "+e);
        }
        return null;
    }

    /**
     * Test if a commit exists in the given git repository
     *
     * @param git
     * @param oldCommitSha
     * @return oldCommitSha if the commit exists in the repo, a new commit SHA if the commit has been retrieved from GitHub and applied back, or null if the retrieve failed.
     */
    private String testCommitExistence(Git git, String oldCommitSha) {
        try {
            ObjectId commitObject = git.getRepository().resolve(oldCommitSha);
            git.getRepository().open(commitObject);
            return oldCommitSha;
        } catch (MissingObjectException e) {
            return retrieveAndApplyCommitFromGithub(git, oldCommitSha);
        } catch (IncorrectObjectTypeException e) {
            this.addStepError("Error while testing commit: "+e);
        } catch (AmbiguousObjectException e) {
            this.addStepError("Error while testing commit: "+e);
        } catch (IOException e) {
            this.addStepError("Error while testing commit: "+e);
        }
        return null;
    }

    protected void businessExecute() {
        String repository = this.inspector.getRepoSlug();
        String repoRemotePath = GITHUB_ROOT_REPO+repository+".git";
        String repoLocalPath = this.inspector.getRepoLocalPath();

        // start cloning
        try {
            this.getLogger().debug("Cloning repository "+repository+" has in the following directory: "+repoLocalPath);
            Git git = Git.cloneRepository()
                    .setURI(repoRemotePath)
                    .setDirectory(new File(repoLocalPath))
                    .call();

            if (this.build.isPullRequest()) {
                PRInformation prInformation = this.build.getPRInformation();

                this.getLogger().debug("Reproduce the PR for "+repository+" by fetching remote branch and merging.");
                String remoteBranchPath = GITHUB_ROOT_REPO+prInformation.getOtherRepo().getSlug()+".git";

                RemoteAddCommand remoteBranchCommand = git.remoteAdd();
                remoteBranchCommand.setName("PR");
                remoteBranchCommand.setUri(new URIish(remoteBranchPath));
                remoteBranchCommand.call();

                git.fetch().setRemote("PR").call();

                String commitHeadSha = testCommitExistence(git, prInformation.getHead().getSha());
                String commitBaseSha = testCommitExistence(git, prInformation.getBase().getSha());

                if (commitHeadSha == null) {
                    this.addStepError("Commit head ref cannot be retrieved in the repository: "+prInformation.getHead().getSha()+". Operation aborted.");
                    this.getLogger().debug(prInformation.getHead().toString());
                    this.shouldStop = true;
                    return;
                }

                if (commitBaseSha == null) {
                    this.addStepError("Commit base ref cannot be retrieved in the repository: "+prInformation.getBase().getSha()+". Operation aborted.");
                    this.getLogger().debug(prInformation.getBase().toString());
                    this.shouldStop = true;
                    return;
                }

                this.getLogger().debug("Get the commit "+commitHeadSha+" for repo "+repository);
                git.checkout().setName(commitHeadSha).call();

                RevWalk revwalk = new RevWalk(git.getRepository());
                RevCommit revCommitBase = revwalk.lookupCommit(git.getRepository().resolve(commitBaseSha));

                this.getLogger().debug("Do the merge with the PR commit for repo "+repository);
                git.merge().include(revCommitBase).setFastForward(MergeCommand.FastForwardMode.NO_FF).call();
            } else {
                String commitCheckout = this.build.getCommit().getSha();

                commitCheckout = this.testCommitExistence(git, commitCheckout);

                if (commitCheckout != null) {
                    this.getLogger().debug("Get the commit "+commitCheckout+" for repo "+repository);
                    git.checkout().setName(commitCheckout).call();
                } else {
                    this.addStepError("Error while getting the commit to checkout from the repo.");
                    this.shouldStop = true;
                    return;
                }

            }



        } catch (Exception e) {
            this.getLogger().warn("Repository "+repository+" cannot be cloned.");
            this.getLogger().debug(e.toString());
            this.addStepError(e.getMessage());
            this.shouldStop = true;
            return;
        }

        this.state = ProjectState.CLONABLE;
    }

    protected void cleanMavenArtifacts() {}
}
