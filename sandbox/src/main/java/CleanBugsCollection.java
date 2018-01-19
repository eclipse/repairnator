
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
        String pathToDirectory = "../bearsData/bugLists/for-script";
        String fileForPushedBranches = "bugs-collection-branches.txt";

        List<String> branchNameToKeep = new ArrayList<>();
        List<String> branchNameToDelete = new ArrayList<>();

        File dir = new File(pathToDirectory);
        if (dir.exists()) {
            Files.list(dir.toPath()).forEach((Path filePath) -> {
                if (!filePath.getFileName().toString().endsWith(fileForPushedBranches)) {
                    try {
                        branchNameToKeep.addAll(Files.readAllLines(filePath));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        for (String s : Files.readAllLines(new File(dir, fileForPushedBranches).toPath())) {
            if (!s.equals("master")) {
                if (!branchNameToKeep.contains(s)) {
                    branchNameToDelete.add(s);
                }
            }
        }

        System.out.println("Branches to delete:");
        System.out.println(StringUtils.join(branchNameToDelete, "\n"));
    }
}
