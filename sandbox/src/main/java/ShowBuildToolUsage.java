import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.BuildStatus;
import fr.inria.spirals.jtravis.entities.BuildTool;
import fr.inria.spirals.jtravis.entities.Repository;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.jtravis.helpers.RepositoryHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by urli on 29/09/2017.
 */
public class ShowBuildToolUsage {
    public static void main(String[] args) throws IOException {
        List<String> allProjects = Files.readAllLines(new File(args[0]).toPath());
        String pathOutput = args[1];
        File outputFile = new File(pathOutput);

        Set<String> notOnTravis = new HashSet<>();
        Set<String> useMavenTool = new HashSet<>();
        Set<String> useGradleTool = new HashSet<>();
        Set<String> useUnknownTool = new HashSet<>();
        Set<String> noBuildPassOnMaster = new HashSet<>();

        for (String project : allProjects) {
            Repository repo = RepositoryHelper.getRepositoryFromSlug(project);
            if (repo == null || !repo.isActive()) {
                notOnTravis.add(project);
            } else {
                Build build = BuildHelper.getLastBuildFromMaster(repo);
                if (build == null) {
                    noBuildPassOnMaster.add(project);
                    continue;
                }
                if (build.getBuildStatus() != BuildStatus.PASSED) {
                    build = BuildHelper.getLastBuildOfSameBranchOfStatusBeforeBuild(build, BuildStatus.PASSED, true);
                    if (build == null) {
                        noBuildPassOnMaster.add(project);
                        continue;
                    }
                }

                BuildTool buildTool = build.getBuildTool();

                switch (buildTool) {
                    case MAVEN:
                        useMavenTool.add(project);
                        break;

                    case GRADLE:
                        useGradleTool.add(project);
                        break;

                    case UNKNOWN:
                        useUnknownTool.add(project);
                        break;
                }
            }
        }

        System.out.println("Computed statistics");
        System.out.println("Total number of projects: "+allProjects.size());
        System.out.println("Not using travis: "+notOnTravis.size());
        System.out.println("No build passing on master: "+noBuildPassOnMaster.size());
        System.out.println("Using maven: "+useMavenTool.size());
        System.out.println("Using gradle: "+useGradleTool.size());
        System.out.println("Using unknown: "+useUnknownTool.size());


        BufferedWriter buffer = new BufferedWriter(new FileWriter(outputFile));
        buffer.write("name\tuse Travis\thas Passing build on master\tUse Maven\tUse Gradle\n");
        buffer.flush();

        for (String s : notOnTravis) {
            buffer.write(s+"\t0\t0\t0\t0\n");
            buffer.flush();
        }

        for (String s : noBuildPassOnMaster) {
            buffer.write(s+"\t1\t0\t0\t0\n");
            buffer.flush();
        }

        for (String s : useUnknownTool) {
            buffer.write(s+"\t1\t1\t0\t0\n");
            buffer.flush();
        }

        for (String s : useMavenTool) {
            buffer.write(s+"\t1\t1\t1\t0\n");
            buffer.flush();
        }

        for (String s : useGradleTool) {
            buffer.write(s+"\t1\t1\t0\t1\n");
            buffer.flush();
        }

        buffer.close();
        System.out.println("All results written in "+pathOutput);
    }
}
