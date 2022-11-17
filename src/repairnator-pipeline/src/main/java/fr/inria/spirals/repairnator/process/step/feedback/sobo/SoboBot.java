package fr.inria.spirals.repairnator.process.step.feedback.sobo;

import com.google.common.collect.Lists;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.step.feedback.AbstractFeedbackStep;
import fr.inria.spirals.repairnator.process.step.repair.soraldbot.SoraldAdapter;
import fr.inria.spirals.repairnator.process.step.repair.soraldbot.models.SoraldTargetCommit;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SoboBot extends AbstractFeedbackStep {
    private SoraldTargetCommit commit;
    private String originalBranchName;
    private String workingRepoPath;
    private static final String REPO_PATH = "tmp_repo";

    public SoboBot(ProjectInspector inspector) {
        super(inspector, true);
    }
    public SoboBot() {
        super();
    }

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
        Git git = null;
        try {
            git = getInspector().openAndGetGitObject();
        } catch (IOException e) {
            getInspector().getLogger().info("Not able to open GitHub Object");
        }
        Repository repo = git.getRepository();
        String repoOwnerUserName = getUserName(getInspector().getRepoSlug());
        String task = getTask(getInspector().getRepoSlug());


        if (System.getenv("command").equals("true") ) {

            try {
                SoboAdapter.getInstance(getInspector().getWorkspace()).readCommand(getInspector(), repoOwnerUserName, task);
                return StepStatus.buildSuccess(this);
            } catch (Exception e) {
                getInspector().getLogger().info("can't read command");
                return StepStatus.buildSkipped(this, "There are no new commands to process");
            }


        } else {
            boolean successfulInit = init();
            if (!successfulInit) {
                return StepStatus.buildSkipped(this, "Error while sending feedback with Sobo");
            }

            getLogger().info("Working on: " + commit.getCommitId() + " -- On repo: " + commit.getRepoName());
            GitHub github = SoboAdapter.getInstance(getInspector().getWorkspace()).connectWithGH();
            String commitAuthor;
            GHCommit commitObject;
            Date commitDate;
            try {
                commitObject = github.getRepository(getInspector().getRepoSlug()).getCommit(commit.getCommitId());
                if(commitObject.getAuthor()!=null){
                commitAuthor = commitObject.getAuthor().getLogin();}
                else commitAuthor= repoOwnerUserName;
                commitDate=commitObject.getCommitDate();
                getLogger().info("Commit author : "+  commitAuthor);
            } catch (IOException e) {
                e.printStackTrace();
                return StepStatus.buildSkipped(this, "Error while getting commit author");

            }


            String rules = System.getenv("SONAR_RULES")!=null? System.getenv("SONAR_RULES"): SoboConstants.RULES_SLOT_1;//Arrays.asList(RepairnatorConfig.getInstance().getSonarRules());
            getLogger().info("Working with rules: " + rules);
            String dir = getInspector().getWorkspace() + "\\stats.json";
            boolean isRepoAuthorized4AutomaticFeedback=SoboAdapter.getInstance(getInspector().getWorkspace()).checkUserRepo(repoOwnerUserName, task) ;
            boolean isCommitAuthorStudent=SoboAdapter.getInstance(getInspector().getWorkspace()).checkUser(commitAuthor);
            if (isRepoAuthorized4AutomaticFeedback && isCommitAuthorStudent) {
                try {

                    getLogger().info("Mining Sonar Rules");
                    SoraldAdapter.getInstance(getInspector().getWorkspace()).mine(rules, repo.getDirectory().getParentFile(), dir);
                } catch (Exception e) {
                    return StepStatus.buildSkipped(this, "Error while mining with Sorald");
                }

                try{
                    getLogger().info("Catching mining File and sending the data to MongoDB");

                    SoboAdapter.getInstance(getInspector().getWorkspace()).readExitFile(dir, commit.getCommitId(), commitAuthor, task, getInspector(),  commitDate);
                } catch (Exception e) {
                    return StepStatus.buildSkipped(this, "Error while analizing exit file");
                }
                try{
                    getLogger().info("Getting the most common rule, creating the issue and updating db");
                    //create the issue                              //String commit, String user, String task, ProjectInspector inspector
                    SoboAdapter.getInstance(getInspector().getWorkspace()).getMostCommonRule(commit.getCommitId(), commitAuthor, task, getInspector());
                } catch (Exception e) {
                    return StepStatus.buildSkipped(this, "Error while analyzing exit file");
                }

            }


            return StepStatus.buildSuccess(this);
        }
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

    /**
     *
     * @param repoName the slug of the student repository following the sintax inda-{year}/{user}-task-{n}
     * @return userName of the owner of the repo
     */
    public String getUserName(String repoName){
            char[] chars = repoName.toCharArray();
            String user="";
            int index = repoName.indexOf("inda-");
            if (index!=-1){
                for(int i =index+8;i< chars.length;i++){
                    if(chars[i]=='-'){
                        return user;
                    }
                    user+=chars[i];

                }
                return user;
            }
            index = repoName.indexOf('/');

            for(int i =0;i<index;i++){
                user+=chars[i];
            }
            return user;

        }

    /**
     *
     * @param repoName the slug of the student repository following the sintax inda-{year}/{user}-task-{n}
     * @return task related to the repository
     */
    public String getTask(String repoName){
        StringBuilder task= new StringBuilder();
        char[] chars = repoName.toCharArray();

        // iterate over `char[]` array using enhanced for-loop

        int index = repoName.indexOf("task-");
        if (index==-1){
            return task.toString();
        }
        for(int i =index;i< chars.length;i++){
            task.append(chars[i]);
        }
        return task.toString();

    }


}
