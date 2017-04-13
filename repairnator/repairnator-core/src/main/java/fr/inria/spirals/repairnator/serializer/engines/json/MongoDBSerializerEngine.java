package fr.inria.spirals.repairnator.serializer.engines.json;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import fr.inria.spirals.repairnator.serializer.SerializerType;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import fr.inria.spirals.repairnator.serializer.mongodb.MongoConnection;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by urli on 27/03/2017.
 */
public class MongoDBSerializerEngine implements SerializerEngine {
    private Logger logger = LoggerFactory.getLogger(MongoDBSerializerEngine.class);
    private MongoDatabase mongoDatabase;

    public MongoDBSerializerEngine(MongoConnection mongoConnection) {
        this.mongoDatabase = mongoConnection.getMongoDatabase();
    }


    @Override
    public void serialize(List<SerializedData> data, SerializerType serializer) {
        if (this.mongoDatabase != null) {
            MongoCollection<Document> collection = this.mongoDatabase.getCollection(serializer.getFilename());

            List<Document> listDocuments = new ArrayList<>();
            for (SerializedData oneData : data) {
                Document doc = Document.parse(oneData.getAsJson().toString());

                /*try {
                    collection.insertOne(doc);
                } catch (Exception e) {
                    logger.error("Error while inserting doc",e);
                }*/
                listDocuments.add(doc);
            }

            try {
                collection.insertMany(listDocuments);
            } catch (Exception e) {
                logger.error("Error while inserting all documents", e);
            }

        } else {
            logger.error("Mongo connection is null, there was certainly a problem with the connection.");
        }

    }
}
