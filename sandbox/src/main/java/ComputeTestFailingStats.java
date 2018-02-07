import com.mongodb.Block;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import fr.inria.spirals.repairnator.serializer.mongodb.MongoConnection;
import org.bson.Document;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;

/**
 * This class intends to compute a number of occurences by test failure name,
 * from a repairnator database and to output it in a file
 *
 * Created by urli on 04/10/2017.
 */
public class ComputeTestFailingStats {

    private static int totalFailingBuild = 0;
    private static int totalNumberOfFailures = 0;

    public static void main(String[] args) throws IOException {

        if (args.length < 4) {
            System.err.println("Usage: java ComputeTestFailingStats <url of mongodb with auth> <db name> <collection name> <path of the file to write>");
            System.exit(-1);
        }

        Map<String, Integer> occurencesByFailure = new HashMap<>();
        String dbCollectionUrl = args[0];
        String dbName = args[1];
        String collectionName = args[2];

        String pathOutput = args[3];
        File outputFile = new File(pathOutput);

        MongoConnection mongoConnection = new MongoConnection(dbCollectionUrl, dbName);
        MongoDatabase database = mongoConnection.getMongoDatabase();
        MongoCollection<Document> collection = database.getCollection(collectionName);

        Calendar limitDateFebruary2017 = Calendar.getInstance();
        Calendar limitDateJanuary2018 = Calendar.getInstance();
        limitDateFebruary2017.set(2017, Calendar.FEBRUARY, 1);
        limitDateJanuary2018.set(2018, Calendar.JANUARY, 1);

        Block<Document> block = new Block<Document>(){

            @Override
            public void apply(Document document) {
                totalFailingBuild++;
                String typeOfFailures = document.getString("typeOfFailures");

                for (String failure : typeOfFailures.split(",")) {
                    if (failure.endsWith(":")) {
                        failure = failure.substring(0, failure.length()-1);
                    }
                    if (failure.equals("skip") || failure.equals("skipped")) {
                        continue;
                    }
                    if (!occurencesByFailure.containsKey(failure)) {
                        occurencesByFailure.put(failure, 0);
                    }
                    int nbOcc = occurencesByFailure.get(failure);
                    nbOcc++;
                    occurencesByFailure.put(failure, nbOcc);
                    totalNumberOfFailures++;
                }
            }
        };
        collection.find(
                and(
                        lt("buildFinishedDate", limitDateJanuary2018.getTime()),
                        gt("buildFinishedDate", limitDateFebruary2017.getTime()),
                        in("status", "PATCHED", "test errors", "test failure")
                )
        ).forEach(
                block
        );

        BufferedWriter buffer = new BufferedWriter(new FileWriter(outputFile));
        buffer.write("failure\tNb Occurences\n");
        buffer.flush();



        for (Map.Entry<String,Integer> entry : occurencesByFailure.entrySet().stream().sorted(Map.Entry.comparingByValue(Collections.reverseOrder())).collect(Collectors.toList())) {
            buffer.write(entry.getKey()+"\t"+entry.getValue()+"\n");
            buffer.flush();
        }

        buffer.close();
        System.out.println("Output written to "+pathOutput+" - "+totalFailingBuild+" failing build detected and "+totalNumberOfFailures+" failures counted.");
    }
}
