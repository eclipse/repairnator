package fr.inria.spirals.repairnator.process.step.repair.soraldbot;

import fr.inria.spirals.repairnator.process.step.repair.soraldbot.models.SoraldTargetCommit;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import static fr.inria.spirals.repairnator.process.step.repair.soraldbot.SoraldConstants.SORALD_GIT_PATCHES_DIR;

public class SoraldGithubInteractionsAdapter {
    private static final Logger logger = LoggerFactory.getLogger(SoraldGithubInteractionsAdapter.class);
    private static SoraldGithubInteractionsAdapter _instance;

    private static final String SORALD_CI_REPO = "Sorald-CI";
    private static final String SORALD_URL = "git@github.com:khaes-kth/Sorald-CI.git";

    private String tmpdir;

    public SoraldGithubInteractionsAdapter(String tmpdir){
        this.tmpdir = tmpdir;
    }

    public static SoraldGithubInteractionsAdapter getInstance(String tmpdir){
        if(_instance == null)
            _instance = new SoraldGithubInteractionsAdapter(tmpdir);
        return _instance;
    }

    public File cloneRepo(String repoUrl, String commitId, String dirname)
            throws IOException, GitAPIException {
        File repoDir = new File(tmpdir + File.separator + dirname);

        if (repoDir.exists())
            FileUtils.deleteDirectory(repoDir);

        repoDir.mkdirs();

        Git git = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(repoDir)
                .call();

        if (commitId != null)
            git.checkout().setName(commitId).call();

        git.close();

        return repoDir;
    }

    public String createFork
            (
                    List<File> patchedFiles,
                    String rule,
                    SoraldTargetCommit commit
            ) {
        logger.info("patched files for " + commit.getCommitUrl() + ":");
        patchedFiles.forEach(x -> logger.info(x.getName()));

        File repoDir = null;
        try {
            repoDir = cloneRepo(commit.getRepoUrl(), commit.getCommitId(), "repo");
        } catch (Exception e) {
            logger.error("could not clone repo");
            return null;
        }

        for (File patch : patchedFiles) {
            ProcessBuilder processBuilder =
                    new ProcessBuilder("git", "apply", patch.getAbsolutePath())
                            .directory(repoDir).inheritIO();

            try {
                Process p = processBuilder.start();
                int res = p.waitFor();
                if (res != 0) {
                    logger.error("cannot apply patch");
                    return null;
                }
            } catch (InterruptedException | IOException e) {
                logger.error("Error while executing git command to apply patch");
                return null;
            }
        }

        logger.info("all patches applied");

        File soraldRepo = new File(tmpdir + File.separator + SORALD_CI_REPO);
        try {
            FileUtils.deleteDirectory(soraldRepo);
        } catch (IOException e) {
            logger.error("error while removing old sorald-ci dir");
            return null;
        }

        try {
            ProcessBuilder processBuilder =
                    new ProcessBuilder("git", "clone", SORALD_URL)
                            .directory(new File(tmpdir)).inheritIO();
            Process p = processBuilder.start();
            int res = p.waitFor();
            if (res != 0) {
                logger.error("cannot clone sorald-ci");
                return null;
            }
        } catch (Exception e) {
            logger.error("error while cloning sorald-ci");
            return null;
        }

        String copiedFixedRepoDir = soraldRepo.getPath() + File.separator + "fixed_repo";

        try {
            FileUtils.copyDirectory(repoDir,
                    new File(copiedFixedRepoDir));
        } catch (IOException e) {
            logger.error("cannot copy patched repo into sorald-ci");
            return null;
        }

        try {
            FileUtils.deleteDirectory(new File(copiedFixedRepoDir + File.separator + ".git"));
        } catch (IOException e) {
            logger.error("error while removing .git folder");
            return null;
        }

        try {
            File copiedPatchesDir = new File(copiedFixedRepoDir + File.separator + SORALD_GIT_PATCHES_DIR);
            if(!copiedPatchesDir.exists())
                copiedPatchesDir.mkdirs();

            for(File patch : patchedFiles) {
                FileUtils.copyFile(patch, new File(copiedPatchesDir.getPath() + File.separator + patch.getName()));
            }

        } catch (IOException e) {
            logger.error("cannot copy patched repo into sorald-ci");
            return null;
        }

        String newBranch = "fixed_" + commit.getRepoName() + "_" + commit.getCommitId() + "_" + rule + "_"
                + new Date().getTime();
        try {
            try {
                FileUtils.writeStringToFile(new File(copiedFixedRepoDir + File.separator + "fixed_repo_info.txt"),
                        commit.toString() + System.lineSeparator() + "rule: " + rule, "UTF-8");
            } catch (Exception e) {
                logger.error("cannot cat the fix info");
            }

            ProcessBuilder processBuilder =
                    new ProcessBuilder("git", "add", "--all")
                            .directory(new File(copiedFixedRepoDir)).inheritIO();
            Process p = processBuilder.start();
            int res = p.waitFor();
            if (res != 0) {
                logger.error("cannot git add all");
                return null;
            }

            processBuilder =
                    new ProcessBuilder("git", "checkout", "-b", newBranch)
                            .directory(soraldRepo).inheritIO();
            p = processBuilder.start();
            res = p.waitFor();
            if (res != 0) {
                logger.error("cannot checkout new branch");
                return null;
            }

            processBuilder =
                    new ProcessBuilder("git", "commit", "-m", "\"fixed\"")
                            .directory(soraldRepo).inheritIO();
            p = processBuilder.start();
            res = p.waitFor();
            if (res != 0) {
                logger.error("cannot commit fixed");
                return null;
            }

            processBuilder =
                    new ProcessBuilder("git", "push", "origin", newBranch)
                            .directory(soraldRepo).inheritIO();
            p = processBuilder.start();
            res = p.waitFor();
            if (res != 0) {
                logger.error("cannot push new branch");
                return null;
            }

        } catch (Exception e) {
            logger.error("error while pushing new fork");
            return null;
        }

        return SORALD_URL.replace(".git", "") + "/tree/" + newBranch;
    }
}
