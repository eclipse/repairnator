/**
 * Created by urli on 01/09/2017.
 */
public class RepairInfo {
    private String pushBranchName;

    private String githubProject;
    private String bugCommit;
    private String patchCommit;
    private String prId;

    public String getPushBranchName() {
        return pushBranchName;
    }

    public void setPushBranchName(String pushBranchName) {
        this.pushBranchName = pushBranchName;
    }

    public String getGithubProject() {
        return githubProject;
    }

    public void setGithubProject(String githubProject) {
        this.githubProject = githubProject;
    }

    public String getBugCommit() {
        return bugCommit;
    }

    public void setBugCommit(String bugCommit) {
        this.bugCommit = bugCommit;
    }

    public String getPatchCommit() {
        return patchCommit;
    }

    public void setPatchCommit(String patchCommit) {
        this.patchCommit = patchCommit;
    }

    public String getPrId() {
        return prId;
    }

    public void setPrId(String prId) {
        this.prId = prId;
    }
}
