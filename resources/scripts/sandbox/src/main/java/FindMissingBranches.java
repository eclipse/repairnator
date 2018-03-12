import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FindMissingBranches {
    private static final String URL_PREFIX = "https://github.com/Spirals-Team/librepair-experiments/tree/";

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

        Map<String, String> branchesPerBuildId = new HashMap<>();
        OkHttpClient httpClient = new OkHttpClient();
        List<String> buildIdsWithoutBranches = new ArrayList<>();

        int nbLine = 0;
        for (String lineCSV : linesCSV) {
            if (nbLine++ > 0) {
                String[] splitted = lineCSV.split("\t");

                String buildId = null;
                String branchUrl = null;

                if (splitted.length > 1) {
                    buildId = splitted[0];
                } else if (splitted.length == 3) {
                    branchUrl = splitted[2];
                } else {
                    System.err.println("Error while parsing CSV line number "+nbLine+": "+lineCSV);
                }

                if (branchUrl != null && !branchUrl.isEmpty()) {
                    branchesPerBuildId.put(buildId, branchUrl);
                } else {
                    if (branchByIds.containsKey(buildId)) {
                        String branchName = branchByIds.get(buildId);
                        String url = URL_PREFIX+branchName;

                        branchesPerBuildId.put(buildId, url);
                    } else {
                        buildIdsWithoutBranches.add(buildId);
                    }
                }
            }
        }

        FileWriter fw = new FileWriter(new File(pathOutput));

        for (Map.Entry<String, String> entry : branchesPerBuildId.entrySet()) {
            fw.append(entry.getKey());
            fw.append("\t");
            fw.append(entry.getValue());
            fw.append("\n");
            fw.flush();
        }
        fw.close();
        System.out.println("The following build id do not have an associated branch:");
        System.out.println(StringUtils.join(buildIdsWithoutBranches, "\n"));
        System.out.println("db.inspector.find({buildId: {$in: ["+StringUtils.join(buildIdsWithoutBranches, ",")+"]}})");
    }


}
