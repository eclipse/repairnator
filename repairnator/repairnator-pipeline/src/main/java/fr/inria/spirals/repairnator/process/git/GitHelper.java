package fr.inria.spirals.repairnator.process.git;

import fr.inria.jtravis.entities.Build;
import fr.inria.jtravis.entities.PullRequest;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.Metrics;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import org.apache.commons.io.FileUtils;
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
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by fernanda on 01/03/17.
 */
public class GitHelper {

    private static final String TRAVIS_FILE = ".travis.yml";

    private int nbCommits;

    public GitHelper() {
        this.nbCommits = 0;
    }


    public int getNbCommits() {
        return nbCommits;
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
    public String testCommitExistence(Git git, String oldCommitSha, AbstractStep step, Build build) {
        try {
            ObjectId commitObject = git.getRepository().resolve(oldCommitSha);
            git.getRepository().open(commitObject);
            return oldCommitSha;
        } catch (IOException e) {
            step.addStepError("Error while testing commit: " + e);
        }
        return null;
    }

    public boolean addAndCommitRepairnatorLogAndProperties(JobStatus status, Git git, String commitMsg) {
        if (!RepairnatorConfig.getInstance().isPush()) {
            return false;
        }

        try {
            Status gitStatus = git.status().call();
            if (!gitStatus.isClean()) {
                this.getLogger().debug("Commit the logs and properties files");

                List<String> filesChanged = new ArrayList<>();
                filesChanged.addAll(gitStatus.getUncommittedChanges());
                filesChanged.addAll(gitStatus.getUntracked());
                filesChanged.addAll(gitStatus.getUntrackedFolders());

                List<String> filesToAdd = new ArrayList<>(status.getCreatedFilesToPush());
                List<String> filesToCheckout = new ArrayList<>();
                for (String fileName : filesChanged) {
                    if (!fileName.contains("repairnator")) {
                        filesToCheckout.add(fileName);
                    } else {
                        filesToAdd.add(fileName);
                    }
                }

                if (filesToAdd.isEmpty()) {
                    this.getLogger().info("No repairnator properties or log file to commit.");
                    return false;
                }

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

                this.getLogger().info(filesToAdd.size()+" repairnators logs and/or properties file to commit.");

                this.gitAdd(filesToAdd, git);

                PersonIdent personIdent = new PersonIdent("Luc Esape", "luc.esape@gmail.com");
                git.commit().setMessage("repairnator: add log and properties \n"+commitMsg).setCommitter(personIdent)
                        .setAuthor(personIdent).call();

                this.nbCommits++;

                return true;
            } else {
                return false;
            }
        } catch (GitAPIException e) {
            this.getLogger().error("Error while committing repairnator properties/log files ",e);
            return false;
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
        this.getLogger().info("Step " + step.getName() + " - GitHub ratelimit: Limit: " + rateLimit.limit + " Remaining: " + rateLimit.remaining
                + " Reset hour: " + dateFormat.format(rateLimit.reset));
    }

    public boolean mergeTwoCommitsForPR(Git git, Build build, PullRequest prInformation, String repository, AbstractStep step, List<String> paths) {
        try {
            String remoteBranchPath = CloneRepository.GITHUB_ROOT_REPO + prInformation.getOtherRepo().getFullName() + ".git";

            RemoteAddCommand remoteBranchCommand = git.remoteAdd();
            remoteBranchCommand.setName("PR");
            remoteBranchCommand.setUri(new URIish(remoteBranchPath));
            remoteBranchCommand.call();

            git.fetch().setRemote("PR").call();

            String commitHeadSha = this.testCommitExistence(git, prInformation.getHead().getSHA1(), step, build);
            String commitBaseSha = this.testCommitExistence(git, prInformation.getBase().getSHA1(), step, build);

            if (commitHeadSha == null) {
                step.addStepError("Commit head ref cannot be retrieved in the repository: "
                        + prInformation.getHead().getSHA1() + ". Operation aborted.");
                this.getLogger().debug("Step " + step.getName() + " - " + prInformation.getHead().toString());
                return false;
            }

            if (commitBaseSha == null) {
                step.addStepError("Commit base ref cannot be retrieved in the repository: "
                        + prInformation.getBase().getSHA1() + ". Operation aborted.");
                this.getLogger().debug("Step " + step.getName() + " - " + prInformation.getBase().toString());
                return false;
            }

            this.getLogger().debug("Step " + step.getName() + " - Get the commit " + commitHeadSha + " for repo " + repository);

            if (paths != null) {
                this.gitResetPaths(commitHeadSha, paths, git.getRepository().getDirectory().getParentFile());
                PersonIdent personIdent = new PersonIdent("Luc Esape", "luc.esape@gmail.com");
                git.commit().setMessage("Undo changes on source code").setAuthor(personIdent).setCommitter(personIdent).call();
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

    public void computePatchStats(Metrics metric, Git git, RevCommit headRev, RevCommit commit) {
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

            for (DiffEntry entry : entries) {
                String path;
                if (entry.getChangeType() == DiffEntry.ChangeType.DELETE) {
                    path = entry.getOldPath();
                } else {
                    path = entry.getNewPath();
                }
                if (!path.contains("repairnator")) {
                    changedFiles.add(path);

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
                                    nbLineDeleted += edit.getLengthA();
                                    nbLineAdded += edit.getLengthB();
                                    break;

                                case EMPTY:
                                    break;
                            }
                        }
                    }
                }
            }

            metric.setPatchAddedLines(nbLineAdded);
            metric.setPatchDeletedLines(nbLineDeleted);
            metric.setPatchChangedFiles(changedFiles.size());
        } catch (IOException e) {
            this.getLogger().error("Error while computing stat on the patch", e);
        }
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

    /**
     * Copy the files from a directory into another.
     *
     * @param sourceDir is the directory containing the files to be copied from.
     * @param targetDir is the directory where the files from sourceDir are going to be copied to.
     * @param excludedFileNames is an optional parameter that may contain a list of file names not to be included into targetDir.
     * @param isToPerfectlyMatch is a parameter to be used when excludedFileNames is set with one or more file names.
     *                           When isToPerfectlyMatch is set as "true", the files with the exactly names in excludedFileNames will not be copied into targetDir.
     *                           When isToPerfectlyMatch is set as "false", the file with names containing substring of the names in excludedFileNames will not be copied into targetDir.
     *                           For instance, if excludedFileNames contains the file name ".git", when isToPerfectlyMatch is true, the file ".gitignore" will be copied into targetDir, and when isToPerfectlyMatch is false, the file ".gitignore" will NOT be copied into targetDir.
     * @param step is the pipeline step from where this method was called (the info from the step is only used for logging purpose).
     *
     */
    public void copyDirectory(File sourceDir, File targetDir, String[] excludedFileNames, boolean isToPerfectlyMatch, AbstractStep step) {
        getLogger().debug("Copying files...");
        if (sourceDir != null && targetDir != null) {
            getLogger().debug("Source dir: " + sourceDir.getPath());
            getLogger().debug("Target dir: " + targetDir.getPath());

            try {
                FileUtils.copyDirectory(sourceDir, targetDir, new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        for (String excludedFileName : excludedFileNames) {
                            if (isToPerfectlyMatch) {
                                String excludedFilePath = sourceDir.getPath() + "/" + excludedFileName;
                                if (file.getPath().equals(excludedFilePath)) {
                                    getLogger().debug("File not copied: " + file.getPath());
                                    return false;
                                }
                            } else {
                                if (file.getPath().contains(excludedFileName)) {
                                    getLogger().debug("File not copied: " + file.getPath());
                                    return false;
                                }
                            }
                        }
                        return true;
                    }
                });
            } catch (IOException e) {
                step.addStepError("Error while copying files to prepare the git repository folder towards to push data.", e);
            }
        } else {
            step.addStepError("Error while copying files to prepare the git repository folder towards to push data: the source and/or target folders are null.");
        }
    }

    public void removeNotificationFromTravisYML(File directory, AbstractStep step) {
        File travisFile = new File(directory, TRAVIS_FILE);

        if (!travisFile.exists()) {
            getLogger().warn("Travis file has not been detected. It should however exists.");
        } else {
            try {
                List<String> lines = Files.readAllLines(travisFile.toPath());
                List<String> newLines = new ArrayList<>();
                boolean changed = false;
                boolean inNotifBlock = false;

                for (String line : lines) {
                    if (line.trim().equals("notifications:")) {
                        changed = true;
                        inNotifBlock = true;
                    }
                    if (inNotifBlock) {
                        if (line.trim().isEmpty()) {
                            inNotifBlock = false;
                            newLines.add(line);
                        } else {
                            newLines.add("#"+line);
                        }
                    } else {
                        newLines.add(line);
                    }
                }

                if (changed) {
                    getLogger().info("Notification block detected. The travis file will be changed.");
                    File bakTravis = new File(directory, "bak"+TRAVIS_FILE);
                    Files.deleteIfExists(bakTravis.toPath());
                    Files.move(travisFile.toPath(), bakTravis.toPath());
                    FileWriter fw = new FileWriter(travisFile);
                    for (String line : newLines) {
                        fw.append(line);
                        fw.append("\n");
                        fw.flush();
                    }
                    fw.close();

                    step.getInspector().getJobStatus().getCreatedFilesToPush().add(".travis.yml");
                    step.getInspector().getJobStatus().getCreatedFilesToPush().add("bak.travis.yml");
                }
            } catch (IOException e) {
                getLogger().warn("Error while changing travis file", e);
            }
        }
    }

    public void removeGhOauthFromCreatedFilesToPush(File directory, List<String> fileNames) {
        String ghOauthPattern = "--ghOauth\\s+[\\w]+";
        for (String fileName : fileNames) {
            File file = new File(directory, fileName);

            if (!file.exists()) {
                getLogger().warn("The file "+file.toPath()+" does not exist.");
            } else {
                Charset charset = StandardCharsets.UTF_8;
                try {
                    String content = new String(Files.readAllBytes(file.toPath()), charset);
                    String updatedContent = content.replaceAll(ghOauthPattern, "[REMOVED]");
                    if (!content.equals(updatedContent)) {
                        getLogger().info("ghOauth info detected in file "+file.toPath()+". Such file will be changed.");
                        Files.write(file.toPath(), updatedContent.getBytes(charset));
                    }
                } catch (IOException e) {
                    getLogger().warn("Error while checking if file "+file.toPath()+" contains ghOauth info.", e);
                }
            }
        }
    }

}
