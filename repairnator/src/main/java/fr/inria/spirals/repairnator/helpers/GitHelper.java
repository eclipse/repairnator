package fr.inria.spirals.repairnator.helpers;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.Commit;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.PatchApplyException;
import org.eclipse.jgit.api.errors.PatchFormatException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.kohsuke.github.*;

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

            step.getLogger().debug("Retrieve commit patch from the following URL: " + patchUrl);

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

                step.getLogger().info("Exec following command: git apply " + tempFile.getAbsolutePath());
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
            step.getLogger().debug("The commit has more than one parent : " + commit.getHtmlUrl());
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
        step.getLogger().info("GitHub ratelimit: Limit: " + rateLimit.limit + " Remaining: " + rateLimit.remaining
                + " Reset hour: " + dateFormat.format(rateLimit.reset));
    }
}
