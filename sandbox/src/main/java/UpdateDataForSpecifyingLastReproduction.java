import com.mongodb.Block;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import fr.inria.spirals.repairnator.serializer.mongodb.MongoConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Sorts.orderBy;

/**
 * Created by urli on 29/08/2017.
 */
public class UpdateDataForSpecifyingLastReproduction {

    public static void main(String[] args) {
        String dbConnectionUrl = args[0];
        String dbName = args[1];
        String collectionName = args[2];

        MongoConnection mongoConnection = new MongoConnection(dbConnectionUrl, dbName);
        MongoDatabase database = mongoConnection.getMongoDatabase();
        MongoCollection collection = database.getCollection(collectionName);

        Calendar limitDateMay = Calendar.getInstance();
        limitDateMay.set(2017, Calendar.MAY, 10);

        final List<ObjectId> updatedDocs = new ArrayList<>();

        Set<Integer> ids = new HashSet<>();

        Block<Document> block = new Block<Document>() {

            @Override
            public void apply(Document document) {
                ObjectId documentId = document.getObjectId("_id");

                Object pBuildId = document.get("previousBuildId");

                if (pBuildId instanceof Integer) {
                    int previousBuildId = document.getInteger("previousBuildId", -1);
                    if (previousBuildId != -1) {
                        boolean lastReproducedBuggyBuild = !ids.contains(previousBuildId);
                        ids.add(previousBuildId);

                        document.append("lastReproducedBuggyBuild", lastReproducedBuggyBuild);
                        collection.replaceOne(eq("_id", documentId), document, new UpdateOptions().upsert( true ));
                        updatedDocs.add(documentId);
                    }
                }

            }
        };

        collection.find().sort(orderBy(descending("buildReproductionDate"))).forEach(
                block
        );

        System.out.println("Updated docs: "+updatedDocs.size());

    }
}
