package fr.inria.spirals.repairnator.process.step.feedback.sobo;

import com.google.common.collect.Lists;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.step.feedback.AbstractFeedbackStep;
import fr.inria.spirals.repairnator.process.step.repair.soraldbot.SoraldAdapter;
import fr.inria.spirals.repairnator.process.step.repair.soraldbot.SoraldBot;
import fr.inria.spirals.repairnator.process.step.repair.soraldbot.models.SoraldTargetCommit;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SoboBot extends AbstractFeedbackStep {
    String repoName;
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
        getLogger().info("Working on: " + commit.getCommitId()+ " -- On repo: " +commit.getRepoName() );
        if (!successfulInit) {
            return StepStatus.buildSkipped(this, "Error while sending feedback with Sobo");
        }


        String rules = "S109,S1155,S1481";//Arrays.asList(RepairnatorConfig.getInstance().getSonarRules());
        getLogger().info("Working on: " + commit.getCommitUrl() + " " + commit.getCommitId() + " " );
        String dir =getInspector().getWorkspace()+"\\stats.json";
        try{


            Git git = getInspector().openAndGetGitObject();
            Repository repo=git.getRepository();
            String userName = getUserName(commit.getRepoName());
            String task= getTask(commit.getRepoName());

            if(System.getenv("command")!=null){
                // get Main Issue
                //get the comment - command
                //execute
                SoboAdapter.getInstance(getInspector().getWorkspace()).getMainIssue(getInspector());


            }
            else{

            getLogger().info("Mining Sonar Rules");
            SoraldAdapter.getInstance(getInspector().getWorkspace()).mine(rules,repo.getDirectory().getParentFile(),dir);

            getLogger().info("Catching mining File and sending the data to MongoDB");
            SoboAdapter.getInstance(getInspector().getWorkspace()).readExitFile(dir,commit.getCommitId(),userName,task);


            //parse the exit file
            // send the data to the DB
            // make a request to the database
            //create the issue                              //String commit, String user, String task, ProjectInspector inspector
            SoboAdapter.getInstance(getInspector().getWorkspace()).getMostCommonRule(commit.getCommitId(),userName,task,getInspector() );}


        } catch (Exception e) {
            return StepStatus.buildSkipped(this, "Error while mining with Sorald");
        }


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
    public String getTask(String repoName){
        String task="";
        char[] chars = repoName.toCharArray();

        // iterate over `char[]` array using enhanced for-loop

        int index = repoName.indexOf("task-");
        if (index==-1){
            return task;
        }
        for(int i =index;i< chars.length;i++){
            task+=chars[i];
        }
        return task;

    }
}
