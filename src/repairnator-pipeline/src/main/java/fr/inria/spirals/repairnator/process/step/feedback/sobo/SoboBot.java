package fr.inria.spirals.repairnator.process.step.feedback.sobo;

import com.google.common.collect.Lists;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.step.feedback.AbstractFeedbackStep;
import fr.inria.spirals.repairnator.process.step.repair.soraldbot.SoraldBot;
import fr.inria.spirals.repairnator.process.step.repair.soraldbot.models.SoraldTargetCommit;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class SoboBot extends AbstractFeedbackStep {
    String repo;
    private SoraldTargetCommit commit;
    private String originalBranchName;
    private String workingRepoPath;
    private static final String REPO_PATH = "tmp_repo";

    /**
     * {@return if the initialization is successful}
     */
    private boolean init() {
        commit = new SoraldTargetCommit(getConfig().getGitCommitHash(), getInspector().getRepoSlug());
        workingRepoPath = getInspector().getWorkspace() + File.separator + REPO_PATH;

        try {
            originalBranchName = getOriginalBranch();
        } catch (IOException | GitAPIException e) {
            getLogger().error("IOException while looking for the original branch: " + e.getLocalizedMessage());
        }

        return commit != null && workingRepoPath != null && originalBranchName != null;
    }

    @Override
    protected StepStatus businessExecute() {
        boolean successfulInit= init();
        //mine the repo, fetch the output file, send the commit
        getLogger().info("Working on: " + commit+ " -- On repo: " +repo);
        if (!successfulInit) {
            return StepStatus.buildSkipped(this, "Error while sending feedback with Sobo");
        }

        RepairnatorConfig debug = getConfig();

        return StepStatus.buildSuccess(this);
    }

    @Override
    public String getFeedbackToolName() {
        return SoboConstants.SOBO_TOOL_NAME;
    }

    private String getOriginalBranch() throws IOException, GitAPIException {
        String branchName = getBranchOfCommit(getInspector().getRepoLocalPath(), commit.getCommitId());

        if (branchName == null) {
            getLogger().error("The branch of the commit was not found");
            return null;
        }
        return branchName;
    }
        public String getBranchOfCommit(String gitDir, String commitName) throws IOException, GitAPIException {
            Git git = getInspector().openAndGetGitObject();

            List<Ref> branches = git.branchList().call();

            Set<String> containingBranches = new HashSet<>();
            for (Ref branch : branches) {
                String branchName = branch.getName();
                Iterable<RevCommit> commits = git.log().add(git.getRepository().resolve(branchName)).call();
                List<RevCommit> commitsList = Lists.newArrayList(commits.iterator());
                if (commitsList.stream().anyMatch(rev -> rev.getName().equals(commitName))) {
                    containingBranches.add(branchName);
                }
            }

            git.close();

            if (containingBranches.size() == 0)
                return null;

            Optional<String> selectedBranch = containingBranches.stream()
                    .filter(b -> b.equals("master") || b.equals("main") || b.contains("/master") || b.contains("/main")).findAny();

            return selectedBranch.isPresent() ? selectedBranch.get() : containingBranches.iterator().next();
        }
}
