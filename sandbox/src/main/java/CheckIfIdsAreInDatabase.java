import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.serializer.mongodb.MongoConnection;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.mongodb.client.model.Filters.*;

/**
 * Created by urli on 22/08/2017.
 */
public class CheckIfIdsAreInDatabase {

    public static void main(String[] args) throws IOException {
        List<String> allIds = Files.readAllLines(new File(args[0]).toPath());
        String dbCollectionUrl = args[1];
        String dbName = args[2];
        String collectionName = args[3];

        List<String> projectsNames = null;
        if (args.length > 4) {
            projectsNames = Files.readAllLines(new File(args[4]).toPath());
            String githubLogin = args[5];
            String githubToken = args[6];

            RepairnatorConfig.getInstance().setGithubLogin(githubLogin);
            RepairnatorConfig.getInstance().setGithubToken(githubToken);
        }

        MongoConnection mongoConnection = new MongoConnection(dbCollectionUrl, dbName);
        MongoDatabase database = mongoConnection.getMongoDatabase();
        MongoCollection collection = database.getCollection(collectionName);

        Calendar limitDateMay = Calendar.getInstance();
        //limitDateMay.set(2017, Calendar.MAY, 10);
        limitDateMay.set(2017, Calendar.SEPTEMBER, 8);

        Calendar limitDateNow = Calendar.getInstance();

        HashMap<String,List<Integer>> results = new HashMap<>();

        int i = 0;
        for (String s : allIds) {
            int buildId = Integer.parseInt(s);
            long total = collection.count(
                    and(
                            gt("buildReproductionDate", limitDateMay.getTime()),
                            eq("previousBuildId", buildId)
                    )
            );

            if (total == 0) {
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
        }

        System.out.println(allIds.size()+" ids read, and got: "+i);
        System.out.println(results.keySet().size()+" detected projects: ("+StringUtils.join(results.keySet(), ",")+")");
        System.out.println("Results:");
        for (String s : results.keySet()) {
            System.out.println("Project "+s+" : ");
            System.out.println(StringUtils.join(results.get(s), "\n"));
            System.out.println("\n");
        }

    }
}
