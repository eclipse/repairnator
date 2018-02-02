import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FindMissingBranches {

    public static void main(String[] args) throws IOException {
        String pathCollectedBuildidsForBranches = "/Users/urli/Github/librepair/resources/buildids-associated-branches.txt";
        String CSV = "/Users/urli/Github/librepair/resources/SEIP/seip-reproduced-bugs.csv";

        List<String> linesAssociatedBuildids = Files.readAllLines(new File(pathCollectedBuildidsForBranches).toPath());
        List<String> linesCSV = Files.readAllLines(new File(CSV).toPath());

        Map<String, String> branchByIds = new HashMap<>();

        String pathOutput = "/tmp/buildid-with-branch.csv";

        for (String lineAssociatedBuild : linesAssociatedBuildids) {
            String[] splitted = lineAssociatedBuild.split(" ");

            if (splitted.length == 2) {
                String buildid = splitted[0];
                String branchname = splitted[1];

                branchByIds.put(buildid, branchname);
            }
        }

        for (String lineCSV : linesCSV) {
            String[] splitted = lineCSV.split("\t");

        }
    }


}
