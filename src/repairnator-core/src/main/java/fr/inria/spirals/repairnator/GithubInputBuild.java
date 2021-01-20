package fr.inria.spirals.repairnator;

import java.util.ArrayList;
import java.util.List;

public class GithubInputBuild implements InputBuild {

    private String url;
    private String sha;

    public GithubInputBuild(String url, String sha)
    {
        this.url = url;
        this.sha = sha;
    }

    public String getSha() {
        return sha;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public List<String> getEnvVariables() {
        List<String> r = new ArrayList<>();

        r.add("GITHUB_URL=" + url);
        r.add("GITHUB_SHA=" + sha);
        return r;
    }

    @Override
    public String toString() {
        return url + "-" + sha;
    }
}
