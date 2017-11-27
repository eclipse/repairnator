import com.mongodb.Block;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import fr.inria.spirals.repairnator.serializer.mongodb.MongoConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.HashSet;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Sorts.orderBy;

/**
 * Created by urli on 10/10/2017.
 */
public class RemoveDuplicatedBuildsId {
    static int counterDeleted = 0;
    static int counterKept = 0;

    public static void main(String[] args) {
        Set<Integer> buildIds = new HashSet<>();

        String dbCollectionUrl = args[0];
        String dbName = args[1];
        String collectionName = args[2];

        MongoConnection mongoConnection = new MongoConnection(dbCollectionUrl, dbName);
        MongoDatabase database = mongoConnection.getMongoDatabase();
        MongoCollection collection = database.getCollection(collectionName);


        Block<Document> block = new Block<Document>(){

            @Override
            public void apply(Document document) {
                int buildId = document.getInteger("buildId");
                ObjectId id = document.getObjectId("_id");

                if (buildIds.contains(buildId)) {
                    collection.deleteOne(eq("_id", id));
                    counterDeleted++;
                    return;
                } else {
                    buildIds.add(buildId);
                    counterKept++;
                }
            }
        };
        collection.find().sort(orderBy(descending("buildReproductionDate"))).forEach(
                block
        );

        System.out.println(counterDeleted+" entries deleted and "+counterKept+" kept.");
    }
}
