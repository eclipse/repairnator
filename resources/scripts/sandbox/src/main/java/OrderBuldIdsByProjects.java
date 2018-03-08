import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by urli on 08/09/2017.
 */
public class OrderBuldIdsByProjects {
    public static void main(String[] args) throws IOException {
        List<String> allIds = Files.readAllLines(new File(args[0]).toPath());

        List<String> projectsNames = null;
        if (args.length > 4) {
            projectsNames = Files.readAllLines(new File(args[1]).toPath());
            String githubLogin = args[2];
            String githubToken = args[3];

            RepairnatorConfig.getInstance().setGithubLogin(githubLogin);
            RepairnatorConfig.getInstance().setGithubToken(githubToken);
        }

        HashMap<String,List<Integer>> results = new HashMap<>();
        int i = 0;
        for (String s : allIds) {
            int buildId = Integer.parseInt(s);

            Build build = BuildHelper.getBuildFromId(buildId, null);
            String projectName = build.getRepository().getSlug();

            if (projectsNames == null || projectsNames.contains(projectName)) {
                if (!results.containsKey(projectName)) {
                    results.put(projectName, new ArrayList<>());
                }
                results.get(projectName).add(buildId);
                i++;
            }

        }

        System.out.println(results.keySet().size()+" detected projects: ("+ StringUtils.join(results.keySet(), ",")+")");
        System.out.println("Results:");
        for (String s : results.keySet()) {
            System.out.println("Project "+s+" : ");
            System.out.println(StringUtils.join(results.get(s), "\n"));
            System.out.println("\n");
        }

    }
}
