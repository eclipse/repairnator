import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by urli on 27/09/2017.
 */
public class CheckIfIdsHaveABranch {
    public static void main(String[] args) throws IOException {
        List<String> allIds = Files.readAllLines(new File(args[0]).toPath());
        String ghLogin = args[1];
        String ghToken = args[2];

        GitHub gitHub = GitHubBuilder.fromEnvironment().withOAuthToken(ghToken, ghLogin).build();
        GHRepository repo = gitHub.getRepository("surli/bugs-collection");

        Set<String> branchNames = repo.getBranches().keySet();
        Map<String, String> branchById = new HashMap<>();

        for (String branchName : branchNames) {
            String[] splitted = branchName.split("-");
            if (splitted.length > 1) {
                String key = "";
                for (int i = 0; i < splitted.length - 2; i++) {
                    key += splitted[i];
                    if (i < splitted.length - 3) {
                        key += "-";
                    }
                }

                branchById.put(key, branchName);
            }
        }

        List<String> branchToDelete = new ArrayList<>();
        String slug = null;
        for (String line : allIds) {
            if (line.startsWith("Project")) {
                String[] splitted = line.split(" ");
                slug = splitted[1];
            } else {
                String key = slug+"-"+line.trim();
                if (branchById.containsKey(key)) {
                    System.out.println(key+" -> "+branchById.get(key));
                    branchToDelete.add(branchById.get(key));
                }
            }
        }
    }
}
