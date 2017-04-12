package fr.inria.spirals.repairnator.serializer.mongodb;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by urli on 27/03/2017.
 */
public class MongoConnection {
    private Logger logger = LoggerFactory.getLogger(MongoConnection.class);

    private MongoDatabase mongoDatabase;
    private boolean isConnected;

    public MongoConnection(String mongoDBURI, String dbName) {
        try {
            MongoClientURI clientURI = new MongoClientURI(mongoDBURI+"/"+dbName);
            MongoClient client = new MongoClient(clientURI);

            /*boolean existingDb = false;
            for (String s : client.listDatabaseNames()) {
                if (s.equals(dbName)) {
                    existingDb = true;
                    break;
                }
            }

            if (!existingDb) {
                logger.info("The database "+dbName+" does not exist, it will certainly be created if you have rights.");
            }*/

            this.mongoDatabase = client.getDatabase(dbName);
            this.isConnected = true;
        } catch (Exception e) {
            logger.error("Error while connecting to mongoDB, serializers won't be used.", e);
            this.isConnected = false;
        }
    }

    public MongoDatabase getMongoDatabase() {
        return mongoDatabase;
    }

    public boolean isConnected() {
        return isConnected;
    }
}
