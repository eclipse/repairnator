import com.mongodb.Block;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import fr.inria.spirals.repairnator.serializer.mongodb.MongoConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;

/**
 * Created by urli on 04/10/2017.
 */
public class CleanNopolCollection {
    static int counterDeleted = 0;
    static int counterKept = 0;
    public static void main(String[] args) {
        Map<Integer, Set<String>> presentInDb = new HashMap<>();

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
                String location = document.getString("testClassLocation");
                ObjectId id = document.getObjectId("_id");

                if (presentInDb.containsKey(buildId)) {
                    Set<String> localSet = presentInDb.get(buildId);
                    if (localSet.contains(location)) {
                        collection.deleteOne(eq("_id", id));
                        counterDeleted++;
                        return;
                    } else {
                        localSet.add(location);
                    }
                } else {
                    Set<String> localSet = new HashSet<>();
                    localSet.add(location);
                    presentInDb.put(buildId, localSet);
                }
                counterKept++;
            }
        };
        collection.find().forEach(
            block
        );

        System.out.println(counterDeleted+" entries deleted and "+counterKept+" kept.");
    }
}
