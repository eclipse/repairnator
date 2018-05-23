package fr.inria.spirals.repairnator.process.inspectors.metrics4bears.repository;

public class Repository {

    private String name;
    private long githubId;
    private String url;
    private boolean fork;
    private Original original;
    private boolean isPullRequest;
    private int pullRequestId;

    public Repository() {
        this.original = new Original();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getGithubId() {
        return githubId;
    }

    public void setGithubId(long githubId) {
        this.githubId = githubId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean getFork() {
        return fork;
    }

    public void setFork(boolean fork) {
        this.fork = fork;
    }

    public Original getOriginal() {
        return original;
    }

    public boolean getIsPullRequest() {
        return isPullRequest;
    }

    public void setIsPullRequest(boolean isPullRequest) {
        this.isPullRequest = isPullRequest;
    }

    public int getPullRequestId() {
        return pullRequestId;
    }

    public void setPullRequestId(int pullRequestId) {
        this.pullRequestId = pullRequestId;
    }

}
