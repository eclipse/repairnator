package fr.inria.spirals.repairnator.serializer.engines.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import fr.inria.spirals.repairnator.serializer.Serializers;
import fr.inria.spirals.repairnator.serializer.engines.SerializedData;
import fr.inria.spirals.repairnator.serializer.engines.SerializerEngine;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by urli on 27/03/2017.
 */
public class MongoDBSerializerEngine implements SerializerEngine {
    MongoClient mongoClient;
    MongoDatabase mongoDatabase;

    public MongoDBSerializerEngine(String mongoURI, String dbName) {
        this.mongoClient = new MongoClient(new MongoClientURI(mongoURI));
        this.mongoDatabase = this.mongoClient.getDatabase(dbName);
    }


    @Override
    public void serialize(List<SerializedData> data, Serializers serializer) {
        MongoCollection<Document> collection = this.mongoDatabase.getCollection(serializer.getFilename());

        List<Document> listDocuments = new ArrayList<>();
        for (SerializedData oneData : data) {
            Document doc = Document.parse(oneData.getAsJson().toString());
            listDocuments.add(doc);
        }

        collection.insertMany(listDocuments);
    }
}
