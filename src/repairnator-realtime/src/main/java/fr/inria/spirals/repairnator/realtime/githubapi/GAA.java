package fr.inria.spirals.repairnator.realtime.githubapi;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import org.apache.commons.io.FileUtils;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.File;
import java.io.IOException;

import static org.kohsuke.github.GitHub.connectToEnterpriseWithOAuth;

// GAM stands for: Github Api Adapter
public class GAA {
    private static final String TOKENS_PATH = System.getProperty("user.home") + "/Downloads/config.ini";
    private static int lastUsedToken = 0;

    public static GitHub g() throws IOException {
        GitHubBuilder githubBuilder = new GitHubBuilder();

        if ((tokens == null || tokens.length == 0) && RepairnatorConfig.getInstance().getGithubToken() != null)
            tokens = new String[]{RepairnatorConfig.getInstance().getGithubToken()};

        if (tokens != null && tokens.length > 0){
            try {
            githubBuilder = githubBuilder.withOAuthToken(tokens[lastUsedToken++ % tokens.length]);
            } catch (Exception e) {
                System.out.println("Repo not found in public Github");
            }
        }

        return githubBuilder.build();
    }

    /**
     * This method is used to connect to GitHub Enterprise
     * example, KTH is https://gits-15.sys.kth.se/api/v3
     * @param enterpriseAPI the Enterprise API
     * @throws IOException if the connection fails
     */
    public static GitHub g(String enterpriseAPI) throws IOException {
        GitHubBuilder githubBuilder = new GitHubBuilder();

        if ((tokens == null || tokens.length == 0) && RepairnatorConfig.getInstance().getGithubToken() != null)
            tokens = new String[]{RepairnatorConfig.getInstance().getGithubToken()};

        if (tokens != null && tokens.length > 0){
            try{
                return connectToEnterpriseWithOAuth(enterpriseAPI, System.getenv("login"), System.getenv("GOAUTH"));
            }catch (Exception e) {
                System.out.println("Repo not found in  Enterprise Github");
            }
        }

        return githubBuilder.build();
    }

    static {
        try {
            tokens = FileUtils.readLines(new File(TOKENS_PATH),
                    "UTF-8").toArray(new String[0]);
        } catch (IOException e) {
            new RuntimeException("No Github-API token file is provided. Repairnator will work without tokens.", e);

        }
    }

    private static String[] tokens;
}
