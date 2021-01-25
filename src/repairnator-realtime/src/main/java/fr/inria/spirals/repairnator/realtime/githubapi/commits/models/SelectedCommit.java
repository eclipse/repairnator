package fr.inria.spirals.repairnator.realtime.githubapi.commits.models;

public class SelectedCommit {
    private static final String COMMIT_URL_TEMPLATE = "https://github.com/{repo}/commit/{commit}";
    private Boolean isTravisFailed;
    private Boolean isGithubActionsFailed;
    private String commitId;
    private String repoName;
    private String commitUrl;

    public SelectedCommit
            (
                    Boolean isTravisFailed,
                    Boolean isGithubActionsFailed,
                    String commitId,
                    String repoName
            ) {
        this.isGithubActionsFailed = isGithubActionsFailed;
        this.isTravisFailed = isTravisFailed;
        this.commitId = commitId;
        this.repoName = repoName;
        this.commitUrl = COMMIT_URL_TEMPLATE.replace("{repo}", repoName).replace("{commit}", commitId);
    }

    public String getCommitUrl() {
        return commitUrl;
    }

    public void setCommitUrl(String commitUrl) {
        this.commitUrl = commitUrl;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public Boolean getGithubActionsFailed() {
        return isGithubActionsFailed;
    }

    public void setGithubActionsFailed(Boolean githubActionsFailed) {
        isGithubActionsFailed = githubActionsFailed;
    }

    public Boolean getTravisFailed() {
        return isTravisFailed;
    }

    public void setTravisFailed(Boolean travisFailed) {
        isTravisFailed = travisFailed;
    }

    @Override
    public String toString() {
        return "FailedCommit{" +
                "commitId='" + commitId + '\'' +
                ", repoName='" + repoName + '\'' +
                '}';
    }
}
