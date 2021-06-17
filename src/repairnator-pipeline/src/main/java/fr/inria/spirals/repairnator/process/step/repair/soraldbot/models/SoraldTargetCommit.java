package fr.inria.spirals.repairnator.process.step.repair.soraldbot.models;

public class SoraldTargetCommit {
    private static final String REPO_URL_TEMPLATE = "https://github.com/{slug}/";
    private static final String COMMIT_URL_TEMPLATE = REPO_URL_TEMPLATE + "commit/{commit-id}";

    private String commitUrl;
    private String commitId;
    private String repoUrl;
    private String repoName;

    public SoraldTargetCommit(String commitId, String repoName){
        this.repoName = repoName;
        this.commitId = commitId;

        this.repoUrl = REPO_URL_TEMPLATE.replace("{slug}", repoName);
        this.commitUrl = COMMIT_URL_TEMPLATE.replace("{slug}", repoName).replace("{commit-id}", commitId);
    }

    public String getCommitUrl() {
        return commitUrl;
    }

    public void setCommitUrl(String commitUrl) {
        this.commitUrl = commitUrl;
    }

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }
}
