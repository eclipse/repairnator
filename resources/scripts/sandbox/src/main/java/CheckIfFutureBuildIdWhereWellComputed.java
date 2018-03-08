import com.mongodb.Block;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.serializer.mongodb.MongoConnection;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gt;

/**
 * Created by urli on 22/08/2017.
 */
public class CheckIfFutureBuildIdWhereWellComputed {
    static int i = 0;

    public static void main(String[] args) throws IOException {
        List<String> allIds = Files.readAllLines(new File(args[0]).toPath());
        String dbCollectionUrl = args[1];
        String dbName = args[2];
        String collectionName = args[3];

        final List<String> projectsNames = Files.readAllLines(new File(args[4]).toPath());
        String githubLogin = args[5];
        String githubToken = args[6];

        RepairnatorConfig.getInstance().setGithubLogin(githubLogin);
        RepairnatorConfig.getInstance().setGithubToken(githubToken);

        MongoConnection mongoConnection = new MongoConnection(dbCollectionUrl, dbName);
        MongoDatabase database = mongoConnection.getMongoDatabase();
        MongoCollection collection = database.getCollection(collectionName);

        Calendar limitDateMay = Calendar.getInstance();
        limitDateMay.set(2017, Calendar.MAY, 10);

        HashMap<String,List<Integer>> results = new HashMap<>();



        Block<Document> block = new Block<Document>() {

            @Override
            public void apply(Document document) {
                Object pBuildId = document.get("previousBuildId");

                if (pBuildId instanceof Integer) {
                    int previousBuildId = document.getInteger("previousBuildId", -1);
                    int nextBuildId = document.getInteger("buildId", -1);
                    if (previousBuildId != -1 && nextBuildId != -1) {
                       Build previousBuild = BuildHelper.getBuildFromId(previousBuildId, null);
                       Build nextBuild = BuildHelper.getNextBuildOfSameBranchOfStatusAfterBuild(previousBuild, null);

                       if (nextBuild.getId() != nextBuildId) {
                           String projectName = previousBuild.getRepository().getSlug();

                           if (projectsNames == null || projectsNames.contains(projectName)) {
                               if (!results.containsKey(projectName)) {
                                   results.put(projectName, new ArrayList<>());
                               }
                               results.get(projectName).add(previousBuildId);
                               i++;
                           }
                       }
                    }
                }

            }
        };

        for (String s : allIds) {
            int buildId = Integer.parseInt(s);
            collection.find(
                    and(
                            gt("buildReproductionDate", limitDateMay.getTime()),
                            eq("previousBuildId", buildId),
                            eq("lastReproducedBuggyBuild", true)
                    )
            ).forEach(
                    block
            );
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
