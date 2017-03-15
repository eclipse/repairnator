package fr.inria.spirals.repairnator.process.git;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.Commit;
import fr.inria.spirals.jtravis.entities.PRInformation;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.PatchApplyException;
import org.eclipse.jgit.api.errors.PatchFormatException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.URIish;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by fernanda on 01/03/17.
 */
public class GitHelper {

    private static GitHelper instance;

    private GitHelper() {}

    private static GitHelper getInstance() {
        if (instance == null) {
            instance = new GitHelper();
        }
        return instance;
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Test if a commit exists in the given git repository
     *
     * @param git
     * @param oldCommitSha
     * @return oldCommitSha if the commit exists in the repo, a new commit SHA
     *         if the commit has been retrieved from GitHub and applied back, or
     *         null if the retrieve failed.
     */
    public static String testCommitExistence(Git git, String oldCommitSha, AbstractStep step, Build build) {
        try {
            ObjectId commitObject = git.getRepository().resolve(oldCommitSha);
            git.getRepository().open(commitObject);
            return oldCommitSha;
        } catch (MissingObjectException e) {
            return retrieveAndApplyCommitFromGithub(git, oldCommitSha, step, build);
        } catch (IncorrectObjectTypeException e) {
            step.addStepError("Error while testing commit: " + e);
        } catch (AmbiguousObjectException e) {
            step.addStepError("Error while testing commit: " + e);
        } catch (IOException e) {
            step.addStepError("Error while testing commit: " + e);
        }
        return null;
    }

    /**
     * When a commit has been force deleted it still can be retrieved from
     * GitHub API. This function intend to retrieve a patch from the Github API
     * and to apply it back on the repo
     *
     * @param git
     * @param oldCommitSha
     * @return the SHA of the commit created after applying the patch or null if
     *         an error occured.
     */
    private static String retrieveAndApplyCommitFromGithub(Git git, String oldCommitSha, AbstractStep step, Build build) {
        try {
            Status gitStatus = git.status().call();
            if (!gitStatus.isClean()) {
                GitHelper.getInstance().getLogger().debug("Commit the logs and properties files");
                AddCommand addCommand = git.add();

                String path = git.getRepository().getDirectory().getParent();
                for (File file : new File(path).listFiles()) {
                    if (file.getName().contains("repairnator")) {
                        addCommand.addFilepattern(file.getName());
                    }
                }

                addCommand.call();
                PersonIdent personIdent = new PersonIdent("Luc Esape", "luc.esape@gmail.com");
                git.commit().setMessage("repairnator: add log and properties").setCommitter(personIdent)
                        .setAuthor(personIdent).call();
            }

            GitHub gh = GitHubBuilder.fromEnvironment().build();
            GHRepository ghRepo = gh.getRepository(build.getRepository().getSlug());

            String lastKnowParent = getLastKnowParent(gh, ghRepo, git, oldCommitSha, step);

            // checkout the repo to the last known parent of the deleted commit
            git.checkout().setName(lastKnowParent).call();

            // get from github a patch between that commit and the targeted
            // commit
            // note that this patch could contain changes of multiple commits
            GHCompare compare = ghRepo.getCompare(lastKnowParent, oldCommitSha);

            showGitHubRateInformation(gh, step);

            URL patchUrl = compare.getPatchUrl();

            getInstance().getLogger().debug("Step " + step.getName() + " - Retrieve commit patch from the following URL: " + patchUrl);

            // retrieve it through a simple HTTP request
            // some errors occurs when applying patch from snippets contained in
            // GHCompare object
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(patchUrl).build();
            Call call = client.newCall(request);
            Response response = call.execute();

            File tempFile = File.createTempFile(build.getRepository().getSlug(), "patch");

            // apply the patch and commit changes using message and authors of
            // the referenced commit.
            if (response.code() == 200) {

                FileWriter writer = new FileWriter(tempFile);
                writer.write(response.body().string());
                writer.flush();
                writer.close();

                getInstance().getLogger().info("Step " + step.getName() + " - Exec following command: git apply " + tempFile.getAbsolutePath());
                ProcessBuilder processBuilder = new ProcessBuilder("git", "apply", tempFile.getAbsolutePath())
                        .directory(new File(step.getInspector().getRepoLocalPath())).inheritIO();

                Process p = processBuilder.start();
                try {
                    p.waitFor();

                    // Applying patch does not work as the move of file is
                    // broken in JGit
                    // It assumes the target directory exists.
                    // ApplyResult result =
                    // git.apply().setPatch(response.body().byteStream()).call();

                    Commit buildCommit = build.getCommit();

                    // add all file for the next commit
                    git.add().addFilepattern(".").call();

                    // do the commit
                    RevCommit ref = git.commit().setAll(true)
                            .setAuthor(buildCommit.getAuthorName(), buildCommit.getAuthorEmail())
                            .setCommitter(buildCommit.getCommitterName(), buildCommit.getCommitterEmail())
                            .setMessage(buildCommit.getMessage()
                                    + "\n(This is a retrieve from the following deleted commit: " + oldCommitSha + ")")
                            .call();

                    tempFile.delete();

                    return ref.getName();
                } catch (InterruptedException e) {
                    step.addStepError("Error while executing git command to apply patch: " + e);
                }

            }
        } catch (IOException e) {
            step.addStepError("Error while getting commit from Github: " + e);
        } catch (PatchFormatException e) {
            step.addStepError("Error while getting patch from Github: " + e);
        } catch (PatchApplyException e) {
            step.addStepError("Error while applying patch from Github: " + e);
        } catch (GitAPIException e) {
            step.addStepError("Error with Git API: " + e);
        }
        return null;
    }

    private static String getLastKnowParent(GitHub gh, GHRepository ghRepo, Git git, String oldCommitSha, AbstractStep step) throws IOException {
        showGitHubRateInformation(gh, step);
        GHCommit commit = ghRepo.getCommit(oldCommitSha); // get the deleted
        // commit from GH
        List<String> commitParents = commit.getParentSHA1s();

        if (commitParents.isEmpty()) {
            step.addStepError("The following commit does not have any parent in GitHub: " + oldCommitSha
                    + ". It cannot be resolved.");
            return null;
        }

        if (commitParents.size() > 1) {
            getInstance().getLogger().debug("Step " + step.getName() + " - The commit has more than one parent : " + commit.getHtmlUrl());
        }

        String parent = commitParents.get(0);

        try {
            ObjectId commitObject = git.getRepository().resolve(parent);
            git.getRepository().open(commitObject);

            return parent;
        } catch (MissingObjectException e) {
            return getLastKnowParent(gh, ghRepo, git, parent, step);
        }
    }

    private static void showGitHubRateInformation(GitHub gh, AbstractStep step) throws IOException {
        GHRateLimit rateLimit = gh.getRateLimit();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        getInstance().getLogger().info("Step " + step.getName() + " - GitHub ratelimit: Limit: " + rateLimit.limit + " Remaining: " + rateLimit.remaining
                + " Reset hour: " + dateFormat.format(rateLimit.reset));
    }

    public static boolean mergeTwoCommitsForPR(Git git, Build build, PRInformation prInformation, String repository, AbstractStep step) {
        try {
            String remoteBranchPath = CloneRepository.GITHUB_ROOT_REPO + prInformation.getOtherRepo().getSlug() + ".git";

            RemoteAddCommand remoteBranchCommand = git.remoteAdd();
            remoteBranchCommand.setName("PR");
            remoteBranchCommand.setUri(new URIish(remoteBranchPath));
            remoteBranchCommand.call();

            git.fetch().setRemote("PR").call();

            String commitHeadSha = GitHelper.testCommitExistence(git, prInformation.getHead().getSha(), step, build);
            String commitBaseSha = GitHelper.testCommitExistence(git, prInformation.getBase().getSha(), step, build);

            if (commitHeadSha == null) {
                step.addStepError("Commit head ref cannot be retrieved in the repository: "
                        + prInformation.getHead().getSha() + ". Operation aborted.");
                getInstance().getLogger().debug("Step " + step.getName() + " - " + prInformation.getHead().toString());
                return false;
            }

            if (commitBaseSha == null) {
                step.addStepError("Commit base ref cannot be retrieved in the repository: "
                        + prInformation.getBase().getSha() + ". Operation aborted.");
                getInstance().getLogger().debug("Step " + step.getName() + " - " + prInformation.getBase().toString());
                return false;
            }

            getInstance().getLogger().debug("Step " + step.getName() + " - Get the commit " + commitHeadSha + " for repo " + repository);
            git.checkout().setName(commitHeadSha).call();

            RevWalk revwalk = new RevWalk(git.getRepository());
            RevCommit revCommitBase = revwalk.lookupCommit(git.getRepository().resolve(commitBaseSha));

            getInstance().getLogger().debug("Step " + step.getName() + " - Do the merge with the PR commit for repo " + repository);
            git.merge().include(revCommitBase).setFastForward(MergeCommand.FastForwardMode.NO_FF).call();
        } catch (Exception e) {
            getInstance().getLogger().warn("Step " + step.getName() + " - Repository " + repository + " cannot be cloned.");
            getInstance().getLogger().debug("Step " + step.getName() + " - " + e.toString());
            step.addStepError(e.getMessage());
            return false;
        }
        return true;
    }
}
