package fr.inria.spirals.repairnator.process.inspectors.properties.repository;

public class Original {

    private String name;
    private long githubId;
    private String url;

    public Original() {
        this.name = "";
        this.githubId = 0;
        this.url = "";
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
}
