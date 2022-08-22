package fr.inria.spirals.repairnator.realtime.githubapi.commits.models;

/**
 * It represents a Pull Request on GitHub.
 */
public class SelectedPullRequest {
    private long id;
    private int number;
    private String url;
    private String headCommitSHA1;
    private String repoName;

    public SelectedPullRequest(long id, int number, String url, String headCommitSHA1, String repoName) {
        this.id = id;
        this.number = number;
        this.url = url;
        this.headCommitSHA1 = headCommitSHA1;
        this.repoName = repoName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHeadCommitSHA1() {
        return headCommitSHA1;
    }

    public void setHeadCommitSHA1(String headCommitSHA1) {
        this.headCommitSHA1 = headCommitSHA1;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    @Override
    public String toString() {
        return "SelectedPullRequest{" +
                "id=" + id +
                ", number=" + number +
                ", url='" + url + '\'' +
                ", headCommitSHA1='" + headCommitSHA1 + '\'' +
                ", repoName='" + repoName + '\'' +
                '}';
    }
}
