
// This class intends to compare bugs collection file
// containing name of actual repo branches
// against list of reproduced bugs containing

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CleanBugsCollection {

    public static void main(String[] args) throws IOException {
        String prefix = "* https://github.com/surli/bugs-collection/tree/";
        String pathToDirectory = "../bearsData/validated";
        String fileForPushedBranches = "bugs-collection-branches.txt";

        List<String> pushedBranches = new ArrayList<>();
        List<String> branchNameToKeep = new ArrayList<>();
        List<String> branchNameToDelete = new ArrayList<>();

        File dir = new File(pathToDirectory);

        if (!dir.exists()) {
            throw new RuntimeException("Error with dir");
        }

        for (String s : Files.readAllLines(new File(dir, fileForPushedBranches).toPath())) {
            pushedBranches.add(s);
        }

        System.out.println("Number of pushed branches: "+pushedBranches.size());

        Files.list(dir.toPath()).forEach((Path filePath) -> {
            if (!filePath.getFileName().toString().endsWith(fileForPushedBranches)) {
                try {
                    for (String s : Files.readAllLines(filePath)) {
                        if (s.startsWith(prefix)) {
                            String branchName = s.substring(prefix.length()).trim();
                            if (!pushedBranches.contains(branchName)) {
                                System.err.println("The following branch has not been pused on the repo: "+branchName);
                            } else {
                                branchNameToKeep.add(branchName);
                            }
                        } else {
                            System.err.println("The following branch has a wrong name: "+s);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        System.out.println("Number of validated branches: "+branchNameToKeep.size());

        for (String allPushed : pushedBranches) {
            if (!allPushed.equals("master")) {
                if (!branchNameToKeep.contains(allPushed)) {
                    branchNameToDelete.add(allPushed);
                }
            }
        }

        System.out.println("Number of branches to delete: "+branchNameToDelete.size());
        System.out.println(StringUtils.join(branchNameToDelete, "\n"));
    }
}
