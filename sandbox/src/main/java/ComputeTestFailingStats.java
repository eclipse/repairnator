import com.mongodb.Block;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import fr.inria.spirals.repairnator.serializer.mongodb.MongoConnection;
import org.bson.Document;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.mongodb.client.model.Filters.*;

/**
 * Created by urli on 04/10/2017.
 */
public class ComputeTestFailingStats {

    static int totalFailingBuild = 0;
    static int totalNumberOfFailures = 0;

    public static void main(String[] args) throws IOException {

        Map<String, Integer> occurencesByFailure = new HashMap<>();
        String dbCollectionUrl = args[0];
        String dbName = args[1];
        String collectionName = args[2];

        String pathOutput = args[3];
        File outputFile = new File(pathOutput);

        MongoConnection mongoConnection = new MongoConnection(dbCollectionUrl, dbName);
        MongoDatabase database = mongoConnection.getMongoDatabase();
        MongoCollection collection = database.getCollection(collectionName);


        Block<Document> block = new Block<Document>(){

            @Override
            public void apply(Document document) {
                totalFailingBuild++;
                String typeOfFailures = document.getString("typeOfFailures");

                for (String failure : typeOfFailures.split(",")) {
                    if (failure.endsWith(":")) {
                        failure = failure.substring(0, failure.length()-1);
                    }
                    if (failure.equals("skip")) {
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
        collection.find(and(ne("typeOfFailures", ""), ne("typeOfFailures", null))).forEach(
                block
        );

        BufferedWriter buffer = new BufferedWriter(new FileWriter(outputFile));
        buffer.write("failure\tNb Occurences\n");
        buffer.flush();

        for (Map.Entry<String,Integer> entry : occurencesByFailure.entrySet()) {
            buffer.write(entry.getKey()+"\t"+entry.getValue()+"\n");
            buffer.flush();
        }

        buffer.close();
        System.out.println("Output written to "+pathOutput+" - "+totalFailingBuild+" failing build detected and "+totalNumberOfFailures+" failures counted.");
    }
}
