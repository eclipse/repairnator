package fr.inria.spirals.jtravis.helpers;

/**
 * Created by urli on 27/06/2017.
 */
public class GithubTokenHelper {
    private static GithubTokenHelper instance;

    private String githubLogin;
    private String githubOauth;

    private GithubTokenHelper() {}

    public static GithubTokenHelper getInstance() {
        if (instance == null) {
            instance = new GithubTokenHelper();
        }
        return instance;
    }

    public String getGithubLogin() {
        return (isAvailable()) ? githubLogin : null;
    }

    public void setGithubLogin(String githubLogin) {
        this.githubLogin = githubLogin;
    }

    public String getGithubOauth() {
        return (isAvailable()) ? githubOauth : null;
    }

    public void setGithubOauth(String githubOauth) {
        this.githubOauth = githubOauth;
    }

    public boolean isAvailable() {
        if (this.githubLogin != null && this.githubOauth != null) {
            return true;
        }

        if (System.getenv("GITHUB_OAUTH") != null && System.getenv("GITHUB_LOGIN") != null) {
            this.setGithubLogin(System.getenv("GITHUB_LOGIN"));
            this.setGithubOauth(System.getenv("GITHUB_OAUTH"));
            return true;
        }

        return false;
    }
}
