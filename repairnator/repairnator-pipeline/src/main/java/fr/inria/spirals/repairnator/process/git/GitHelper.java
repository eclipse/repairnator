package fr.inria.spirals.repairnator.process.git;

import fr.inria.jtravis.entities.Build;
import fr.inria.jtravis.entities.PullRequest;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.properties.patchDiff.PatchDiff;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by fernanda on 01/03/17.
 */
public class GitHelper {

    private int nbCommits;

    private static PersonIdent committerIdent;

    public GitHelper() {
        this.nbCommits = 0;
    }


    public int getNbCommits() {
        return nbCommits;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(GitHelper.class);
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
    public String testCommitExistence(Git git, String oldCommitSha, AbstractStep step, Build build) {
        try {
            ObjectId commitObject = git.getRepository().resolve(oldCommitSha);
            git.getRepository().open(commitObject);
            return oldCommitSha;
        } catch (IOException e) {
            step.addStepError("Error while testing commit: " + e);
            if (!build.getBranch().isExistsOnGithub()) {
                step.addStepError("The commit can't be resolved because the branch where such commit was done does not exist in GitHub anymore: " + Utils.getBranchUrl(build.getBranch().getName(), build.getRepository().getSlug()));
            }
        }
        return null;
    }

    public static PersonIdent getCommitterIdent() {
        if (committerIdent == null) {
            committerIdent =  new PersonIdent(RepairnatorConfig.getInstance().getGithubUserName(), RepairnatorConfig.getInstance().getGithubUserEmail());
        }

        return committerIdent;
    }

    public boolean addAndCommitRepairnatorLogAndProperties(JobStatus status, Git git, String commitMsg) {
        if (!RepairnatorConfig.getInstance().isPush()) {
            return false;
        }

        try {
            Status gitStatus = git.status().call();
            if (!gitStatus.isClean()) {
                this.getLogger().debug("Commit repairnator files...");

                List<String> filesChanged = new ArrayList<>();
                filesChanged.addAll(gitStatus.getUncommittedChanges());
                filesChanged.addAll(gitStatus.getUntracked());
                filesChanged.addAll(gitStatus.getUntrackedFolders());

                List<String> filesToAdd = new ArrayList<>(status.getCreatedFilesToPush());
                List<String> filesToCheckout = new ArrayList<>();
                for (String fileName : filesChanged) {
                    if (!status.isCreatedFileToPush(fileName)) {
                        filesToCheckout.add(fileName);
                    } else {
                        filesToAdd.add(fileName);
                    }
                }

                if (filesToAdd.isEmpty()) {
                    this.getLogger().info("There is no repairnator file to commit.");
                    return false;
                }
                this.getLogger().info(filesToAdd.size()+" repairnator files to commit.");

                if (!filesToCheckout.isEmpty()) {
                    this.getLogger().debug("Checkout "+filesToCheckout.size()+" files.");

                    this.getLogger().info("Exec following command: git checkout -- " + StringUtils.join(filesToCheckout, " "));
                    ProcessBuilder processBuilder = new ProcessBuilder("git", "checkout", "--", StringUtils.join(filesToCheckout, " "))
                            .directory(git.getRepository().getDirectory().getParentFile()).inheritIO();

                    try {
                        Process p = processBuilder.start();
                        p.waitFor();

                    } catch (InterruptedException|IOException e) {
                        this.getLogger().error("Error while executing git command to checkout files: " + e);
                        return false;
                    }

                    //git.checkout().addPaths(filesToCheckout).call();
                }

                this.gitAdd(filesToAdd, git);
                git.commit()
                        .setMessage("repairnator: add files created to push \n"+commitMsg)
                        .setCommitter(this.getCommitterIdent())
                        .setAuthor(this.getCommitterIdent()).call();

                this.nbCommits++;

                return true;
            } else {
                return false;
            }
        } catch (GitAPIException e) {
            this.getLogger().error("Error while committing files created by Repairnator.", e);
            return false;
        }
    }

    public void initAllSubmodules(Git git) {
        this.getLogger().info("Init git submodules.");
        ProcessBuilder processBuilder = new ProcessBuilder("git", "submodule", "update", "--init", "--recursive")
                .directory(git.getRepository().getDirectory().getParentFile()).inheritIO();

        try {
            Process p = processBuilder.start();
            p.waitFor();

        } catch (InterruptedException|IOException e) {
            this.getLogger().error("Error while executing git command to get git submodules: " + e);
        }
    }

    private String getLastKnowParent(GitHub gh, GHRepository ghRepo, Git git, String oldCommitSha, AbstractStep step) throws IOException {
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
            this.getLogger().debug("Step " + step.getName() + " - The commit has more than one parent : " + commit.getHtmlUrl());
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

    private void showGitHubRateInformation(GitHub gh, AbstractStep step) throws IOException {
        GHRateLimit rateLimit = gh.getRateLimit();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        this.getLogger().info("Step " + step.getName() + " - GitHub rate limit: Limit: " + rateLimit.limit +
                " - Remaining: " + rateLimit.remaining + " - Reset hour: " + dateFormat.format(rateLimit.reset));
    }

    public boolean mergeTwoCommitsForPR(Git git, Build build, PullRequest prInformation, String repository, AbstractStep step, List<String> paths) {
        try {
            String remoteBranchPath = Utils.getCompleteGithubRepoUrl(prInformation.getOtherRepo().getFullName());

            RemoteAddCommand remoteBranchCommand = git.remoteAdd();
            remoteBranchCommand.setName("PR");
            remoteBranchCommand.setUri(new URIish(remoteBranchPath));
            remoteBranchCommand.call();

            git.fetch().setRemote("PR").call();

            String commitHeadSha = this.testCommitExistence(git, prInformation.getHead().getSHA1(), step, build);
            String commitBaseSha = this.testCommitExistence(git, prInformation.getBase().getSHA1(), step, build);

            if (commitHeadSha == null) {
                step.addStepError("Commit head ref cannot be retrieved from the repository: "
                        + prInformation.getHead().getSHA1() + ". Operation aborted.");
                return false;
            }

            if (commitBaseSha == null) {
                step.addStepError("Commit base ref cannot be retrieved from the repository: "
                        + prInformation.getBase().getSHA1() + ". Operation aborted.");
                return false;
            }

            this.getLogger().debug("Step " + step.getName() + " - Get the commit " + commitHeadSha + " for repo " + repository);

            if (paths != null) {
                this.gitResetPaths(commitHeadSha, paths, git.getRepository().getDirectory().getParentFile());
                git.commit().setMessage("Undo changes on source code").setAuthor(this.getCommitterIdent()).setCommitter(this.getCommitterIdent()).call();
            } else {
                git.checkout().setName(commitHeadSha).call();
            }

            RevWalk revwalk = new RevWalk(git.getRepository());
            RevCommit revCommitBase = revwalk.lookupCommit(git.getRepository().resolve(commitBaseSha));

            this.getLogger().debug("Step " + step.getName() + " - Do the merge with the PR commit for repo " + repository);
            MergeResult result = git.merge().include(revCommitBase).setFastForward(MergeCommand.FastForwardMode.NO_FF).call();
            this.nbCommits++;
        } catch (Exception e) {
            step.addStepError(e.getMessage());
            this.getLogger().error("Step " + step.getName() + " - Repository " + repository + " cannot be cloned.",e);
            return false;
        }
        return true;
    }

    public String forkRepository(String repository, AbstractStep step) throws IOException {
        GitHub gh = RepairnatorConfig.getInstance().getGithub();
        showGitHubRateInformation(gh, step);
        if (gh.getRateLimit().remaining > 10) {
            GHRepository originalRepo = gh.getRepository(repository);
            if (originalRepo != null) {
                return originalRepo.fork().getUrl().toString();
            }
        }
        return null;
    }

    public void computePatchStats(JobStatus jobStatus, Git git, RevCommit headRev, RevCommit commit) {
        try {
            ObjectReader reader = git.getRepository().newObjectReader();
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            oldTreeIter.reset(reader, headRev.getTree());
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(reader, commit.getTree());

            DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
            diffFormatter.setRepository(git.getRepository());
            diffFormatter.setContext(0);
            List<DiffEntry> entries = diffFormatter.scan(newTreeIter, oldTreeIter);

            int nbLineAdded = 0;
            int nbLineDeleted = 0;
            Set<String> changedFiles = new HashSet<>();
            Set<String> addedFiles = new HashSet<>();
            Set<String> deletedFiles = new HashSet<>();

            for (DiffEntry entry : entries) {
                String path;
                if (entry.getChangeType() == DiffEntry.ChangeType.DELETE) {
                    path = entry.getOldPath();
                } else {
                    path = entry.getNewPath();
                }
                if (!jobStatus.isCreatedFileToPush(path) && path.endsWith(".java")) {
                    if (entry.getChangeType() == DiffEntry.ChangeType.MODIFY ||
                            entry.getChangeType() == DiffEntry.ChangeType.RENAME) {
                        changedFiles.add(path);
                    } else if (entry.getChangeType() == DiffEntry.ChangeType.ADD ||
                            entry.getChangeType() == DiffEntry.ChangeType.COPY) {
                        addedFiles.add(path);
                    } else if (entry.getChangeType() == DiffEntry.ChangeType.DELETE) {
                        deletedFiles.add(path);
                    }

                    FileHeader fileHeader = diffFormatter.toFileHeader(entry);
                    List<? extends HunkHeader> hunks = fileHeader.getHunks();
                    for (HunkHeader hunk : hunks) {
                        EditList edits = hunk.toEditList();
                        for (Edit edit : edits) {
                            switch (edit.getType()) {
                                case INSERT:
                                    nbLineAdded += edit.getLengthB();
                                    break;

                                case DELETE:
                                    nbLineDeleted += edit.getLengthA();
                                    break;

                                case REPLACE:
                                    int diff = edit.getLengthA() - edit.getLengthB();
                                    if (diff > 0) {
                                        nbLineAdded += edit.getLengthA();
                                        nbLineDeleted += edit.getLengthB();
                                    } else {
                                        nbLineDeleted += edit.getLengthA();
                                        nbLineAdded += edit.getLengthB();
                                    }
                                    break;

                                case EMPTY:
                                    break;
                            }
                        }
                    }
                }
            }

            PatchDiff patchDiff = jobStatus.getProperties().getPatchDiff();
            patchDiff.getFiles().setNumberAdded(addedFiles.size());
            patchDiff.getFiles().setNumberChanged(changedFiles.size());
            patchDiff.getFiles().setNumberDeleted(deletedFiles.size());
            patchDiff.getLines().setNumberAdded(nbLineAdded);
            patchDiff.getLines().setNumberDeleted(nbLineDeleted);
        } catch (IOException e) {
            this.getLogger().error("Error while computing stat on the patch.", e);
        }
    }

    public static int gitCreateNewBranchAndCheckoutIt(String path, String branchName) {
        ProcessBuilder processBuilder = new ProcessBuilder("git", "checkout", "-b", branchName)
                .directory(new File(path)).inheritIO();

        try {
            Process p = processBuilder.start();
            return p.waitFor();
        } catch (InterruptedException|IOException e) {
            getLogger().error("Error while executing git command to create new branch and checkout it: " + e);
        }

        return -1;
    }

    public void gitAdd(List<String> files, Git git) {
        for (String file : files) {
            // add force is not supported by JGit...
            ProcessBuilder processBuilder = new ProcessBuilder("git", "add", "-f", file)
                    .directory(git.getRepository().getDirectory().getParentFile()).inheritIO();

            try {
                Process p = processBuilder.start();
                p.waitFor();
            } catch (InterruptedException|IOException e) {
                this.getLogger().error("Error while executing git command to add files: " + e);
            }
        }
    }

    public void gitResetPaths(String commit, List<String> paths, File gitDirectory) {
        paths = this.removeDuplicatePaths(paths);

        List<String> gitReset = new ArrayList<>();
        gitReset.add("git");
        gitReset.add("reset");
        gitReset.add(commit);
        gitReset.add("--");
        for (String path : paths) {
            gitReset.add(path);
        }
        this.executeGitCommand(gitReset.toArray(new String[0]), gitDirectory);

        List<String> gitClean = new ArrayList<>();
        gitClean.add("git");
        gitClean.add("clean");
        gitClean.add("-fd");
        gitClean.add("--");
        for (String path : paths) {
            gitClean.add(path);
        }
        this.executeGitCommand(gitClean.toArray(new String[0]), gitDirectory);

        String[] gitCheckout = {"git", "checkout", "--", "."};
        this.executeGitCommand(gitCheckout, gitDirectory);
    }

    public void executeGitCommand(String[] gitCommand, File gitDirectory) {
        this.getLogger().debug("Executing git command: " + StringUtils.join(gitCommand, " "));

        ProcessBuilder processBuilder = new ProcessBuilder(gitCommand).directory(gitDirectory).inheritIO();
        try {
            Process process = processBuilder.start();
            process.waitFor();
        } catch (InterruptedException|IOException e) {
            this.getLogger().error("Error while executing git command: " + e);
        }
    }

    public List<String> removeDuplicatePaths(List<String> paths) {
        List<String> newPaths = new ArrayList<>();
        for (String path : paths) {
            if (!newPaths.contains(path)) {
                newPaths.add(path);
            }
        }
        return newPaths;
    }
}
