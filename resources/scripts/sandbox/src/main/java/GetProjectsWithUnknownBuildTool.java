import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.BuildStatus;
import fr.inria.spirals.jtravis.entities.BuildTool;
import fr.inria.spirals.jtravis.entities.Repository;
import fr.inria.spirals.jtravis.helpers.RepositoryHelper;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by urli on 19/09/2017.
 */
public class GetProjectsWithUnknownBuildTool {
    public static void main(String[] args) throws IOException {
        List<String> allProjects = Files.readAllLines(new File(args[0]).toPath());

        List<String> results = new ArrayList<>();

        for (String project : allProjects) {
            Repository repo = RepositoryHelper.getRepositoryFromSlug(project);

            if (repo != null) {
                Build b = repo.getLastBuild(false);

                if (b != null) {
                    BuildTool tool = b.getBuildTool();

                    if (b.getBuildStatus() == BuildStatus.PASSED && tool == BuildTool.UNKNOWN) {
                        results.add("https://travis-ci.org/"+project);
                    }

                }
            }
        }

        System.out.println(results.size()+" results");
        System.out.println(StringUtils.join(results,"\n"));
    }
}
